package com.overshop.app.auth

import java.security.MessageDigest
import android.content.Context
import com.overshop.app.database.AppDatabase
import com.overshop.app.database.UserDao
import com.overshop.app.database.toUserAccount
import com.overshop.app.database.toUserEntity

object AuthRepository {

    private lateinit var userDao: UserDao

    fun initialize(context: Context) {
        userDao = AppDatabase
            .getDatabase(context)
            .userDao()
    }
    suspend fun initializeDemoAdminInDatabase() {

        val adminEmail = "admin@overshop.com"

        val existingAdmin = userDao.getUserByEmail(adminEmail)

        if (existingAdmin != null) {
            return
        }

        val admin = UserAccount(
            fullName = "Administrador OVERSHOP",
            email = adminEmail,
            phone = "3000000000",
            passwordHash = hashPassword("Admin123!"),
            role = "Administrador"
        )

        userDao.insertUser(admin.toUserEntity())
    }

    private val accounts = mutableListOf<UserAccount>()

    private var currentUser: UserAccount? = null

    private fun hashPassword(password: String): String {

        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }



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

        val existingUser = userDao.getUserByEmail(normalizedEmail)

        if (existingUser != null) {
            return false
        }

        val account = UserAccount(
            fullName = fullName.trim(),
            email = normalizedEmail,
            phone = phone.trim(),
            passwordHash = hashPassword(password),
            role = role
        )

        return try {
            userDao.insertUser(account.toUserEntity())
            true
        } catch (exception: android.database.sqlite.SQLiteConstraintException) {
            false
        }
    }


    // INICIAR SESIÓN
    suspend fun login(email: String, password: String): Boolean {

        val account = userDao.getUserByEmail(email.trim())
            ?.toUserAccount()

        val authenticated = account != null &&
                account.passwordHash == hashPassword(password)

        currentUser = if (authenticated) account else null

        return authenticated
    }

    // OBTENER USUARIO AUTENTICADO
    fun getCurrentUser(): UserAccount? {
        return currentUser
    }

    // CERRAR SESIÓN
    fun logout() {
        currentUser = null
    }

    // CONSULTAR TODOS LOS USUARIOS
    suspend fun getAccounts(): List<UserAccount> {
        return userDao.getAllUsers().map { it.toUserAccount() }
    }

    // CONSULTAR USUARIO POR CORREO
    suspend fun getAccountByEmail(email: String): UserAccount? {
        return userDao.getUserByEmail(email.trim().lowercase())
            ?.toUserAccount()
    }

    // CREAR CUENTA DESDE EL ADMINISTRADOR

    suspend fun createAccountByAdmin(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ): Boolean {

        if (role !in listOf("Comprador", "Vendedor")) {
            return false
        }

        return register(
            fullName = fullName,
            email = email,
            phone = phone,
            password = password,
            role = role
        )
    }


    // ACTUALIZAR DATOS DE USUARIO

    suspend fun updateAccount(
        email: String,
        fullName: String,
        phone: String,
        role: String
    ): Boolean {

        if (role !in listOf("Comprador", "Vendedor")) {
            return false
        }

        val existingUser = userDao.getUserByEmail(email.trim().lowercase())
            ?: return false

        val updatedUser = existingUser.copy(
            fullName = fullName.trim(),
            phone = phone.trim(),
            role = role
        )

        userDao.updateUser(updatedUser)

        if (currentUser?.email.equals(email.trim(), ignoreCase = true)) {
            currentUser = updatedUser.toUserAccount()
        }

        return true
    }


    // ELIMINAR USUARIO
    suspend fun deleteAccount(email: String): Boolean {
        // Impedir que el usuario autenticado se elimine a sí mismo
        if (currentUser?.email.equals(email.trim(), ignoreCase = true)) {
            return false
        }

        val user = userDao.getUserByEmail(email.trim().lowercase())
            ?: return false

        userDao.deleteUser(user)

        return true
    }

    // VALIDAR SI UN CORREO EXISTE
    suspend fun emailExists(email: String): Boolean {
        return userDao.getUserByEmail(email.trim().lowercase()) != null
    }

    // CONTAR USUARIOS
    fun getAccountCount(): Int {
        return accounts.size
    }

    // CONTAR USUARIOS POR ROL
    fun getAccountCountByRole(role: String): Int {
        return accounts.count {
            it.role == role
        }
    }


}
