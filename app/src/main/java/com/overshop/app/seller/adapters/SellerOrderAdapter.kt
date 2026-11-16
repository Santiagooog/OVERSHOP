package com.overshop.app.seller.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.models.Order
import java.text.NumberFormat
import java.util.Locale

/** Vincula pedidos recibidos con su estado y controles de actualización del Vendedor. */
class SellerOrderAdapter(
    private var orders: List<Order>,
    private val onStatusChanged: (Order, String) -> Unit
) : RecyclerView.Adapter<SellerOrderAdapter.OrderViewHolder>() {

    private val statuses = listOf(
        "Confirmado",
        "Preparando",
        "Enviado",
        "Entregado",
        "Cancelado"
    )

    /** Mantiene las vistas y el selector de estado de una tarjeta de pedido. */
    inner class OrderViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val reference: TextView =
            view.findViewById(R.id.tvSellerOrderReference)

        val customer: TextView =
            view.findViewById(R.id.tvSellerOrderCustomer)

        val date: TextView =
            view.findViewById(R.id.tvSellerOrderDate)

        val items: TextView =
            view.findViewById(R.id.tvSellerOrderItems)

        val payment: TextView =
            view.findViewById(R.id.tvSellerOrderPayment)

        val total: TextView =
            view.findViewById(R.id.tvSellerOrderTotal)

        val status: Spinner =
            view.findViewById(R.id.spinnerSellerOrderStatus)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seller_order, parent, false)

        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrderViewHolder,
        position: Int
    ) {

        val order = orders[position]

        holder.reference.text =
            "Referencia: ${order.reference}"

        holder.customer.text =
            "Comprador: ${order.customerName}"

        holder.date.text =
            "Fecha: ${order.date}"

        holder.items.text = order.items.joinToString("\n\n") {
            "${it.productName}\n" +
                    "Talla: ${it.size} | Cantidad: ${it.quantity}\n" +
                    "Subtotal: ${formatCurrency(it.subtotal)}"
        }

        holder.payment.text =
            "Pago simulado (sin cobro) · ${order.paymentMethod}"

        holder.total.text =
            "TOTAL: ${formatCurrency(order.total)}"

        val spinnerAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            statuses
        )

        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        holder.status.adapter = spinnerAdapter

        val selectedIndex = statuses.indexOf(order.status)
            .let { if (it >= 0) it else 0 }

        holder.status.setSelection(selectedIndex, false)
        holder.status.isEnabled = order.paymentStatus == "simulated"

        holder.status.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val newStatus = statuses[position]

                    if (newStatus != order.status) {
                        onStatusChanged(order, newStatus)
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit
            }
    }

    override fun getItemCount(): Int = orders.size

    /** Replaces the visible seller order list after a database refresh. */
    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    /** Formats order totals for the seller's order cards. */
    private fun formatCurrency(amount: Int): String {
        return NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        ).format(amount)
    }
}
