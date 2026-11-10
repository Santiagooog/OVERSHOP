package com.overshop.app.buyer.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.buyer.adapters.ProductAdapter
import com.overshop.app.models.Product
import android.content.Intent
import com.overshop.app.seller.adapters.ProductDetailActivity
import com.overshop.app.repositories.ProductRepository
class CatalogFragment : Fragment(R.layout.fragment_catalog) {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var tvCatalogCount: TextView

    private var selectedCategory = "Todos"
    private var searchQuery = ""

    private val products = ProductRepository.getProducts()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        tvCatalogCount = view.findViewById(R.id.tvCatalogCount)

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.recyclerProducts)


        productAdapter = ProductAdapter(products) { product ->

            val intent = android.content.Intent(
                requireContext(),
                ProductDetailActivity::class.java
            ).apply {

                putExtra("product_id", product.id)
                putExtra("product_name", product.name)
                putExtra("product_category", product.category)
                putExtra("product_price", product.price)
                putExtra("product_image", product.imageRes)
                putExtra("product_description", product.description)

            }

            startActivity(intent)
        }


        recyclerView.layoutManager =
            GridLayoutManager(requireContext(), 2)

        recyclerView.adapter = productAdapter



        val search = view.findViewById<EditText>(
            R.id.etCatalogSearch
        )

        search.doAfterTextChanged { text ->
            searchQuery = text.toString()
            applyFilters()
        }

        val categories = mapOf(
            R.id.btnAll to "Todos",
            R.id.btnCatalogHoodies to "Hoodies",
            R.id.btnCatalogTshirts to "Camisetas",
            R.id.btnCatalogPants to "Pantalones"
        )

        categories.forEach { (buttonId, category) ->
            view.findViewById<Button>(buttonId).setOnClickListener {
                selectedCategory = category
                applyFilters()
            }
        }

        applyFilters()
    }

    private fun applyFilters() {
        val filtered = products.filter { product ->
            val matchesCategory =
                selectedCategory == "Todos" ||
                        product.category == selectedCategory

            val matchesSearch =
                product.name.contains(searchQuery, ignoreCase = true) ||
                        product.category.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }

        productAdapter.updateProducts(filtered)
        tvCatalogCount.text = "${filtered.size} productos encontrados"
    }
}
