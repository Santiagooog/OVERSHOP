package com.overshop.app.models

/** Datos de contacto y entrega usados para completar el perfil del comprador. */
data class UserProfile(
    var name: String,
    var email: String,
    var phone: String,
    var address: String,
    var city: String
)
