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
import androidx.lifecycle.lifecycleScope
import com.overshop.app.R
import com.overshop.app.buyer.adapters.ProductAdapter
import com.overshop.app.models.Product
import android.content.Intent
import com.overshop.app.seller.adapters.ProductDetailActivity
import com.overshop.app.repositories.ProductRepository
import kotlinx.coroutines.launch
class CatalogFragment : Fragment(R.layout.fragment_catalog) {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var tvCatalogCount: TextView

    private var selectedCategory = "Todos"
    private var searchQuery = ""

    private var products: List<Product> = emptyList()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        selectedCategory = arguments?.getString(ARG_CATEGORY) ?: "Todos"

        tvCatalogCount = view.findViewById(R.id.tvCatalogCount)

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.recyclerProducts)


        productAdapter = ProductAdapter(emptyList()) { product ->

            val intent = android.content.Intent(
                requireContext(),
                ProductDetailActivity::class.java
            ).apply {

                putExtra("product_id", product.id)
                putExtra("product_name", product.name)
                putExtra("product_category", product.category)
                putExtra("product_price", product.price)
                putExtra("product_image", product.imageRes)
                putExtra("product_image_url", product.imageUrl)
                putExtra("product_document_id", product.documentId)
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
            R.id.btnCatalogPants to "Pantalones",
            R.id.btnCatalogAccessories to "Accesorios"
        )

        categories.forEach { (buttonId, category) ->
            view.findViewById<Button>(buttonId).setOnClickListener {
                selectedCategory = category
                applyFilters()
            }
        }

        applyFilters()
        loadProducts()
    }

    private fun loadProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                products = ProductRepository.getProducts()
                applyFilters()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "No se pudo cargar el catálogo. Revisa la conexión.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun applyFilters() {
        val filtered = products.filter { product ->
            val matchesCategory =
                selectedCategory == "Todos" ||
                        product.category.equals(selectedCategory, ignoreCase = true)

            val matchesSearch =
                product.name.contains(searchQuery, ignoreCase = true) ||
                        product.category.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }

        productAdapter.updateProducts(filtered)
        tvCatalogCount.text = "${filtered.size} productos encontrados"
    }

    companion object {
        const val ARG_CATEGORY = "catalog_category"
    }
}
