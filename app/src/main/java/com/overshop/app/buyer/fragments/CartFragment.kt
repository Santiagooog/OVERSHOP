package com.overshop.app.buyer.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.repositories.CartRepository
import com.overshop.app.cart.adapters.CartAdapter
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment(R.layout.fragment_cart) {

    private lateinit var adapter: CartAdapter
    private lateinit var recyclerCart: RecyclerView
    private lateinit var tvCartCount: TextView
    private lateinit var tvCartEmpty: TextView
    private lateinit var tvCartTotal: TextView
    private lateinit var layoutCartSummary: LinearLayout
    private lateinit var btnCheckout: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerCart = view.findViewById(R.id.recyclerCart)
        tvCartCount = view.findViewById(R.id.tvCartCount)
        tvCartEmpty = view.findViewById(R.id.tvCartEmpty)
        tvCartTotal = view.findViewById(R.id.tvCartTotal)
        layoutCartSummary = view.findViewById(R.id.layoutCartSummary)
        btnCheckout = view.findViewById(R.id.btnCheckout)

        adapter = CartAdapter(
            items = CartRepository.getItems(),

            onIncrease = { item ->
                CartRepository.updateQuantity(
                    item.product.id,
                    item.size,
                    item.quantity + 1
                )
                refreshCart()
            },

            onDecrease = { item ->
                CartRepository.updateQuantity(
                    item.product.id,
                    item.size,
                    item.quantity - 1
                )
                refreshCart()
            },

            onRemove = { item ->
                CartRepository.removeItem(
                    item.product.id,
                    item.size
                )
                refreshCart()
            }
        )

        recyclerCart.layoutManager = LinearLayoutManager(requireContext())
        recyclerCart.adapter = adapter

        btnCheckout.setOnClickListener {
            if (CartRepository.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Tu carrito está vacío",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val intent = android.content.Intent(
                    requireContext(),
                    com.overshop.app.buyer.CheckoutActivity::class.java
                )

                startActivity(intent)
            }
        }

        refreshCart()
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized) {
            refreshCart()
        }
    }

    private fun refreshCart() {
        val items = CartRepository.getItems()

        adapter.updateItems(items)

        val count = CartRepository.getItemCount()
        tvCartCount.text = "$count productos"

        val empty = items.isEmpty()

        tvCartEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        recyclerCart.visibility = if (empty) View.GONE else View.VISIBLE
        layoutCartSummary.visibility = if (empty) View.GONE else View.VISIBLE

        tvCartTotal.text = formatCurrency(CartRepository.getTotal())
    }

    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
