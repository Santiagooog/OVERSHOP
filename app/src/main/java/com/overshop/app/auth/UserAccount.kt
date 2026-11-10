package com.overshop.app.auth

data class UserAccount(
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val role: String
)
