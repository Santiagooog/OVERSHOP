package com.overshop.app.buyer

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.overshop.app.models.Order
import com.overshop.app.repositories.CartRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Opens the ePayco hosted test checkout and waits for the signed server webhook status. */
class EpaycoPaymentActivity : AppCompatActivity() {
    private val scope = MainScope()
    private var webView: WebView? = null
    private var registration: com.google.firebase.firestore.ListenerRegistration? = null
    private var completed = false
    private lateinit var status: TextView
    private var orderReference = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderReference = intent.getStringExtra("order_reference").orEmpty()
        val sessionId = intent.getStringExtra("session_id").orEmpty()
        if (orderReference.isBlank() || !sessionId.matches(Regex("[A-Za-z0-9]+"))) {
            setResult(RESULT_CANCELED); finish(); return
        }

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = TextView(this).apply { text = "Esperando confirmación segura de ePayco…"; textSize = 16f; setPadding(20, 16, 20, 16) }
        layout.addView(status)
        val web = WebView(this)
        webView = web
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !url.startsWith("https://")
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String?) {
                when (title) {
                    "OVERSHOP_PAYMENT_RESPONSE" -> status.text = "Pago recibido por ePayco. Verificando confirmación del servidor…"
                    "OVERSHOP_PAYMENT_ERROR" -> status.text = "ePayco reportó un error. Puedes volver e intentar nuevamente."
                }
            }
        }
        layout.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(Button(this).apply { text = "Cerrar checkout"; setOnClickListener { finish() } })
        setContentView(layout)

        listenForServerConfirmation()
        val html = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body><p>Abriendo checkout seguro de ePayco…</p>
            <script src="https://checkout.epayco.co/checkout-v2.js"></script>
            <script>
              try {
                const checkout = ePayco.checkout.configure({sessionId: "$sessionId", type: "onpage", test: true});
                checkout.setHooks({
                  onResponse: () => { document.title = "OVERSHOP_PAYMENT_RESPONSE"; },
                  onErrors: () => { document.title = "OVERSHOP_PAYMENT_ERROR"; },
                  onClosed: () => { document.title = "OVERSHOP_PAYMENT_RESPONSE"; }
                });
                checkout.open();
              } catch (e) { document.title = "OVERSHOP_PAYMENT_ERROR"; }
            </script></body></html>
        """.trimIndent()
        web.loadDataWithBaseURL("https://checkout.epayco.co/", html, "text/html", "UTF-8", null)
    }

    private fun listenForServerConfirmation() {
        registration = FirebaseFirestore.getInstance().collection("orders").document(orderReference)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || completed) return@addSnapshotListener
                val order = snapshot.toObject(Order::class.java) ?: return@addSnapshotListener
                when (order.paymentStatus) {
                    "approved" -> onApproved(order)
                    "rejected" -> status.text = "Pago rechazado. Cierra el checkout y vuelve a intentarlo."
                    "pending" -> status.text = "Pago pendiente en ePayco. Espera la confirmación o consulta con el comercio."
                }
            }
    }

    private fun onApproved(order: Order) {
        if (completed) return
        completed = true
        status.text = "Pago aprobado. Finalizando pedido…"
        scope.launch {
            try { CartRepository.clearCloudCart() } catch (_: Exception) { }
            setResult(RESULT_OK, intent.putExtra("order_reference", order.reference)
                .putExtra("order_total", order.total)
                .putExtra("customer_name", order.customerName)
                .putExtra("payment_method", order.paymentMethod))
            finish()
        }
    }

    override fun onDestroy() {
        registration?.remove()
        webView?.apply { stopLoading(); loadUrl("about:blank"); clearHistory(); removeAllViews(); destroy() }
        scope.cancel()
        super.onDestroy()
    }
}
