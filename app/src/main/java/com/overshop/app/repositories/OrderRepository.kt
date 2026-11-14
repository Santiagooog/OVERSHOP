package com.overshop.app.repositories

import com.overshop.app.models.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object OrderRepository {

    private val orders = mutableListOf<Order>()

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun saveOrder(order: Order): Boolean {
        return try {
            firestore.collection("orders").document(order.reference).set(order).await()
            orders.add(0, order)
            true
        } catch (_: Exception) {
            false
        }
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

    suspend fun updateOrderStatus(
        reference: String,
        newStatus: String
    ): Boolean {

        return try {
            firestore.collection("orders").document(reference)
                .update("status", newStatus).await()
            val index = orders.indexOfFirst { it.reference == reference }
            if (index >= 0) orders[index] = orders[index].copy(status = newStatus)
            true
        } catch (_: Exception) { false }
    }

    suspend fun getOrdersFromCloud(): List<Order> {
        return try {
            val result = firestore.collection("orders").get().await()
            result.documents.mapNotNull { it.toObject(Order::class.java) }
                .also { cloudOrders ->
                    orders.clear()
                    orders.addAll(cloudOrders.sortedByDescending { it.date })
                }
        } catch (_: Exception) { orders.toList() }
    }

    /*
    fun updateOrderStatusLocalOnly(
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
    */

    fun clearOrders() {
        orders.clear()
    }
}
