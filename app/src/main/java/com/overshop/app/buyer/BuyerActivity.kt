package com.overshop.app.buyer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.overshop.app.R
import com.overshop.app.buyer.fragments.CartFragment
import com.overshop.app.buyer.fragments.CatalogFragment
import com.overshop.app.buyer.fragments.HomeFragment
import com.overshop.app.buyer.fragments.ProfileFragment
import com.overshop.app.auth.AuthRepository
import android.widget.Toast
class BuyerActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = AuthRepository.getCurrentUser()

        if (currentUser?.role != "Comprador") {
            Toast.makeText(
                this,
                "Acceso denegado. Se requiere una cuenta de comprador.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        setContentView(R.layout.activity_buyer)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }

                R.id.nav_catalog -> {
                    loadFragment(CatalogFragment())
                    true
                }

                R.id.nav_cart -> {
                    loadFragment(CartFragment())
                    true
                }

                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.buyerFragmentContainer, fragment)
            .commit()
    }

    /** Opens the catalog with a category selected from the home screen. */
    fun openCatalog(category: String) {
        val catalog = CatalogFragment().apply {
            arguments = Bundle().apply { putString(CatalogFragment.ARG_CATEGORY, category) }
        }
        bottomNavigation.selectedItemId = R.id.nav_catalog
        loadFragment(catalog)
    }
}
