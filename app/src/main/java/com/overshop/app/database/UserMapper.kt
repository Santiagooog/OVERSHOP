package com.overshop.app.database

import com.overshop.app.auth.UserAccount

fun UserEntity.toUserAccount(): UserAccount {
    return UserAccount(
        fullName = fullName,
        email = email,
        phone = phone,
        passwordHash = passwordHash,
        role = role
    )
}

fun UserAccount.toUserEntity(): UserEntity {
    return UserEntity(
        fullName = fullName,
        email = email,
        phone = phone,
        passwordHash = passwordHash,
        role = role
    )
}
