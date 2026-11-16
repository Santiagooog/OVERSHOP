package com.overshop.app.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.functions.FirebaseFunctions

/** Coordinates Firebase Authentication sessions and the matching Firestore user profiles. */
object AuthRepository {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var currentUser: UserAccount? = null

    /** Registers a public Buyer or Seller and rolls back Auth if the profile write fails. */
    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): Boolean {

        if (role !in listOf("Comprador", "Vendedor")) {
            return false
        }

        val normalizedEmail = email.trim().lowercase()

        var createdAuthUser: com.google.firebase.auth.FirebaseUser? = null

        return try {

            val result = auth.createUserWithEmailAndPassword(
                normalizedEmail,
                password
            ).await()

            val firebaseUser = result.user
                ?: return false

            createdAuthUser = firebaseUser

            val account = UserAccount(
                uid = firebaseUser.uid,
                fullName = fullName.trim(),
                email = normalizedEmail,
                phone = phone.trim(),
                role = role
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(account)
                .await()

            auth.signOut()
            currentUser = null

            true

        } catch (exception: Exception) {

            try {
                createdAuthUser?.delete()?.await()
            } catch (_: Exception) {
                // Si no se puede eliminar, se requiere revisión.
            }

            auth.signOut()
            currentUser = null

            false
        }
    }

    /** Authenticates by email/password, then loads the user's role and profile from Firestore. */
    suspend fun login(
        email: String,
        password: String
    ): Boolean {

        return try {

            val normalizedEmail = email.trim().lowercase()

            val result = auth.signInWithEmailAndPassword(
                normalizedEmail,
                password
            ).await()

            val firebaseUser = result.user
                ?: return false

            val document = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (!document.exists()) {
                auth.signOut()
                currentUser = null
                return false
            }

            val account = document.toObject(UserAccount::class.java)
            if (account == null) {
                auth.signOut()
                currentUser = null
                return false
            }

            currentUser = account.copy(
                uid = firebaseUser.uid
            )

            true

        } catch (exception: Exception) {

            currentUser = null
            false
        }
    }

    // OBTENER USUARIO AUTENTICADO
    /** Returns the account currently cached for the active Firebase session. */
    fun getCurrentUser(): UserAccount? {
        return currentUser
    }

    /** Restores the profile tied to Firebase Auth's encrypted persistent session. */
    /** Restores the cached account from Firestore after the app process is recreated. */
    suspend fun restoreAuthenticatedSession(): UserAccount? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val snapshot = firestore.collection("users").document(firebaseUser.uid).get().await()
            snapshot.toObject(UserAccount::class.java)?.copy(uid = firebaseUser.uid)
                ?.also { currentUser = it }
        } catch (_: Exception) { null }
    }

    /** Sends Firebase's password reset email to the supplied account. */
    /** Sends Firebase's password-reset email and reports whether the request succeeded. */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email.trim().lowercase()).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    // CERRAR SESIÓN
    /** Ends the Firebase session and clears the in-memory account. */
    fun logout() {
        auth.signOut()
        currentUser = null
    }

    // CONSULTAR TODOS LOS USUARIOS
    /** Loads account profiles for the administrator's user-management screen. */
    suspend fun getAccounts(): List<UserAccount> {
        return try {
            firestore.collection("users")
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(UserAccount::class.java)
                }
        } catch (exception: Exception) {
            emptyList()
        }
    }

    /** Counts all account profiles stored in Firestore. */
    /** Counts stored profiles for the administrator dashboard. */
    suspend fun getAccountCount(): Int = try {
        firestore.collection("users").get().await().size()
    } catch (_: Exception) { 0 }

    /** Counts account profiles assigned to a role in Firestore. */
    /** Counts profiles whose role matches the requested dashboard category. */
    suspend fun getAccountCountByRole(role: String): Int = try {
        firestore.collection("users").whereEqualTo("role", role).get().await().size()
    } catch (_: Exception) { 0 }

    // CONSULTAR USUARIO POR CORREO
    /** Looks up one Firestore profile by its normalized email address. */
    suspend fun getAccountByEmail(email: String): UserAccount? {
        return try {
            val result = firestore.collection("users")
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1)
                .get()
                .await()

            result.documents.firstOrNull()
                ?.toObject(UserAccount::class.java)

        } catch (exception: Exception) {
            null
        }
    }

    // VALIDAR SI UN CORREO EXISTE
    /** Checks whether a Firestore profile already uses the supplied email. */
    suspend fun emailExists(email: String): Boolean {
        return getAccountByEmail(email) != null
    }


    // ACTUALIZAR PERFIL DE USUARIO
    /** Updates editable profile fields for an existing Firestore account. */
    suspend fun updateAccount(
        uid: String,
        fullName: String,
        phone: String,
        role: String,
        address: String? = null,
        city: String? = null
    ): Boolean {

        if (role !in listOf("Comprador", "Vendedor")) {
            return false
        }

        if (fullName.isBlank() || phone.isBlank()) {
            return false
        }

        return try {

            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName.trim(),
                "phone" to phone.trim(),
                "role" to role
            )
            address?.let { updates["address"] = it.trim() }
            city?.let { updates["city"] = it.trim() }
            firestore.collection("users").document(uid).update(updates).await()

            if (currentUser?.uid == uid) {
                currentUser = currentUser?.copy(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    role = role,
                    address = address ?: currentUser?.address.orEmpty(),
                    city = city ?: currentUser?.city.orEmpty()
                )
            }

            true

        } catch (exception: Exception) {
            false
        }
    }

    /** Creates an account in a secondary Auth session, keeping the administrator signed in. */
    /** Creates a managed Auth account with a secondary Firebase app so admin stays signed in. */
    suspend fun createManagedAccount(context: Context, fullName: String, email: String, phone: String, password: String, role: String): Boolean {
        if (role !in listOf("Comprador", "Vendedor", "Administrador") || fullName.isBlank() || phone.isBlank() || password.length < 8) return false
        var createdUser: com.google.firebase.auth.FirebaseUser? = null
        val secondaryApp = FirebaseApp.getApps(context).firstOrNull { it.name == ADMIN_ACCOUNT_APP }
            ?: FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, ADMIN_ACCOUNT_APP)
            ?: return false
        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        return try {
            val accountEmail = email.trim().lowercase()
            val firebaseUser = secondaryAuth.createUserWithEmailAndPassword(accountEmail, password).await().user
                ?: return false
            createdUser = firebaseUser
            val profile = UserAccount(
                uid = firebaseUser.uid,
                fullName = fullName.trim(),
                email = accountEmail,
                phone = phone.trim(),
                role = role
            )
            firestore.collection("users").document(firebaseUser.uid).set(profile).await()
            secondaryAuth.signOut()
            true
        } catch (_: Exception) {
            try { createdUser?.delete()?.await() } catch (_: Exception) { }
            secondaryAuth.signOut()
            false
        }
    }

    /** Deletes an account through a server-verified administrator callable. */
    /** Requests privileged Auth deletion; it needs the project's deployed admin Cloud Function. */
    suspend fun deleteManagedAccount(uid: String): Boolean {
        if (uid.isBlank() || uid == currentUser?.uid) return false
        return try {
            FirebaseFunctions.getInstance().getHttpsCallable("adminDeleteUser").call(mapOf("uid" to uid)).await()
            true
        } catch (_: Exception) { false }
    }

    private const val ADMIN_ACCOUNT_APP = "overshop-admin-account-creator"

}
