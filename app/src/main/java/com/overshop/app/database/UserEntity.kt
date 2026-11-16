package com.overshop.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
/** Entidad Room que representa un usuario en la base de datos local. */
data class UserEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val uid: String = "",

    val fullName: String,

    val email: String,

    val phone: String,

    val role: String
)
