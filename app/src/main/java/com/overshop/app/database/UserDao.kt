package com.overshop.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
/** Consultas y operaciones Room disponibles para la entidad local de usuario. */
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    /** Inserts a local user row and returns the generated row identifier. */
    suspend fun insertUser(user: UserEntity): Long

    @Update
    /** Replaces the stored values for an existing local user row. */
    suspend fun updateUser(user: UserEntity)

    @Delete
    /** Removes the supplied local user row. */
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    /** Returns every user currently stored in the local Room table. */
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    /** Returns the local user matching an email address, if present. */
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    /** Counts all local user rows. */
    suspend fun getUserCount(): Int

    @Query("SELECT COUNT(*) FROM users WHERE role = :role")
    /** Counts local user rows for the requested role. */
    suspend fun getUserCountByRole(role: String): Int

    @Query("DELETE FROM users")
    /** Clears the local user table. */
    suspend fun deleteAllUsers()
}
