
package com.overshop.app.auth

/** Modelo de perfil asociado a una identidad de Firebase Authentication. */
data class UserAccount(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val address: String = "",
    val city: String = ""
)
