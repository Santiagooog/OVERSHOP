package com.overshop.app.buyer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.overshop.app.R
import com.overshop.app.models.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val image: ImageView = view.findViewById(R.id.imgProduct)
        val name: TextView = view.findViewById(R.id.tvProductName)
        val category: TextView =
            view.findViewById(R.id.tvProductCategory)
        val price: TextView = view.findViewById(R.id.tvProductPrice)
        val button: Button = view.findViewById(R.id.btnViewProduct)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)

        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductViewHolder,
        position: Int
    ) {
        val product = products[position]

        if (product.imageUrl.isNotBlank()) {
            Glide.with(holder.image).load(product.imageUrl).placeholder(product.imageRes).into(holder.image)
        } else if (product.imageRes != 0) holder.image.setImageResource(product.imageRes)
        holder.name.text = product.name
        holder.category.text = product.category

        val formatter = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        )
        formatter.maximumFractionDigits = 0
        holder.price.text = formatter.format(product.price)

        holder.button.setOnClickListener {
            onProductClick(product)
        }

        holder.itemView.setOnClickListener {
            onProductClick(product)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
