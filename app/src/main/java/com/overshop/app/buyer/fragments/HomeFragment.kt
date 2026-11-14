package com.overshop.app.buyer.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.buyer.BuyerActivity
import com.overshop.app.buyer.adapters.ProductAdapter
import com.overshop.app.models.Product
import com.overshop.app.repositories.ProductRepository
import com.overshop.app.seller.adapters.ProductDetailActivity
import kotlinx.coroutines.launch

/** Home page with category shortcuts and a best-selling product selection. */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var productsAdapter: ProductAdapter
    private lateinit var featuredList: RecyclerView
    private lateinit var emptyMessage: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        featuredList = view.findViewById(R.id.recyclerFeaturedProducts)
        emptyMessage = view.findViewById(R.id.tvFeaturedEmpty)
        productsAdapter = ProductAdapter(emptyList(), ::openProduct)
        featuredList.layoutManager = GridLayoutManager(requireContext(), 2)
        featuredList.adapter = productsAdapter

        view.findViewById<View>(R.id.tvExploreCollection).setOnClickListener {
            (activity as? BuyerActivity)?.openCatalog("Todos")
        }

        mapOf(
            R.id.btnHoodies to "Hoodies",
            R.id.btnTshirts to "Camisetas",
            R.id.btnPants to "Pantalones",
            R.id.btnAccessories to "Accesorios"
        ).forEach { (buttonId, category) ->
            view.findViewById<Button>(buttonId).setOnClickListener {
                (activity as? BuyerActivity)?.openCatalog(category)
            }
        }

        loadFeaturedProducts()
    }

    /** Loads top-selling products and fills missing spots with random catalog items. */
    private fun loadFeaturedProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val products = ProductRepository.getProducts()
                val ranked = products.filter { it.salesCount > 0 }
                    .sortedByDescending { it.salesCount }
                    .take(3)
                val remaining = products.filterNot { it in ranked }.shuffled()
                val featured = (ranked + remaining.take(3 - ranked.size)).take(3)

                productsAdapter.updateProducts(featured)
                featuredList.layoutParams = featuredList.layoutParams.apply {
                    val rowCount = (featured.size + 1) / 2
                    val rowHeight = (350 * resources.displayMetrics.density).toInt()
                    height = rowCount * rowHeight
                }
                emptyMessage.visibility = if (featured.isEmpty()) View.VISIBLE else View.GONE
            } catch (_: Exception) {
                emptyMessage.visibility = View.VISIBLE
                emptyMessage.text = "No se pudieron cargar los productos. Revisa tu conexión."
                Toast.makeText(requireContext(), "No se pudieron cargar los productos destacados.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Opens the selected product's detail screen. */
    private fun openProduct(product: Product) {
        val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
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
}
