package com.overshop.app.repositories

import com.overshop.app.models.UserProfile

object UserRepository {

    private var currentUser: UserProfile? = null

    fun saveUser(user: UserProfile) {
        currentUser = user
    }

    fun getUser(): UserProfile? {
        return currentUser
    }

    fun updateUser(user: UserProfile) {
        currentUser = user
    }

    fun isUserRegistered(): Boolean {
        return currentUser != null
    }

    fun clearUser() {
        currentUser = null
    }
}
