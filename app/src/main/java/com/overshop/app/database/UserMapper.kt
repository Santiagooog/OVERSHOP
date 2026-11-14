package com.overshop.app.database

import com.overshop.app.auth.UserAccount

// Convierte una entidad local en un perfil de usuario.
fun UserEntity.toUserAccount(): UserAccount {
    return UserAccount(
        uid = uid,
        fullName = fullName,
        email = email,
        phone = phone,
        role = role
    )
}

// Convierte un perfil en una entidad de almacenamiento local.
fun UserAccount.toUserEntity(): UserEntity {
    return UserEntity(
        uid = uid,
        fullName = fullName,
        email = email,
        phone = phone,
        role = role
    )
}
