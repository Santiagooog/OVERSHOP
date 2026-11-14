package com.overshop.app.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
        role: String
    ): Boolean {

        if (role !in listOf("Comprador", "Vendedor")) {
            return false
        }

        if (fullName.isBlank() || phone.isBlank()) {
            return false
        }

        return try {

            firestore.collection("users")
                .document(uid)
                .update(
                    mapOf(
                        "fullName" to fullName.trim(),
                        "phone" to phone.trim(),
                        "role" to role
                    )
                )
                .await()

            if (currentUser?.uid == uid) {
                currentUser = currentUser?.copy(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    role = role
                )
            }

            true

        } catch (exception: Exception) {
            false
        }
    }

}
