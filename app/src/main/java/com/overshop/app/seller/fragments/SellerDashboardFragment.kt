package com.overshop.app.seller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.overshop.app.R
import com.overshop.app.repositories.ProductRepository
import com.overshop.app.repositories.OrderRepository
import java.text.NumberFormat
import java.util.Locale

class SellerDashboardFragment :
    Fragment(R.layout.fragment_seller_dashboard) {

    private lateinit var tvProductCount: TextView
    private lateinit var tvOrderCount: TextView
    private lateinit var tvSalesTotal: TextView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        tvProductCount =
            view.findViewById(R.id.tvSellerProductCount)

        tvOrderCount =
            view.findViewById(R.id.tvSellerOrderCount)

        tvSalesTotal =
            view.findViewById(R.id.tvSellerSalesTotal)

        val btnProducts = view.findViewById<Button>(
            R.id.btnSellerManageProducts
        )

        val btnOrders = view.findViewById<Button>(
            R.id.btnSellerViewOrders
        )

        btnProducts.setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(
                    R.id.sellerBottomNavigation
                )
                .selectedItemId = R.id.nav_seller_products
        }

        btnOrders.setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(
                    R.id.sellerBottomNavigation
                )
                .selectedItemId = R.id.nav_seller_orders
        }

        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()

        if (::tvProductCount.isInitialized) {
            refreshDashboard()
        }
    }

    private fun refreshDashboard() {

        val products = ProductRepository.getProducts()
        val orders = OrderRepository.getOrders()

        // Cantidad total de productos registrados
        tvProductCount.text = products.size.toString()

        // Cantidad total de pedidos registrados
        tvOrderCount.text = orders.size.toString()

        // Sumar pedidos que no estén cancelados
        val totalSales = orders
            .filter { it.status != "Cancelado" }
            .sumOf { it.total }

        tvSalesTotal.text = formatCurrency(totalSales)
    }

    private fun formatCurrency(amount: Int): String {

        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
