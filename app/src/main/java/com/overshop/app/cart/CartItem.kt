package com.overshop.app.cart

import com.overshop.app.models.Product

data class CartItem(
    val product: Product,
    val size: String,
    var quantity: Int
) {
    val subtotal: Int
        get() = product.price * quantity
}
