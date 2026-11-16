package com.overshop.app.repositories

import com.overshop.app.cart.CartItem
import com.overshop.app.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.overshop.app.auth.AuthRepository
import kotlinx.coroutines.tasks.await

/** Keeps the cart synchronized with the signed-in buyer's Firestore cart and local UI state. */
object CartRepository {

    private val items = mutableListOf<CartItem>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    /** Resolves the Firestore cart document for the currently authenticated buyer. */
    private fun cartCollection() = firestore.collection("carts").document(
        AuthRepository.getCurrentUser()?.uid ?: throw IllegalStateException("Sesión requerida")
    ).collection("items")

    /** Reads persisted cart lines from Firestore and maps them to displayable cart items. */
    suspend fun loadCloudItems(): List<CartItem> {
        val fetched = cartCollection().get().await().documents.mapNotNull { it.toObject(CartItem::class.java) }
        items.clear(); items.addAll(fetched)
        return getItems()
    }

    /** Adds a product/size line or increments its quantity in the buyer's cloud cart. */
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

    /** Persists a quantity change, deleting the line when the new quantity is zero. */
    suspend fun updateCloudQuantity(productId: Int, size: String, quantity: Int) {
        val ref = cartCollection().document("${productId}_$size")
        if (quantity <= 0) ref.delete().await() else ref.update("quantity", quantity).await()
        updateQuantity(productId, size, quantity)
    }

    /** Removes one product/size line from the active buyer's Firestore cart. */
    suspend fun removeCloudItem(productId: Int, size: String) {
        cartCollection().document("${productId}_$size").delete().await()
        removeItem(productId, size)
    }

    /** Deletes all persisted cart lines after checkout or an explicit cart reset. */
    suspend fun clearCloudCart() {
        val docs = cartCollection().get().await().documents
        val batch = firestore.batch()
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
        clearCart()
    }

    /** Replaces a matching local cart line so list screens immediately reflect cloud changes. */
    private fun updateLocal(item: CartItem) {
        items.removeAll { it.product.id == item.product.id && it.size == item.size }
        items.add(item)
    }

    /** Adds a new local line or increases the existing product-and-size quantity. */
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

    /** Returns a copy of the in-memory cart lines for rendering. */
    fun getItems(): List<CartItem> {
        return items.toList()
    }

    /** Updates a cached line quantity and removes it when the requested value is zero. */
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

    /** Removes the matching product-and-size line from local cart state. */
    fun removeItem(productId: Int, size: String) {
        items.removeAll {
            it.product.id == productId && it.size == size
        }
    }

    /** Calculates the cart subtotal in the app's integer currency units. */
    fun getTotal(): Int {
        return items.sumOf { it.subtotal }
    }

    /** Returns the sum of quantities across all cached cart lines. */
    fun getItemCount(): Int {
        return items.sumOf { it.quantity }
    }

    /** Clears the in-memory cart after the cloud checkout has completed. */
    fun clearCart() {
        items.clear()
    }

    /** Indicates whether the in-memory cart currently has no lines. */
    fun isEmpty(): Boolean {
        return items.isEmpty()
    }
}
