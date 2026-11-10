package com.overshop.app.seller.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.repositories.OrderRepository
import com.overshop.app.seller.adapters.SellerOrderAdapter

class SellerOrdersFragment :
    Fragment(R.layout.fragment_seller_orders) {

    private lateinit var adapter: SellerOrderAdapter
    private lateinit var recyclerOrders: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerOrders =
            view.findViewById(R.id.recyclerSellerOrders)

        tvCount =
            view.findViewById(R.id.tvSellerOrdersCount)

        tvEmpty =
            view.findViewById(R.id.tvSellerOrdersEmpty)

        adapter = SellerOrderAdapter(
            orders = emptyList(),
            onStatusChanged = { order, newStatus ->

                val updated = OrderRepository.updateOrderStatus(
                    order.reference,
                    newStatus
                )

                if (updated) {
                    Toast.makeText(
                        requireContext(),
                        "Estado actualizado: $newStatus",
                        Toast.LENGTH_SHORT
                    ).show()

                    refreshOrders()
                }
            }
        )

        recyclerOrders.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerOrders.adapter = adapter

        refreshOrders()
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized) {
            refreshOrders()
        }
    }

    private fun refreshOrders() {

        val orders = OrderRepository.getOrders()

        adapter.updateOrders(orders)

        tvCount.text =
            "Pedidos recibidos: ${orders.size}"

        val empty = orders.isEmpty()

        tvEmpty.visibility =
            if (empty) View.VISIBLE else View.GONE

        recyclerOrders.visibility =
            if (empty) View.GONE else View.VISIBLE
    }
}
