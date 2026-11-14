package com.overshop.app.models

data class OrderItem(
    val productName: String = "",
    val productImage: Int = 0,
    val price: Int = 0,
    val size: String = "",
    val quantity: Int = 0
) {
    val subtotal: Int
        get() = price * quantity
}

data class Order(
    val reference: String = "",
    val customerName: String = "",
    val date: String = "",
    val paymentMethod: String = "",
    val items: List<OrderItem> = emptyList(),
    val total: Int = 0,
    val buyerUid: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val status: String = "Confirmado"
)
