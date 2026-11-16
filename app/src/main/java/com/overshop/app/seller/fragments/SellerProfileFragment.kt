package com.overshop.app.seller.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.overshop.app.MainActivity
import com.overshop.app.R
import com.overshop.app.auth.AuthRepository
import com.overshop.app.repositories.CartRepository
import com.overshop.app.repositories.UserRepository

/** Muestra la información de cuenta y las opciones de perfil del Vendedor. */
class SellerProfileFragment :
    Fragment(R.layout.fragment_seller_profile) {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var tvRole: TextView
    private lateinit var btnLogout: Button

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.etSellerProfileName)
        etEmail = view.findViewById(R.id.etSellerProfileEmail)
        etPhone = view.findViewById(R.id.etSellerProfilePhone)
        tvRole = view.findViewById(R.id.tvSellerProfileRole)
        btnLogout = view.findViewById(R.id.btnSellerLogout)

        loadProfile()

        btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    /** Loads the signed-in seller profile and updates the account screen. */
    private fun loadProfile() {

        val user = AuthRepository.getCurrentUser()

        if (user == null || user.role != "Vendedor") {
            Toast.makeText(
                requireContext(),
                "No se encontró una cuenta de vendedor activa.",
                Toast.LENGTH_LONG
            ).show()

            redirectToLogin()
            return
        }

        etName.setText(user.fullName)
        etEmail.setText(user.email)
        etPhone.setText(user.phone)
        tvRole.text = user.role.uppercase()
    }

    /** Confirms and performs sign-out for the current seller session. */
    private fun confirmLogout() {

        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage(
                "¿Estás seguro de que deseas cerrar tu sesión de vendedor?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ ->

                AuthRepository.logout()
                UserRepository.clearUser()
                CartRepository.clearCart()
                redirectToLogin()
            }
            .show()
    }

    /** Clears the seller back stack and returns to the login screen after sign-out. */
    private fun redirectToLogin() {

        val intent = Intent(
            requireContext(),
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
    }
}
