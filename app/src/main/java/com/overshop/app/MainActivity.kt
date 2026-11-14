package com.overshop.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.overshop.app.auth.AuthRepository
import com.overshop.app.buyer.BuyerActivity
import com.overshop.app.seller.SellerActivity
import com.overshop.app.admin.AdminActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val biometricAuthenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializeViews()
        setupBiometricPrompt()
        configureListeners()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
    }

    private fun setupBiometricPrompt() {

        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    lifecycleScope.launch {
                        val user = AuthRepository.restoreAuthenticatedSession()
                        if (user == null) {
                            showMessage("Inicia sesión una vez con tu contraseña antes de usar la huella.")
                        } else {
                            openHomeForRole(user.role)
                        }
                    }
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    showMessage("Autenticación finalizada: $errString")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    showMessage(
                        "No se reconoció la huella. Intenta nuevamente."
                    )
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación OVERSHOP")
            .setSubtitle("Verifica tu identidad para continuar")
            .setDescription("Utiliza tu huella digital registrada")
            .setAllowedAuthenticators(biometricAuthenticators)
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun configureListeners() {

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val btnBiometric = findViewById<Button>(R.id.btnBiometric)

        val tvForgotPassword =
            findViewById<TextView>(R.id.tvForgotPassword)

        val tvRegister =
            findViewById<TextView>(R.id.tvRegister)

        btnLogin.setOnClickListener {
            validateLogin()
        }

        btnGoogle.visibility = View.GONE

        tvForgotPassword.setOnClickListener {
            startActivity(
                Intent(this, RecoveryActivity::class.java)
            )
        }

        tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }

        btnBiometric.setOnClickListener {
            authenticateWithBiometrics()
        }
    }

    private fun authenticateWithBiometrics() {

        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(biometricAuthenticators)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showMessage(
                    "El dispositivo no tiene hardware biométrico."
                )
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showMessage(
                    "El sensor biométrico no está disponible."
                )
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showMessage(
                    "No hay huellas registradas. " +
                            "Configura una en los ajustes del dispositivo."
                )
            }

            else -> {
                showMessage(
                    "No se puede utilizar la autenticación biométrica."
                )
            }
        }
    }

    private fun validateLogin() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo electrónico"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un correo válido"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Ingresa tu contraseña"
            etPassword.requestFocus()
            return
        }

        if (password.length < 8) {
            etPassword.error = "Mínimo 8 caracteres"
            etPassword.requestFocus()
            return
        }

        lifecycleScope.launch {

            val authenticated = AuthRepository.login(email, password)

            if (!authenticated) {
                showMessage(
                    "Correo o contraseña incorrectos. " +
                            "Verifica tus datos o crea una cuenta."
                )
                return@launch
            }

            val user = AuthRepository.getCurrentUser()

            if (user == null) {
                showMessage("No fue posible recuperar la información de la cuenta.")
                return@launch
            }

            showMessage("Bienvenido, ${user.fullName}")

            openHomeForRole(user.role)
        }
    }

    private fun openHomeForRole(role: String) {
            when (role) {

                "Comprador" -> {
                    startActivity(Intent(this@MainActivity, BuyerActivity::class.java))
                }

                "Administrador" -> {
                    startActivity(Intent(this@MainActivity, AdminActivity::class.java))
                }

                "Vendedor" -> {
                    startActivity(Intent(this@MainActivity, SellerActivity::class.java))
                }

                else -> {
                    AuthRepository.logout()
                    showMessage("El tipo de cuenta no es válido.")
                }
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
