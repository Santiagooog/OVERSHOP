package com.overshop.app.repositories

import com.overshop.app.models.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.overshop.app.auth.AuthRepository
import java.text.SimpleDateFormat
import java.util.Locale

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

    /** Atomically writes a simulated order and removes the buyer's cart documents. */
    suspend fun saveSimulatedOrderAndClearCart(order: Order): Boolean {
        return try {
            val uid = AuthRepository.getCurrentUser()?.uid ?: return false
            val cartItems = firestore.collection("carts").document(uid).collection("items").get().await()
            val batch = firestore.batch()
            batch.set(firestore.collection("orders").document(order.reference), order)
            cartItems.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            orders.add(0, order)
            CartRepository.clearCart()
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
            val user = AuthRepository.getCurrentUser()
            val query = if (user?.role == "Comprador") {
                firestore.collection("orders").whereEqualTo("buyerUid", user.uid)
            } else firestore.collection("orders")
            val result = query.get().await()
            result.documents.mapNotNull { it.toObject(Order::class.java) }
                .also { cloudOrders ->
                    orders.clear()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO"))
                    orders.addAll(cloudOrders.sortedByDescending { order ->
                        runCatching { dateFormat.parse(order.date)?.time ?: 0L }.getOrDefault(0L)
                    })
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
