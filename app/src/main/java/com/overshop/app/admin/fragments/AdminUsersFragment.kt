
package com.overshop.app.admin.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.admin.adapters.AdminUserAdapter
import com.overshop.app.auth.AuthRepository
import com.overshop.app.auth.UserAccount
import kotlinx.coroutines.launch

class AdminUsersFragment : Fragment(R.layout.fragment_admin_users) {

    private lateinit var adapter: AdminUserAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var countText: TextView
    private lateinit var searchInput: EditText

    private var allUsers: List<UserAccount> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvAdminUsers)
        countText = view.findViewById(R.id.tvUserCount)
        searchInput = view.findViewById(R.id.etSearchUser)

        adapter = AdminUserAdapter(
            emptyList(),
            onEdit = { user -> showEditDialog(user) },
            onDelete = { user -> showDeleteDialog(user) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.btnAddUser).setOnClickListener {
            showCreateDialog()
        }

        searchInput.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterUsers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        refreshUsers()
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized) {
            refreshUsers()
        }
    }

    // CONSULTAR USUARIOS DESDE ROOM
    private fun refreshUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            allUsers = AuthRepository.getAccounts()
            filterUsers(searchInput.text.toString())
        }
    }

    // FILTRAR USUARIOS
    private fun filterUsers(query: String) {

        val filtered = allUsers.filter {
            it.fullName.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }

        adapter.updateData(filtered)
        countText.text = "Usuarios registrados: ${allUsers.size}"
    }

    // CREAR USUARIO
    private fun showCreateDialog() {

        val context = requireContext()
        val form = createForm(includePassword = true)

        val nameInput = form.getChildAt(0) as EditText
        val emailInput = form.getChildAt(1) as EditText
        val phoneInput = form.getChildAt(2) as EditText
        val passwordInput = form.getChildAt(3) as EditText
        val roleSpinner = form.getChildAt(4) as Spinner

        val dialog = AlertDialog.Builder(context)
            .setTitle("Crear usuario")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val name = nameInput.text.toString().trim()
                    val email = emailInput.text.toString().trim()
                    val phone = phoneInput.text.toString().trim()
                    val password = passwordInput.text.toString()
                    val role = roleSpinner.selectedItem.toString()

                    if (
                        name.isBlank() ||
                        email.isBlank() ||
                        phone.isBlank() ||
                        password.isBlank()
                    ) {
                        Toast.makeText(
                            context,
                            "Completa todos los campos",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS
                            .matcher(email).matches()
                    ) {
                        emailInput.error = "Correo inválido"
                        return@setOnClickListener
                    }

                    if (password.length < 6) {
                        passwordInput.error = "Mínimo 6 caracteres"
                        return@setOnClickListener
                    }

                    viewLifecycleOwner.lifecycleScope.launch {

                        val registered =
                            AuthRepository.createAccountByAdmin(
                                name,
                                email,
                                phone,
                                password,
                                role
                            )

                        if (!registered) {
                            emailInput.error =
                                "Este correo ya está registrado"
                            return@launch
                        }

                        refreshUsers()

                        Toast.makeText(
                            context,
                            "Usuario creado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()
                    }
                }
        }

        dialog.show()
    }

    // EDITAR USUARIO
    private fun showEditDialog(user: UserAccount) {

        val context = requireContext()
        val form = createForm(includePassword = false)

        val nameInput = form.getChildAt(0) as EditText
        val emailInput = form.getChildAt(1) as EditText
        val phoneInput = form.getChildAt(2) as EditText
        val roleSpinner = form.getChildAt(3) as Spinner

        nameInput.setText(user.fullName)
        emailInput.setText(user.email)
        emailInput.isEnabled = false
        phoneInput.setText(user.phone)

        val rolePosition = if (user.role == "Vendedor") 1 else 0
        roleSpinner.setSelection(rolePosition)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Editar usuario")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val name = nameInput.text.toString().trim()
                    val phone = phoneInput.text.toString().trim()
                    val role = roleSpinner.selectedItem.toString()

                    if (name.isBlank() || phone.isBlank()) {
                        Toast.makeText(
                            context,
                            "Completa los campos obligatorios",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    viewLifecycleOwner.lifecycleScope.launch {

                        val updated = AuthRepository.updateAccount(
                            user.email,
                            name,
                            phone,
                            role
                        )

                        if (!updated) {
                            Toast.makeText(
                                context,
                                "No fue posible actualizar el usuario",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@launch
                        }

                        refreshUsers()

                        Toast.makeText(
                            context,
                            "Usuario actualizado",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()
                    }
                }
        }

        dialog.show()
    }

    // ELIMINAR USUARIO
    private fun showDeleteDialog(user: UserAccount) {

        val context = requireContext()
        val currentUser = AuthRepository.getCurrentUser()

        if (currentUser?.email.equals(user.email, ignoreCase = true)) {
            Toast.makeText(
                context,
                "No puedes eliminar tu propia cuenta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(context)
            .setTitle("Eliminar usuario")
            .setMessage(
                "¿Deseas eliminar la cuenta de ${user.fullName}?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                viewLifecycleOwner.lifecycleScope.launch {

                    val deleted = AuthRepository.deleteAccount(user.email)

                    if (deleted) {
                        refreshUsers()

                        Toast.makeText(
                            context,
                            "Usuario eliminado",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "No se pudo eliminar el usuario",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    // CONSTRUIR FORMULARIOS
    private fun createForm(includePassword: Boolean): LinearLayout {

        val context = requireContext()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }

        fun addInput(hintText: String, inputType: Int): EditText {

            return EditText(context).apply {
                hint = hintText
                setSingleLine(true)
                setInputType(inputType)
                layout.addView(this)
            }
        }

        addInput(
            "Nombre completo",
            android.text.InputType.TYPE_CLASS_TEXT
        )

        addInput(
            "Correo electrónico",
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        addInput(
            "Teléfono",
            android.text.InputType.TYPE_CLASS_PHONE
        )

        if (includePassword) {
            addInput(
                "Contraseña",
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            )
        }

        val roleSpinner = Spinner(context)
        val roles = listOf("Comprador", "Vendedor")

        roleSpinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

        layout.addView(roleSpinner)

        return layout
    }
}
