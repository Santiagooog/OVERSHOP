package com.overshop.app

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.overshop.app.auth.AuthRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Pantalla de registro público para crear cuentas Comprador o Vendedor. */
class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText

    private lateinit var rgUserRole: RadioGroup
    private lateinit var cbTerms: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initializeViews()
        configureListeners()
    }

    /** Binds registration fields and the role selector to the activity layout. */
    private fun initializeViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etRegisterEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        rgUserRole = findViewById(R.id.rgUserRole)
        cbTerms = findViewById(R.id.cbTerms)
    }

    /** Connects the submit and navigation controls to registration actions. */
    private fun configureListeners() {
        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            validateRegistration()
        }

        findViewById<TextView>(R.id.tvBackToLogin).setOnClickListener {
            finish()
        }
    }

    /** Validates account data and delegates public Buyer/Seller creation to Firebase. */
    private fun validateRegistration() {

        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (name.isEmpty()) {
            etFullName.error = "Ingresa tu nombre completo"
            etFullName.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un correo válido"
            etEmail.requestFocus()
            return
        }

        if (!phone.matches(Regex("^[0-9]{7,15}$"))) {
            etPhone.error = "Ingresa un teléfono válido"
            etPhone.requestFocus()
            return
        }

        if (password.length < 8) {
            etPassword.error = "Mínimo 8 caracteres"
            etPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Las contraseñas no coinciden"
            etConfirmPassword.requestFocus()
            return
        }

        if (rgUserRole.checkedRadioButtonId == -1) {
            showMessage("Selecciona un tipo de cuenta")
            return
        }

        if (!cbTerms.isChecked) {
            showMessage("Debes aceptar los términos y condiciones")
            return
        }

        val selectedRole = when (rgUserRole.checkedRadioButtonId) {
            R.id.rbBuyer -> "Comprador"
            R.id.rbSeller -> "Vendedor"
            else -> {
                showMessage("Selecciona un tipo de cuenta válido")
                return
            }
        }

        lifecycleScope.launch {

            val registered = AuthRepository.register(
                fullName = name,
                email = email,
                phone = phone,
                password = password,
                role = selectedRole
            )

            if (!registered) {
                etEmail.error = "Este correo ya está registrado"
                etEmail.requestFocus()
                return@launch
            }

            AlertDialog.Builder(this@RegisterActivity)
                .setTitle("¡Registro exitoso!")
                .setMessage(
                    "Tu cuenta fue creada correctamente.\n\n" +
                            "Nombre: $name\n" +
                            "Correo: $email\n" +
                            "Tipo de cuenta: $selectedRole\n\n" +
                            "Ahora puedes iniciar sesión."
                )
                .setCancelable(false)
                .setPositiveButton("VOLVER AL LOGIN") { _, _ ->
                    finish()
                }
                .show()
        }

    }

    /** Displays a short status message after registration actions. */
    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
