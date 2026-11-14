package com.overshop.app.admin.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.overshop.app.MainActivity
import com.overshop.app.R
import com.overshop.app.auth.AuthRepository

/** Shows administrator account details and provides a confirmed sign-out action. */
class AdminProfileFragment : Fragment(R.layout.fragment_admin_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val account = AuthRepository.getCurrentUser()
        view.findViewById<TextView>(R.id.tvAdminProfileName).text = account?.fullName.orEmpty()
        view.findViewById<TextView>(R.id.tvAdminProfileEmail).text = account?.email.orEmpty()
        view.findViewById<TextView>(R.id.tvAdminProfilePhone).text = account?.phone.orEmpty()

        view.findViewById<Button>(R.id.btnAdminLogout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    AuthRepository.logout()
                    val loginIntent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(loginIntent)
                }
                .show()
        }
    }
}
