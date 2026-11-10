package com.overshop.app.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.overshop.app.R
import com.overshop.app.admin.fragments.AdminDashboardFragment
import com.overshop.app.admin.fragments.AdminProductsFragment
import com.overshop.app.admin.fragments.AdminReportsFragment
import com.overshop.app.admin.fragments.AdminUsersFragment
import com.overshop.app.auth.AuthRepository

class AdminActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = AuthRepository.getCurrentUser()

        if (currentUser?.role != "Administrador") {
            Toast.makeText(
                this,
                "Acceso denegado. Se requiere una cuenta administradora.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        setContentView(R.layout.activity_admin)

        bottomNavigation =
            findViewById(R.id.adminBottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->

            val fragment: Fragment = when (item.itemId) {

                R.id.nav_admin_dashboard ->
                    AdminDashboardFragment()

                R.id.nav_admin_users ->
                    AdminUsersFragment()

                R.id.nav_admin_products ->
                    AdminProductsFragment()

                R.id.nav_admin_reports ->
                    AdminReportsFragment()

                else -> AdminDashboardFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.adminFragmentContainer, fragment)
                .commit()

            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId =
                R.id.nav_admin_dashboard
        }
    }
}