package com.overshop.app.repositories

import com.overshop.app.models.Order

object OrderRepository {

    private val orders = mutableListOf<Order>()

    fun saveOrder(order: Order) {
        orders.add(0, order)
    }

    fun getOrders(): List<Order> {
        return orders.toList()
    }

    fun getOrderByReference(reference: String): Order? {
        return orders.find {
            it.reference == reference
        }
    }

    fun getOrderCount(): Int {
        return orders.size
    }

    fun updateOrderStatus(
        reference: String,
        newStatus: String
    ): Boolean {

        val index = orders.indexOfFirst {
            it.reference == reference
        }

        if (index == -1) {
            return false
        }

        orders[index] = orders[index].copy(
            status = newStatus
        )

        return true
    }

    fun clearOrders() {
        orders.clear()
    }
}
