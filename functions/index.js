const admin = require('firebase-admin');
const functions = require('firebase-functions');

admin.initializeApp();

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
