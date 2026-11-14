package com.overshop.app.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.functions.FirebaseFunctions

object AuthRepository {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var currentUser: UserAccount? = null

    // REGISTRAR CUENTA
    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): Boolean {

        if (role !in listOf("Comprador", "Vendedor", "Administrador")) {
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

            false
        }
    }

    // INICIAR SESIÓN
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
                ?: return false

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
    fun getCurrentUser(): UserAccount? {
        return currentUser
    }

    /** Restores the profile tied to Firebase Auth's encrypted persistent session. */
    suspend fun restoreAuthenticatedSession(): UserAccount? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val snapshot = firestore.collection("users").document(firebaseUser.uid).get().await()
            snapshot.toObject(UserAccount::class.java)?.copy(uid = firebaseUser.uid)
                ?.also { currentUser = it }
        } catch (_: Exception) { null }
    }

    /** Sends Firebase's password reset email to the supplied account. */
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email.trim().lowercase()).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    // CERRAR SESIÓN
    fun logout() {
        auth.signOut()
        currentUser = null
    }

    // CONSULTAR TODOS LOS USUARIOS
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
    suspend fun getAccountCount(): Int = try {
        firestore.collection("users").get().await().size()
    } catch (_: Exception) { 0 }

    /** Counts account profiles assigned to a role in Firestore. */
    suspend fun getAccountCountByRole(role: String): Int = try {
        firestore.collection("users").whereEqualTo("role", role).get().await().size()
    } catch (_: Exception) { 0 }

    // CONSULTAR USUARIO POR CORREO
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
    suspend fun emailExists(email: String): Boolean {
        return getAccountByEmail(email) != null
    }


    // ACTUALIZAR PERFIL DE USUARIO
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

    /** Creates another account through a server-verified administrator callable. */
    suspend fun createManagedAccount(fullName: String, email: String, phone: String, password: String, role: String): Boolean {
        if (role !in listOf("Comprador", "Vendedor", "Administrador") || fullName.isBlank() || phone.isBlank() || password.length < 8) return false
        return try {
            FirebaseFunctions.getInstance().getHttpsCallable("adminCreateUser").call(
                mapOf("fullName" to fullName.trim(), "email" to email.trim().lowercase(), "phone" to phone.trim(), "password" to password, "role" to role)
            ).await()
            true
        } catch (_: Exception) { false }
    }

    /** Deletes an account through a server-verified administrator callable. */
    suspend fun deleteManagedAccount(uid: String): Boolean {
        if (uid.isBlank() || uid == currentUser?.uid) return false
        return try {
            FirebaseFunctions.getInstance().getHttpsCallable("adminDeleteUser").call(mapOf("uid" to uid)).await()
            true
        } catch (_: Exception) { false }
    }

}
