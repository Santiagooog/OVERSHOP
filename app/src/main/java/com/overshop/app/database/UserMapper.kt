package com.overshop.app.database

import com.overshop.app.auth.UserAccount

// Convierte una entidad local en un perfil de usuario.
/** Maps the local Room representation to the account model used by authentication flows. */
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
/** Maps an account model into the local Room representation. */
fun UserAccount.toUserEntity(): UserEntity {
    return UserEntity(
        uid = uid,
        fullName = fullName,
        email = email,
        phone = phone,
        role = role
    )
}
