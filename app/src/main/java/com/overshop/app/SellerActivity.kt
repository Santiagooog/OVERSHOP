package com.overshop.app.seller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.overshop.app.R
import com.overshop.app.seller.fragments.SellerDashboardFragment
import com.overshop.app.seller.fragments.SellerProductsFragment
import com.overshop.app.seller.fragments.SellerOrdersFragment
import com.overshop.app.seller.fragments.SellerProfileFragment
import android.widget.Toast
import com.overshop.app.auth.AuthRepository

/** Contenedor de navegación y secciones disponibles para el rol Vendedor. */
class SellerActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = AuthRepository.getCurrentUser()

        if (currentUser?.role != "Vendedor") {
            Toast.makeText(
                this,
                "Acceso denegado. Se requiere una cuenta de vendedor.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        setContentView(R.layout.activity_seller)

        bottomNavigation = findViewById(R.id.sellerBottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->

            val fragment: Fragment = when (item.itemId) {

                R.id.nav_seller_dashboard ->
                    SellerDashboardFragment()

                R.id.nav_seller_products ->
                    SellerProductsFragment()

                R.id.nav_seller_orders ->
                    SellerOrdersFragment()

                R.id.nav_seller_profile ->
                    SellerProfileFragment()

                else -> SellerDashboardFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.sellerFragmentContainer, fragment)
                .commit()

            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId =
                R.id.nav_seller_dashboard
        }
    }
}
