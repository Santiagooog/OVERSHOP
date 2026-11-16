package com.overshop.app.repositories

import com.overshop.app.models.UserProfile

/** Provides the lightweight local cache used to prefill the signed-in user's profile form. */
object UserRepository {

    private var currentUser: UserProfile? = null

    /** Replaces the cached profile with the latest values entered by the user. */
    fun saveUser(user: UserProfile) {
        currentUser = user
    }

    /** Returns the cached profile, if one has been loaded for this app process. */
    fun getUser(): UserProfile? {
        return currentUser
    }

    /** Updates the cached profile after a successful profile or checkout save. */
    fun updateUser(user: UserProfile) {
        currentUser = user
    }

    /** Indicates whether a profile is present in this process's local cache. */
    fun isUserRegistered(): Boolean {
        return currentUser != null
    }

    /** Clears the process-local profile cache without deleting the Firestore profile. */
    fun clearUser() {
        currentUser = null
    }
}
