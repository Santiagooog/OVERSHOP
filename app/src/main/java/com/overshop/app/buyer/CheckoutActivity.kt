package com.overshop.app.buyer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.overshop.app.R
import com.overshop.app.repositories.CartRepository
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import com.overshop.app.models.UserProfile
import com.overshop.app.models.Order
import com.overshop.app.models.OrderItem
import com.overshop.app.repositories.UserRepository
import com.overshop.app.repositories.OrderRepository
import com.overshop.app.seller.adapters.OrderSuccessActivity
import java.text.SimpleDateFormat
import java.util.Date
import androidx.lifecycle.lifecycleScope
import com.overshop.app.auth.AuthRepository
import kotlinx.coroutines.launch

class CheckoutActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var rgPayment: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        etName = findViewById(R.id.etCheckoutName)
        etEmail = findViewById(R.id.etCheckoutEmail)
        etPhone = findViewById(R.id.etCheckoutPhone)
        etAddress = findViewById(R.id.etCheckoutAddress)
        etCity = findViewById(R.id.etCheckoutCity)


        // Recuperar información del comprador si ya existe
        UserRepository.getUser()?.let { user ->
            etName.setText(user.name)
            etEmail.setText(user.email)
            etPhone.setText(user.phone)
            etAddress.setText(user.address)
            etCity.setText(user.city)
        }
        rgPayment = findViewById(R.id.rgPaymentMethods)

        val tvItems = findViewById<TextView>(R.id.tvCheckoutItems)
        val tvTotal = findViewById<TextView>(R.id.tvCheckoutTotal)
        val btnBack = findViewById<TextView>(R.id.tvBackCheckout)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmOrder)

        // Evitar checkout con carrito vacío
        if (CartRepository.isEmpty()) {
            Toast.makeText(
                this,
                "No hay productos en el carrito",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        // Mostrar productos
        val summary = CartRepository.getItems().joinToString("\n\n") {
            "${it.product.name}\n" +
                    "Talla: ${it.size} | Cantidad: ${it.quantity}\n" +
                    "Subtotal: ${formatCurrency(it.subtotal)}"
        }

        tvItems.text = summary

        tvTotal.text = "TOTAL: ${formatCurrency(CartRepository.getTotal())}"

        btnBack.setOnClickListener {
            finish()
        }

        btnConfirm.setOnClickListener {
            confirmOrder()
        }
    }


    private fun confirmOrder() {

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val city = etCity.text.toString().trim()

        val user = UserProfile(
            name = name,
            email = email,
            phone = phone,
            address = address,
            city = city
        )

        // Validar nombre
        if (name.isEmpty()) {
            etName.error = "Ingresa tu nombre"
            etName.requestFocus()
            return
        }

        // Validar correo
        if (email.isEmpty() ||
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            etEmail.error = "Ingresa un correo válido"
            etEmail.requestFocus()
            return
        }

        // Validar teléfono
        if (phone.isEmpty() || phone.length < 7) {
            etPhone.error = "Ingresa un teléfono válido"
            etPhone.requestFocus()
            return
        }

        // Validar dirección
        if (address.isEmpty()) {
            etAddress.error = "Ingresa tu dirección"
            etAddress.requestFocus()
            return
        }

        // Validar ciudad
        if (city.isEmpty()) {
            etCity.error = "Ingresa tu ciudad"
            etCity.requestFocus()
            return
        }

        // Validar método de pago
        val selectedPayment = rgPayment.checkedRadioButtonId

        if (selectedPayment == -1) {
            Toast.makeText(
                this,
                "Selecciona un método de pago",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val paymentMethod = findViewById<android.widget.RadioButton>(
            selectedPayment
        ).text.toString()

        // Verificar que el carrito tenga productos
        val cartItems = CartRepository.getItems()

        if (cartItems.isEmpty()) {
            Toast.makeText(
                this,
                "Tu carrito está vacío",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        // Guardar información del comprador


        UserRepository.saveUser(user)
        Toast.makeText(
            this,
            "Datos del comprador guardados",
            Toast.LENGTH_SHORT
        ).show()

        // Generar referencia del pedido
        val orderReference = "OVS-" +
                java.util.UUID.randomUUID()
                    .toString()
                    .take(8)
                    .uppercase(java.util.Locale.ROOT)

        // Convertir productos del carrito en elementos del pedido
        val orderItems = cartItems.map { item ->
            OrderItem(
                productName = item.product.name,
                productImage = item.product.imageRes,
                price = item.product.price,
                size = item.size,
                quantity = item.quantity
            )
        }

        // Fecha del pedido
        val orderDate = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("es-CO")
        ).format(Date())

        // Crear pedido
        val order = Order(
            reference = orderReference,
            customerName = name,
            date = orderDate,
            paymentMethod = paymentMethod,
            items = orderItems,
            total = CartRepository.getTotal(),
            buyerUid = AuthRepository.getCurrentUser()?.uid.orEmpty(),
            email = email,
            phone = phone,
            address = address,
            city = city,
            status = "Confirmado"
        )

        btnConfirmOrder(order, orderReference, name, paymentMethod)
    }

    /** Persists the purchase before clearing the cart and opening confirmation. */
    private fun btnConfirmOrder(order: Order, orderReference: String, name: String, paymentMethod: String) {
        lifecycleScope.launch {
            val saved = OrderRepository.saveOrder(order)
            if (!saved) {
                Toast.makeText(this@CheckoutActivity, "No se pudo guardar el pedido. Revisa tu conexión e inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                return@launch
            }
            CartRepository.clearCart()
            val intent = android.content.Intent(this@CheckoutActivity, OrderSuccessActivity::class.java).apply {
                putExtra("order_reference", orderReference)
                putExtra("order_total", order.total)
                putExtra("customer_name", name)
                putExtra("payment_method", paymentMethod)
            }
            startActivity(intent)
            finish()
        }
    }


    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
