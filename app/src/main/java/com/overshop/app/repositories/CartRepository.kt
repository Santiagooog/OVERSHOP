package com.overshop.app.repositories

import com.overshop.app.cart.CartItem
import com.overshop.app.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.overshop.app.auth.AuthRepository
import kotlinx.coroutines.tasks.await

object CartRepository {

    private val items = mutableListOf<CartItem>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private fun cartCollection() = firestore.collection("carts").document(
        AuthRepository.getCurrentUser()?.uid ?: throw IllegalStateException("Sesión requerida")
    ).collection("items")

    suspend fun loadCloudItems(): List<CartItem> {
        val fetched = cartCollection().get().await().documents.mapNotNull { it.toObject(CartItem::class.java) }
        items.clear(); items.addAll(fetched)
        return getItems()
    }

    suspend fun addCloudItem(product: Product, size: String, quantity: Int) {
        val id = "${product.id}_$size"
        val ref = cartCollection().document(id)
        val next = firestore.runTransaction { transaction ->
            val current = transaction.get(ref).toObject(CartItem::class.java)
            (if (current == null) CartItem(product, size, quantity) else current.copy(quantity = current.quantity + quantity))
                .also { transaction.set(ref, it) }
        }.await()
        updateLocal(next)
    }

    suspend fun updateCloudQuantity(productId: Int, size: String, quantity: Int) {
        val ref = cartCollection().document("${productId}_$size")
        if (quantity <= 0) ref.delete().await() else ref.update("quantity", quantity).await()
        updateQuantity(productId, size, quantity)
    }

    suspend fun removeCloudItem(productId: Int, size: String) {
        cartCollection().document("${productId}_$size").delete().await()
        removeItem(productId, size)
    }

    suspend fun clearCloudCart() {
        val docs = cartCollection().get().await().documents
        val batch = firestore.batch()
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
        clearCart()
    }

    private fun updateLocal(item: CartItem) {
        items.removeAll { it.product.id == item.product.id && it.size == item.size }
        items.add(item)
    }

    fun addItem(product: Product, size: String, quantity: Int) {
        val existingItem = items.find {
            it.product.id == product.id && it.size == size
        }

        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            items.add(
                CartItem(
                    product = product,
                    size = size,
                    quantity = quantity
                )
            )
        }
    }

    fun getItems(): List<CartItem> {
        return items.toList()
    }

    fun updateQuantity(productId: Int, size: String, quantity: Int) {
        val item = items.find {
            it.product.id == productId && it.size == size
        } ?: return

        if (quantity <= 0) {
            items.remove(item)
        } else {
            item.quantity = quantity
        }
    }

    fun removeItem(productId: Int, size: String) {
        items.removeAll {
            it.product.id == productId && it.size == size
        }
    }

    fun getTotal(): Int {
        return items.sumOf { it.subtotal }
    }

    fun getItemCount(): Int {
        return items.sumOf { it.quantity }
    }

    fun clearCart() {
        items.clear()
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }
}
