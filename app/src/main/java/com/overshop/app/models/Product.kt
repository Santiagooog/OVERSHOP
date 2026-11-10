package com.overshop.app.models

import androidx.annotation.DrawableRes

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Int,
    @DrawableRes val imageRes: Int,
    val description: String
)
