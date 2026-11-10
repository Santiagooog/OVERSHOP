
package com.overshop.app

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

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

        showRecoveryConfirmation()
    }

    private fun showRecoveryConfirmation() {

        AlertDialog.Builder(this)
            .setTitle("Solicitud validada")
            .setMessage(
                "El correo tiene un formato válido.\n\n" +
                        "El envío del enlace de recuperación " +
                        "estará disponible cuando conectemos " +
                        "el servicio de autenticación."
            )
            .setPositiveButton("VOLVER AL LOGIN") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
