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
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import android.view.View

class CheckoutActivity : AppCompatActivity() {

    private var deliveryLatitude = 0.0
    private var deliveryLongitude = 0.0
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var paymentLauncher: ActivityResultLauncher<android.content.Intent>

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var rgPayment: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) captureLocation()
            else Toast.makeText(this, "Permite la ubicación para asociarla al pedido.", Toast.LENGTH_LONG).show()
        }
        paymentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                startActivity(android.content.Intent(this, OrderSuccessActivity::class.java).apply {
                    putExtras(data)
                })
                finish()
            }
        }
        setContentView(R.layout.activity_checkout)

        etName = findViewById(R.id.etCheckoutName)
        etEmail = findViewById(R.id.etCheckoutEmail)
        etPhone = findViewById(R.id.etCheckoutPhone)
        etAddress = findViewById(R.id.etCheckoutAddress)
        etCity = findViewById(R.id.etCheckoutCity)


        // Recuperar información del comprador si ya existe
        AuthRepository.getCurrentUser()?.let { user ->
            etName.setText(user.fullName)
            etEmail.setText(user.email)
            etPhone.setText(user.phone)
            etAddress.setText(user.address)
            etCity.setText(user.city)
        }
        rgPayment = findViewById(R.id.rgPaymentMethods)
        rgPayment.visibility = View.GONE
        findViewById<Button>(R.id.btnUseCurrentLocation).setOnClickListener { requestLocation() }

        val tvItems = findViewById<TextView>(R.id.tvCheckoutItems)
        val tvTotal = findViewById<TextView>(R.id.tvCheckoutTotal)
        val btnBack = findViewById<TextView>(R.id.tvBackCheckout)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmOrder)

        lifecycleScope.launch {
            val items = try { CartRepository.loadCloudItems() } catch (_: Exception) { emptyList() }
            if (items.isEmpty()) {
                Toast.makeText(this@CheckoutActivity, "No hay productos en el carrito o no se pudo sincronizar.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            tvItems.text = items.joinToString("\n\n") {
                "${it.product.name}\nTalla: ${it.size} | Cantidad: ${it.quantity}\nSubtotal: ${formatCurrency(it.subtotal)}"
            }
            tvTotal.text = "TOTAL: ${formatCurrency(items.sumOf { it.subtotal })}"
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnConfirm.setOnClickListener {
            confirmOrder()
        }
    }

    /** Requests foreground permission and captures a one-time delivery coordinate. */
    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) captureLocation()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Suppress("MissingPermission")
    private fun captureLocation() {
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(this, "No se obtuvo ubicación. Activa GPS e inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                deliveryLatitude = location.latitude
                deliveryLongitude = location.longitude
                lifecycleScope.launch {
                    try {
                        val address = withContext(Dispatchers.IO) {
                            Geocoder(this@CheckoutActivity, Locale.forLanguageTag("es-CO"))
                                .getFromLocation(location.latitude, location.longitude, 1)
                                ?.firstOrNull()
                        }
                        address?.getAddressLine(0)?.let { if (etAddress.text.isNullOrBlank()) etAddress.setText(it) }
                        address?.locality?.let { if (etCity.text.isNullOrBlank()) etCity.setText(it) }
                        Toast.makeText(this@CheckoutActivity, "Ubicación asociada al pedido.", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(this@CheckoutActivity, "Coordenadas capturadas; escribe la dirección manualmente.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo obtener la ubicación.", Toast.LENGTH_LONG).show()
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

        val paymentMethod = "ePayco"

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
                productDocumentId = item.product.documentId,
                productId = item.product.id,
                productName = item.product.name,
                productImage = item.product.imageRes,
                productImageUrl = item.product.imageUrl,
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
            latitude = deliveryLatitude,
            longitude = deliveryLongitude,
            status = "Pendiente de pago",
            paymentProvider = "ePayco",
            paymentStatus = "created"
        )

        btnConfirmOrder(order, orderReference, name, paymentMethod)
    }

    /** Saves the order first, then requests an ePayco Smart Checkout session from the trusted backend. */
    private fun btnConfirmOrder(order: Order, orderReference: String, name: String, paymentMethod: String) {
        lifecycleScope.launch {
            val saved = OrderRepository.saveOrder(order)
            if (!saved) {
                Toast.makeText(this@CheckoutActivity, "No se pudo guardar el pedido. Revisa tu conexión e inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                return@launch
            }
            try {
                val result = FirebaseFunctions.getInstance().getHttpsCallable("createEpaycoSession")
                    .call(mapOf("orderReference" to orderReference)).await()
                val response = result.data as? Map<*, *>
                val sessionId = response?.get("sessionId") as? String
                    ?: throw IllegalStateException("ePayco no devolvió la sesión")
                val paymentIntent = android.content.Intent(this@CheckoutActivity, EpaycoPaymentActivity::class.java).apply {
                    putExtra("order_reference", orderReference)
                    putExtra("session_id", sessionId)
                }
                paymentLauncher.launch(paymentIntent)
            } catch (_: Exception) {
                Toast.makeText(this@CheckoutActivity, "No se pudo abrir ePayco. Verifica el despliegue del backend y las llaves de prueba.", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
