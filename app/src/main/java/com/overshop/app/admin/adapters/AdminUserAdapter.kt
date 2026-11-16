package com.overshop.app.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.auth.UserAccount

/** Vincula perfiles de usuario con las filas y acciones de la lista administrativa. */
class AdminUserAdapter(
    private var users: List<UserAccount>,
    private val onEdit: (UserAccount) -> Unit,
    private val onDelete: (UserAccount) -> Unit
) : RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {

    /** Mantiene las vistas de una fila de usuario en la lista administrativa. */
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvUserName)
        val email: TextView = view.findViewById(R.id.tvUserEmail)
        val phone: TextView = view.findViewById(R.id.tvUserPhone)
        val role: TextView = view.findViewById(R.id.tvUserRole)
        val edit: Button = view.findViewById(R.id.btnEditUser)
        val delete: Button = view.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.name.text = user.fullName
        holder.email.text = user.email
        holder.phone.text = "Teléfono: ${user.phone}"
        holder.role.text = user.role

        holder.edit.setOnClickListener {
            onEdit(user)
        }

        holder.delete.setOnClickListener {
            onDelete(user)
        }
    }

    override fun getItemCount(): Int = users.size

    /** Replaces the displayed users and refreshes the list rows. */
    fun updateData(newUsers: List<UserAccount>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
