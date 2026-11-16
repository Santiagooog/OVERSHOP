package com.overshop.app.seller.adapters

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.overshop.app.R
import com.overshop.app.buyer.BuyerActivity
import java.text.NumberFormat
import java.util.Locale

/** Confirma al comprador el registro exitoso de una orden simulada. */
class OrderSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_success)

        val tvCustomer = findViewById<TextView>(R.id.tvOrderCustomer)
        val tvReference = findViewById<TextView>(R.id.tvOrderReference)
        val tvTotal = findViewById<TextView>(R.id.tvOrderTotal)
        val tvPayment = findViewById<TextView>(R.id.tvOrderPayment)
        val btnBack = findViewById<Button>(R.id.btnBackToShop)

        val customer = intent.getStringExtra("customer_name") ?: "Cliente"
        val reference = intent.getStringExtra("order_reference") ?: "Sin referencia"
        val total = intent.getIntExtra("order_total", 0)
        val payment = intent.getStringExtra("payment_method") ?: "No especificado"

        tvCustomer.text = "Pedido registrado para: $customer"
        tvReference.text = "Referencia: $reference"
        tvTotal.text = "Total: ${formatCurrency(total)}"
        tvPayment.text = "Simulación académica (sin cobro): $payment"

        btnBack.setOnClickListener {
            val intent = Intent(this, BuyerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            finish()
        }
    }

    /** Formats the demo order total for the confirmation screen. */
    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
