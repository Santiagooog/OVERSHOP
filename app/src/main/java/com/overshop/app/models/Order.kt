package com.overshop.app.models

data class OrderItem(
    val productName: String,
    val productImage: Int,
    val price: Int,
    val size: String,
    val quantity: Int
) {
    val subtotal: Int
        get() = price * quantity
}

data class Order(
    val reference: String,
    val customerName: String,
    val date: String,
    val paymentMethod: String,
    val items: List<OrderItem>,
    val total: Int,
    val status: String = "Confirmado"
)
