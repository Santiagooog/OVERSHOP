package com.overshop.app.cart.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.cart.CartItem
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var items: List<CartItem>,
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imgCartProduct)
        val name: TextView = view.findViewById(R.id.tvCartName)
        val size: TextView = view.findViewById(R.id.tvCartSize)
        val unitPrice: TextView = view.findViewById(R.id.tvCartUnitPrice)
        val quantity: TextView = view.findViewById(R.id.tvCartQuantity)
        val subtotal: TextView = view.findViewById(R.id.tvCartSubtotal)

        val decrease: Button = view.findViewById(R.id.btnCartDecrease)
        val increase: Button = view.findViewById(R.id.btnCartIncrease)
        val remove: Button = view.findViewById(R.id.btnCartRemove)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)

        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]

        holder.image.setImageResource(item.product.imageRes)
        holder.name.text = item.product.name
        holder.size.text = "Talla: ${item.size}"
        holder.unitPrice.text = "Precio: ${formatCurrency(item.product.price)}"
        holder.quantity.text = item.quantity.toString()
        holder.subtotal.text = "Subtotal: ${formatCurrency(item.subtotal)}"

        holder.increase.setOnClickListener {
            onIncrease(item)
        }

        holder.decrease.setOnClickListener {
            onDecrease(item)
        }

        holder.remove.setOnClickListener {
            onRemove(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
