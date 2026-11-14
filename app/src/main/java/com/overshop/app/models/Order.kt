package com.overshop.app.models

data class OrderItem(
    val productDocumentId: String = "",
    val productId: Int = 0,
    val productName: String = "",
    val productImage: Int = 0,
    val productImageUrl: String = "",
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
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "Confirmado",
    val paymentProvider: String = "",
    val paymentStatus: String = "",
    val paymentReference: String = "",
    val paymentTransactionId: String = ""
)
