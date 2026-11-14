package com.overshop.app.repositories

import com.overshop.app.cart.CartItem
import com.overshop.app.models.Product

object CartRepository {

    private val items = mutableListOf<CartItem>()

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