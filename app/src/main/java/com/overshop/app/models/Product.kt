package com.overshop.app.models

import androidx.annotation.DrawableRes

/** Modelo de producto del catálogo, incluyendo imagen y contador de ventas. */
data class Product(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val price: Int = 0,
    @DrawableRes val imageRes: Int = 0,
    val description: String = "",
    val documentId: String = "",
    val imageUrl: String = "",
    val salesCount: Int = 0
)
