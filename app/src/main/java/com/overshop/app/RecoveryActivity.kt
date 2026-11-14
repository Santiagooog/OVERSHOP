
package com.overshop.app

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.overshop.app.auth.AuthRepository
import kotlinx.coroutines.launch

class RecoveryActivity : AppCompatActivity() {

    private lateinit var etRecoveryEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recovery)

        etRecoveryEmail = findViewById(R.id.etRecoveryEmail)

        val btnRecovery = findViewById<Button>(R.id.btnRecovery)
        val tvBackRecovery = findViewById<TextView>(R.id.tvBackRecovery)

        btnRecovery.setOnClickListener {
            validateRecovery()
        }

        tvBackRecovery.setOnClickListener {
            finish()
        }
    }

    private fun validateRecovery() {

        val email = etRecoveryEmail.text.toString().trim()

        if (email.isEmpty()) {
            etRecoveryEmail.error = "Ingresa tu correo electrónico"
            etRecoveryEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRecoveryEmail.error = "Ingresa un correo válido"
            etRecoveryEmail.requestFocus()
            return
        }

        lifecycleScope.launch {
            val sent = AuthRepository.sendPasswordResetEmail(email)
            if (sent) showRecoveryConfirmation()
            else Toast.makeText(
                this@RecoveryActivity,
                "No se pudo enviar el enlace. Verifica el correo e inténtalo de nuevo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showRecoveryConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Solicitud validada")
            .setMessage(
                "Si la cuenta existe, Firebase enviará un enlace para restablecer la contraseña. Revisa también la carpeta de spam."
            )
            .setPositiveButton("VOLVER AL LOGIN") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
