package com.overshop.app.admin.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.models.Product
import com.overshop.app.repositories.ProductRepository
import com.overshop.app.seller.adapters.SellerProductAdapter

class AdminProductsFragment :
    Fragment(R.layout.fragment_admin_products) {

    private lateinit var adapter: SellerProductAdapter
    private lateinit var recyclerProducts: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView

    private val categories = listOf(
        "Hoodies",
        "Camisetas",
        "Pantalones"
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        recyclerProducts =
            view.findViewById(R.id.recyclerSellerProducts)

        tvCount =
            view.findViewById(R.id.tvSellerProductsCount)

        tvEmpty =
            view.findViewById(R.id.tvSellerProductsEmpty)

        adapter = SellerProductAdapter(
            products = emptyList(),
            onEdit = { product ->
                showProductDialog(product)
            },
            onDelete = { product ->
                confirmDelete(product)
            }
        )

        recyclerProducts.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerProducts.adapter = adapter

        view.findViewById<Button>(
            R.id.btnAddSellerProduct
        ).setOnClickListener {
            showProductDialog()
        }

        refreshProducts()
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized) {
            refreshProducts()
        }
    }

    private fun refreshProducts() {
        val products = ProductRepository.getProducts()

        adapter.updateProducts(products)

        tvCount.text =
            "Productos registrados: ${products.size}"

        tvEmpty.visibility =
            if (products.isEmpty()) View.VISIBLE else View.GONE

        recyclerProducts.visibility =
            if (products.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showProductDialog(
        product: Product? = null
    ) {
        val isEditing = product != null

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }

        val etName = EditText(requireContext()).apply {
            hint = "Nombre del producto"
            setText(product?.name ?: "")
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        val spinnerCategory = Spinner(requireContext())

        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        spinnerCategory.adapter = categoryAdapter

        val selectedIndex = categories.indexOf(
            product?.category
        ).let {
            if (it >= 0) it else 0
        }

        spinnerCategory.setSelection(selectedIndex)

        val etPrice = EditText(requireContext()).apply {
            hint = "Precio en pesos colombianos"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
            setText(product?.price?.toString() ?: "")
        }

        val etDescription = EditText(requireContext()).apply {
            hint = "Descripción del producto"
            setText(product?.description ?: "")
            minLines = 2
            gravity = Gravity.TOP
        }

        form.addView(etName)
        form.addView(spinnerCategory)
        form.addView(etPrice)
        form.addView(etDescription)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(
                if (isEditing) "Editar producto"
                else "Nuevo producto"
            )
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(
                if (isEditing) "GUARDAR CAMBIOS" else "CREAR",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val name = etName.text.toString().trim()
                    val category =
                        spinnerCategory.selectedItem.toString()
                    val priceText =
                        etPrice.text.toString().trim()
                    val description =
                        etDescription.text.toString().trim()

                    if (name.isEmpty()) {
                        etName.error = "Ingresa el nombre"
                        return@setOnClickListener
                    }

                    val price = priceText.toIntOrNull()

                    if (price == null || price <= 0) {
                        etPrice.error = "Ingresa un precio válido"
                        return@setOnClickListener
                    }

                    if (description.isEmpty()) {
                        etDescription.error =
                            "Ingresa una descripción"
                        return@setOnClickListener
                    }

                    val imageRes = product?.imageRes
                        ?: getImageForCategory(category)

                    val newProduct = Product(
                        id = product?.id ?: 0,
                        name = name,
                        category = category,
                        price = price,
                        imageRes = imageRes,
                        description = description
                    )

                    if (isEditing) {
                        ProductRepository.updateProduct(newProduct)
                    } else {
                        ProductRepository.addProduct(newProduct)
                    }

                    refreshProducts()

                    Toast.makeText(
                        requireContext(),
                        if (isEditing) {
                            "Producto actualizado"
                        } else {
                            "Producto creado correctamente"
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    private fun getImageForCategory(category: String): Int {
        return when (category) {
            "Hoodies" -> R.drawable.product_hoodie_black
            "Camisetas" -> R.drawable.product_tshirt_white
            "Pantalones" -> R.drawable.product_cargo_black
            else -> R.drawable.product_hoodie_black
        }
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar producto")
            .setMessage(
                "¿Deseas eliminar ${product.name} del catálogo?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                ProductRepository.deleteProduct(product.id)

                refreshProducts()

                Toast.makeText(
                    requireContext(),
                    "Producto eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }
}
