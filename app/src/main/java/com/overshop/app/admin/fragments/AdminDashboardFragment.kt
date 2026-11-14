package com.overshop.app.admin.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.overshop.app.R
import com.overshop.app.auth.AuthRepository
import com.overshop.app.repositories.OrderRepository
import com.overshop.app.repositories.ProductRepository
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshDashboard(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshDashboard(it) }
    }

    private fun refreshDashboard(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {

        val totalUsers = AuthRepository.getAccountCount()
        val buyers = AuthRepository.getAccountCountByRole("Comprador")
        val sellers = AuthRepository.getAccountCountByRole("Vendedor")
        val products = try { ProductRepository.getProductCount() } catch (_: Exception) { 0 }

        val orders = OrderRepository.getOrdersFromCloud()
        val totalOrders = orders.size

        val totalSales = orders
            .filter { it.paymentStatus == "approved" && it.status != "Cancelado" }
            .sumOf { it.total }

        setStat(view, R.id.statTotalUsers, "Total usuarios", totalUsers.toString())
        setStat(view, R.id.statBuyers, "Compradores", buyers.toString())
        setStat(view, R.id.statSellers, "Vendedores", sellers.toString())
        setStat(view, R.id.statProducts, "Productos", products.toString())
        setStat(view, R.id.statOrders, "Pedidos", totalOrders.toString())

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        setStat(
            view,
            R.id.statSales,
            "Ventas acumuladas",
            currencyFormat.format(totalSales)
        )
        }
    }

    private fun setStat(
        view: View,
        containerId: Int,
        title: String,
        value: String
    ) {
        val container = view.findViewById<LinearLayout>(containerId)

        container.findViewById<TextView>(R.id.tvStatTitle).text = title
        container.findViewById<TextView>(R.id.tvStatValue).text = value
    }
}
