package com.overshop.app.seller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.models.Product
import java.text.NumberFormat
import java.util.Locale

class SellerProductAdapter(
    private var products: List<Product>,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<SellerProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val image: ImageView =
            view.findViewById(R.id.imgSellerProduct)

        val name: TextView =
            view.findViewById(R.id.tvSellerProductName)

        val category: TextView =
            view.findViewById(R.id.tvSellerProductCategory)

        val price: TextView =
            view.findViewById(R.id.tvSellerProductPrice)

        val edit: Button =
            view.findViewById(R.id.btnEditSellerProduct)

        val delete: Button =
            view.findViewById(R.id.btnDeleteSellerProduct)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seller_product, parent, false)

        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductViewHolder,
        position: Int
    ) {

        val product = products[position]

        holder.image.setImageResource(product.imageRes)
        holder.name.text = product.name
        holder.category.text = product.category

        holder.price.text = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(product.price)

        holder.edit.setOnClickListener {
            onEdit(product)
        }

        holder.delete.setOnClickListener {
            onDelete(product)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}