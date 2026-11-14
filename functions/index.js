const admin = require('firebase-admin');
const functions = require('firebase-functions');
const { onCall, onRequest, HttpsError } = require('firebase-functions/v2/https');
const { defineSecret } = require('firebase-functions/params');
const { createHash, timingSafeEqual } = require('crypto');

admin.initializeApp();
const EPAYCO_PUBLIC_KEY = defineSecret('EPAYCO_PUBLIC_KEY');
const EPAYCO_PRIVATE_KEY = defineSecret('EPAYCO_PRIVATE_KEY');
const EPAYCO_CUSTOMER_ID = defineSecret('EPAYCO_CUSTOMER_ID');
const EPAYCO_P_KEY = defineSecret('EPAYCO_P_KEY');

async function requireAdmin(context) {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Debes iniciar sesión.');
  const caller = await admin.firestore().collection('users').doc(context.auth.uid).get();
  if (!caller.exists || caller.get('role') !== 'Administrador') {
    throw new functions.https.HttpsError('permission-denied', 'Se requiere el rol Administrador.');
  }
}

exports.adminCreateUser = functions.https.onCall(async (data, context) => {
  await requireAdmin(context);
  const fullName = String(data.fullName || '').trim();
  const email = String(data.email || '').trim().toLowerCase();
  const phone = String(data.phone || '').trim();
  const password = String(data.password || '');
  const role = String(data.role || '');
  if (!fullName || !/^\S+@\S+\.\S+$/.test(email) || !phone || password.length < 8 || !['Comprador', 'Vendedor', 'Administrador'].includes(role)) {
    throw new functions.https.HttpsError('invalid-argument', 'Verifica los datos y el rol permitido.');
  }
  const user = await admin.auth().createUser({ email, password, displayName: fullName });
  try {
    await admin.firestore().collection('users').doc(user.uid).set({ uid: user.uid, fullName, email, phone, role });
  } catch (error) {
    await admin.auth().deleteUser(user.uid);
    throw new functions.https.HttpsError('internal', 'No se pudo crear el perfil de usuario.');
  }
  return { uid: user.uid };
});

exports.adminDeleteUser = functions.https.onCall(async (data, context) => {
  await requireAdmin(context);
  const uid = String(data.uid || '');
  if (!uid || uid === context.auth.uid) throw new functions.https.HttpsError('invalid-argument', 'No puedes eliminar tu propia cuenta.');
  try { await admin.auth().deleteUser(uid); }
  catch (error) {
    throw new functions.https.HttpsError('internal', 'No se pudo eliminar la cuenta de autenticación.');
  }
  await admin.firestore().collection('users').doc(uid).delete();
  return { deleted: true };
});

// Create a Smart Checkout session only for an order owned by this buyer.
exports.createEpaycoSession = onCall({ region: 'us-central1', secrets: [EPAYCO_PUBLIC_KEY, EPAYCO_PRIVATE_KEY] }, async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Debes iniciar sesión.');
  const uid = request.auth.uid;
  const ref = String(request.data.orderReference || '');
  const db = admin.firestore();
  const [profile, orderDoc] = await Promise.all([
    db.collection('users').doc(uid).get(),
    db.collection('orders').doc(ref).get()
  ]);
  if (!profile.exists || profile.get('role') !== 'Comprador' || !orderDoc.exists || orderDoc.get('buyerUid') !== uid) {
    throw new HttpsError('permission-denied', 'No puedes pagar este pedido.');
  }
  const order = orderDoc.data();
  if (!Number.isInteger(order.total) || order.total <= 0 || order.paymentStatus === 'approved' || !Array.isArray(order.items) || order.items.length === 0) {
    throw new HttpsError('failed-precondition', 'El pedido no tiene un saldo válido.');
  }
  const verifiedItems = await Promise.all(order.items.map(async (item) => {
    if (!item.productDocumentId || !Number.isInteger(item.quantity) || item.quantity < 1 || item.quantity > 20) {
      throw new HttpsError('invalid-argument', 'El pedido contiene un producto o cantidad inválidos.');
    }
    const product = await db.collection('products').doc(String(item.productDocumentId)).get();
    if (!product.exists || !Number.isInteger(product.get('price')) || product.get('price') !== item.price) {
      throw new HttpsError('failed-precondition', 'El precio del catálogo cambió. Actualiza el carrito y vuelve a intentar.');
    }
    return product.get('price') * item.quantity;
  }));
  const verifiedTotal = verifiedItems.reduce((sum, subtotal) => sum + subtotal, 0);
  if (verifiedTotal !== order.total) throw new HttpsError('failed-precondition', 'El total del pedido no coincide con el catálogo.');

  const basic = Buffer.from(`${EPAYCO_PUBLIC_KEY.value()}:${EPAYCO_PRIVATE_KEY.value()}`).toString('base64');
  const login = await fetch('https://apify.epayco.co/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Basic ${basic}` }, body: '{}'
  });
  if (!login.ok) throw new HttpsError('internal', 'ePayco no autorizó la sesión de prueba.');
  const authData = await login.json();
  const confirmation = 'https://us-central1-overshop-5c97d.cloudfunctions.net/epaycoConfirmation';
  const sessionResponse = await fetch('https://apify.epayco.co/payment/session/create', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${authData.token}` },
    body: JSON.stringify({
      checkout_version: '2', name: 'OVERSHOP', description: `Pedido ${ref}`,
      currency: 'COP', amount: verifiedTotal, invoice: ref, country: 'CO', lang: 'ES',
      confirmation, method: 'POST', uniqueTransactionPerBill: true
    })
  });
  const response = await sessionResponse.json();
  if (!sessionResponse.ok || !response.success || !response.data?.sessionId) {
    throw new HttpsError('internal', 'ePayco no pudo crear la sesión de pago.');
  }
  await orderDoc.ref.update({ paymentProvider: 'ePayco', paymentStatus: 'processing', ePaycoSessionId: response.data.sessionId });
  return { sessionId: response.data.sessionId, test: true };
});

// Verify ePayco's server-to-server signature before changing the order state.
exports.epaycoConfirmation = onRequest({ region: 'us-central1', secrets: [EPAYCO_CUSTOMER_ID, EPAYCO_P_KEY] }, async (req, res) => {
  const data = req.method === 'GET' ? req.query : req.body;
  const fields = ['x_ref_payco', 'x_transaction_id', 'x_amount', 'x_currency_code', 'x_signature', 'x_id_invoice'];
  if (!fields.every((field) => data[field] !== undefined)) return res.status(400).send('Missing fields');
  const expected = createHash('sha256').update(`${EPAYCO_CUSTOMER_ID.value()}^${EPAYCO_P_KEY.value()}^${data.x_ref_payco}^${data.x_transaction_id}^${data.x_amount}^${data.x_currency_code}`).digest('hex');
  const actual = String(data.x_signature);
  if (expected.length !== actual.length || !timingSafeEqual(Buffer.from(expected), Buffer.from(actual))) return res.status(400).send('Invalid signature');
  if (String(data.x_currency_code).toUpperCase() !== 'COP') return res.status(400).send('Invalid currency');

  const db = admin.firestore();
  const orderRef = db.collection('orders').doc(String(data.x_id_invoice));
  const eventRef = db.collection('processedPayments').doc(`${data.x_transaction_id}_${data.x_response || 'unknown'}`);
  try {
    await db.runTransaction(async (tx) => {
      const [orderSnap, eventSnap] = await Promise.all([tx.get(orderRef), tx.get(eventRef)]);
      if (eventSnap.exists) return;
      if (!orderSnap.exists || Number(orderSnap.get('total')) !== Number(data.x_amount)) throw new Error('Order or amount mismatch');
      const response = String(data.x_response || '');
      const paymentStatus = response === 'Aceptada' ? 'approved' : response === 'Pendiente' ? 'pending' : 'rejected';
      const orderUpdate = {
        paymentProvider: 'ePayco', paymentStatus,
        paymentReference: String(data.x_ref_payco), paymentTransactionId: String(data.x_transaction_id),
        paymentMethod: String(data.x_franchise || 'ePayco'),
        paymentUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
      };
      if (paymentStatus === 'approved') orderUpdate.status = 'Confirmado';
      tx.update(orderRef, orderUpdate);
      tx.create(eventRef, { orderReference: orderRef.id, response, receivedAt: admin.firestore.FieldValue.serverTimestamp() });
    });
    return res.status(200).send('OK');
  } catch (error) {
    console.error('ePayco confirmation rejected', error.message);
    return res.status(400).send('Could not confirm payment');
  }
});
