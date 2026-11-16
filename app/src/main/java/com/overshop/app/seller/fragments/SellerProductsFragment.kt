package com.overshop.app.seller.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.graphics.Bitmap
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.overshop.app.R
import com.overshop.app.models.Product
import com.overshop.app.repositories.ProductRepository
import com.overshop.app.seller.adapters.SellerProductAdapter
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/** Permite al Vendedor crear, editar y eliminar productos del catálogo. */
class SellerProductsFragment :
    Fragment(R.layout.fragment_seller_products) {

    private var selectedImageUri: Uri? = null
    private var selectedImageBytes: ByteArray? = null
    private lateinit var galleryPicker: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var cameraPicker: androidx.activity.result.ActivityResultLauncher<Void?>

    private lateinit var adapter: SellerProductAdapter
    private lateinit var recyclerProducts: RecyclerView
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView

    private val categories = listOf(
        "Hoodies",
        "Camisetas",
        "Pantalones"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        galleryPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }
        cameraPicker = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap -> selectedImageBytes = bitmap?.toJpegBytes() }
    }

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

    /** Reloads the persisted catalog and publishes the resulting products to the list adapter. */
    private fun refreshProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
        val products = try {
            ProductRepository.ensureDemoProducts()
            ProductRepository.getProducts()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "No se pudo leer el catálogo de Firestore.", Toast.LENGTH_LONG).show()
            emptyList()
        }

        adapter.updateProducts(products)

        tvCount.text =
            "Productos registrados: ${products.size}"

        tvEmpty.visibility =
            if (products.isEmpty()) View.VISIBLE else View.GONE

        recyclerProducts.visibility =
            if (products.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    /** Opens the create/edit form and persists validated product fields to Firestore. */
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
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
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
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(product?.price?.toString() ?: "")
        }

        val etDescription = EditText(requireContext()).apply {
            hint = "Descripción del producto"
            setText(product?.description ?: "")
            minLines = 2
            gravity = android.view.Gravity.TOP
        }

        selectedImageUri = null
        selectedImageBytes = null
        val imageButtons = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(requireContext()).apply { text = "Galería"; setOnClickListener { galleryPicker.launch("image/*") } })
            addView(Button(requireContext()).apply { text = "Cámara"; setOnClickListener { cameraPicker.launch(null) } })
        }

        form.addView(etName)
        form.addView(spinnerCategory)
        form.addView(etPrice)
        form.addView(etDescription)
        form.addView(imageButtons)

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

                    val newProduct = Product(
                        id = product?.id ?: 0,
                        name = name,
                        category = category,
                        price = price,
                        imageRes = product?.imageRes ?: getImageForCategory(category),
                        description = description,
                        documentId = product?.documentId.orEmpty(),
                        imageUrl = product?.imageUrl.orEmpty()
                    )
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            ProductRepository.saveProduct(newProduct, selectedImageUri, selectedImageBytes)
                            refreshProducts()
                            Toast.makeText(requireContext(), if (isEditing) "Producto actualizado" else "Producto creado correctamente", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } catch (_: Exception) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            Toast.makeText(requireContext(), "No se pudo guardar. Revisa conexión y reglas de Storage/Firestore.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        dialog.show()
    }

    /** Selects a bundled fallback image when a product has no remote image URL. */
    private fun getImageForCategory(category: String): Int {

        return when (category) {
            "Hoodies" -> R.drawable.product_hoodie_black
            "Camisetas" -> R.drawable.product_tshirt_white
            "Pantalones" -> R.drawable.product_cargo_black
            else -> R.drawable.product_hoodie_black
        }
    }

    /** Requests confirmation before removing the selected catalog item. */
    private fun confirmDelete(product: Product) {

        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar producto")
            .setMessage(
                "¿Deseas eliminar ${product.name} del catálogo?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        ProductRepository.deleteProduct(product)
                        refreshProducts()
                        Toast.makeText(requireContext(), "Producto eliminado", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "No se pudo eliminar el producto.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    /** Encodes a camera bitmap as JPEG bytes for the Storage upload path. */
    private fun Bitmap.toJpegBytes(): ByteArray = ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, 90, stream)
        stream.toByteArray()
    }
}
