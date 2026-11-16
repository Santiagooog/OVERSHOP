
package com.overshop.app.buyer.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.overshop.app.R
import com.overshop.app.MainActivity
import com.overshop.app.repositories.CartRepository
import com.overshop.app.models.UserProfile
import com.overshop.app.repositories.UserRepository
import com.overshop.app.repositories.OrderRepository
import java.text.NumberFormat
import java.util.Locale
import com.overshop.app.auth.AuthRepository

/** Muestra y actualiza los datos personales, dirección e historial del Comprador. */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText

    private lateinit var btnEdit: Button
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    private lateinit var tvOrderCount: TextView
    private lateinit var tvOrdersEmpty: TextView
    private lateinit var layoutOrders: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.etProfileName)
        etEmail = view.findViewById(R.id.etProfileEmail)
        etPhone = view.findViewById(R.id.etProfilePhone)
        etAddress = view.findViewById(R.id.etProfileAddress)
        etCity = view.findViewById(R.id.etProfileCity)

        btnEdit = view.findViewById(R.id.btnEditProfile)
        btnSave = view.findViewById(R.id.btnSaveProfile)
        btnLogout = view.findViewById(R.id.btnLogout)

        tvOrderCount = view.findViewById(R.id.tvProfileOrderCount)
        tvOrdersEmpty = view.findViewById(R.id.tvProfileOrdersEmpty)
        layoutOrders = view.findViewById(R.id.layoutProfileOrders)

        loadProfile()
        loadOrders()

        btnEdit.setOnClickListener {
            setFieldsEnabled(true)
            btnEdit.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    /** Loads the signed-in buyer's saved Firestore profile into the editable fields. */
    private fun loadProfile() {
        val account = AuthRepository.getCurrentUser()
        val user = account?.let { UserProfile(it.fullName, it.email, it.phone, it.address, it.city) }
            ?: UserRepository.getUser()

        if (user == null) {
            etName.setText("")
            etEmail.setText("")
            etPhone.setText("")
            etAddress.setText("")
            etCity.setText("")
            return
        }

        etName.setText(user.name)
        etEmail.setText(user.email)
        etPhone.setText(user.phone)
        etAddress.setText(user.address)
        etCity.setText(user.city)

        setFieldsEnabled(false)
    }

    /** Enables or locks editable profile inputs according to the current form mode. */
    private fun setFieldsEnabled(enabled: Boolean) {
        etName.isEnabled = enabled
        etEmail.isEnabled = enabled
        etPhone.isEnabled = enabled
        etAddress.isEnabled = enabled
        etCity.isEnabled = enabled
    }

    /** Validates contact and delivery fields before saving the buyer's Firestore profile. */
    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val city = etCity.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Ingresa tu nombre"
            etName.requestFocus()
            return
        }

        if (email.isEmpty() ||
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            etEmail.error = "Ingresa un correo válido"
            etEmail.requestFocus()
            return
        }

        if (phone.length < 7) {
            etPhone.error = "Ingresa un teléfono válido"
            etPhone.requestFocus()
            return
        }

        if (address.isEmpty()) {
            etAddress.error = "Ingresa tu dirección"
            etAddress.requestFocus()
            return
        }

        if (city.isEmpty()) {
            etCity.error = "Ingresa tu ciudad"
            etCity.requestFocus()
            return
        }

        val updatedUser = UserProfile(
            name = name,
            email = email,
            phone = phone,
            address = address,
            city = city
        )

        val account = AuthRepository.getCurrentUser()
        if (account == null) {
            Toast.makeText(requireContext(), "Inicia sesión nuevamente para guardar el perfil.", Toast.LENGTH_LONG).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val updated = AuthRepository.updateAccount(account.uid, name, phone, account.role, address, city)
            if (!updated) {
                Toast.makeText(requireContext(), "No se pudo guardar el perfil en Firestore.", Toast.LENGTH_LONG).show()
                return@launch
            }
            UserRepository.updateUser(updatedUser)
            setFieldsEnabled(false)
            btnEdit.visibility = View.VISIBLE
            btnSave.visibility = View.GONE
            Toast.makeText(requireContext(), "Información actualizada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    /** Loads the active buyer's order history and renders each persisted order. */
    private fun loadOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
        val orders = OrderRepository.getOrdersFromCloud()

        tvOrderCount.text = if (orders.size == 1) {
            "1 pedido registrado"
        } else {
            "${orders.size} pedidos registrados"
        }

        layoutOrders.removeAllViews()

        val empty = orders.isEmpty()

        tvOrdersEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        layoutOrders.visibility = if (empty) View.GONE else View.VISIBLE

        orders.forEach { order ->

            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.color.overshop_black)
            }

            val products = order.items.joinToString("\n") {
                "${it.productName} | Talla ${it.size} | Cantidad: ${it.quantity}"
            }

            val content = """
                Referencia: ${order.reference}
                Fecha: ${order.date}
                Estado: ${order.status}
                
                $products
                
                Pago simulado (sin cobro): ${order.paymentMethod}
                Total: ${formatCurrency(order.total)}
            """.trimIndent()

            val text = TextView(requireContext()).apply {
                this.text = content
                textSize = 14f
                setTextColor(
                    resources.getColor(R.color.overshop_white, null)
                )
                setLineSpacing(5f, 1f)
            }

            card.addView(text)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 14
            }

            layoutOrders.addView(card, params)
        }
        }
    }

    /** Asks for confirmation before ending the current Firebase session. */
    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ ->

                AuthRepository.logout()
                UserRepository.clearUser()
                CartRepository.clearCart()

                val intent = Intent(
                    requireContext(),
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
            }
            .show()
    }

    /** Formats order totals for the buyer's history using Colombian peso conventions. */
    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
