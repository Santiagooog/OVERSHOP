package com.overshop.app.cart

import com.overshop.app.models.Product

data class CartItem(
    val product: Product = Product(),
    val size: String = "",
    var quantity: Int = 0
) {
    val subtotal: Int
        get() = product.price * quantity
}
