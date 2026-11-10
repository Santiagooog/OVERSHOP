package com.overshop.app.admin.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.overshop.app.R
import com.overshop.app.models.Order
import com.overshop.app.repositories.OrderRepository
import java.text.NumberFormat
import java.util.Locale

class AdminReportsFragment :
    Fragment(R.layout.fragment_admin_reports) {

    private val statuses = listOf(
        "Confirmado",
        "Preparando",
        "Enviado",
        "Entregado",
        "Cancelado"
    )

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        refreshReports(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshReports(it) }
    }

    private fun refreshReports(view: View) {

        val orders = OrderRepository.getOrders()

        val totalOrders = orders.size

        val totalSales = orders
            .filter { it.status != "Cancelado" }
            .sumOf { it.total }

        val deliveredSales = orders
            .filter { it.status == "Entregado" }
            .sumOf { it.total }

        view.findViewById<TextView>(
            R.id.tvReportsOrderCount
        ).text = "Pedidos registrados: $totalOrders"

        view.findViewById<TextView>(
            R.id.tvReportsSales
        ).text = "Ventas acumuladas: ${currencyFormat.format(totalSales)}"

        view.findViewById<TextView>(
            R.id.tvReportsDelivered
        ).text = "Ventas entregadas: ${currencyFormat.format(deliveredSales)}"

        renderStatuses(view, orders)
        renderOrders(view, orders)
    }

    private fun renderStatuses(
        view: View,
        orders: List<Order>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.layoutOrderStatuses
        )

        container.removeAllViews()

        statuses.forEach { status ->

            val count = orders.count {
                it.status == status
            }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(13), dp(14), dp(13))
                background = requireContext().getDrawable(
                    R.drawable.bg_admin_stat
                )
            }

            val title = TextView(requireContext()).apply {
                text = status
                textSize = 14f
                setTextColor(
                    requireContext().getColor(R.color.overshop_white)
                )
            }

            val value = TextView(requireContext()).apply {
                text = count.toString()
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(
                    requireContext().getColor(R.color.overshop_orange)
                )
                gravity = Gravity.END
            }

            row.addView(
                title,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            row.addView(value)

            container.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            )
        }
    }

    private fun renderOrders(
        view: View,
        orders: List<Order>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.layoutReportsOrders
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvReportsEmpty
        )

        container.removeAllViews()

        emptyText.visibility =
            if (orders.isEmpty()) View.VISIBLE else View.GONE

        orders.forEach { order ->
            container.addView(createOrderCard(order))
        }
    }

    private fun createOrderCard(order: Order): LinearLayout {

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = requireContext().getDrawable(
                R.drawable.bg_admin_stat
            )
        }

        fun addText(
            value: String,
            color: Int,
            size: Float,
            bold: Boolean = false
        ) {
            val textView = TextView(requireContext()).apply {
                text = value
                textSize = size
                setTextColor(requireContext().getColor(color))

                if (bold) {
                    setTypeface(null, Typeface.BOLD)
                }
            }

            card.addView(
                textView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(6)
                }
            )
        }

        addText(
            order.reference,
            R.color.overshop_white,
            15f,
            true
        )

        addText(
            "Cliente: ${order.customerName}",
            R.color.overshop_text_secondary,
            13f
        )

        addText(
            "Fecha: ${order.date}",
            R.color.overshop_text_secondary,
            13f
        )

        addText(
            "Estado: ${order.status}",
            R.color.overshop_orange,
            13f,
            true
        )

        addText(
            "Productos: ${order.items.sumOf { it.quantity }} unidades",
            R.color.overshop_text_secondary,
            13f
        )

        addText(
            "Total: ${currencyFormat.format(order.total)}",
            R.color.overshop_orange,
            15f,
            true
        )

        return card.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
