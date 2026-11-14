package com.overshop.app.seller.adapters

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.overshop.app.R
import com.overshop.app.repositories.CartRepository
import com.overshop.app.models.Product
import java.text.NumberFormat
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {


    private var productCategory = ""
    private var productDescription = ""
    private var productImage = 0
    private var productImageUrl = ""
    private var productDocumentId = ""
    private var quantity = 1

    private lateinit var tvQuantity: TextView
    private lateinit var tvPrice: TextView

    private var productPrice = 0
    private var productName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Recuperar información enviada desde el catálogo
        productName = intent.getStringExtra("product_name") ?: "Producto"
        productCategory = intent.getStringExtra("product_category") ?: ""
        productPrice = intent.getIntExtra("product_price", 0)
        productDescription = intent.getStringExtra("product_description") ?: ""
        productImage = intent.getIntExtra("product_image", 0)
        productImageUrl = intent.getStringExtra("product_image_url") ?: ""
        productDocumentId = intent.getStringExtra("product_document_id") ?: ""

        // Referencias a los componentes
        val imgProduct = findViewById<ImageView>(R.id.imgDetailProduct)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvCategory = findViewById<TextView>(R.id.tvDetailCategory)
        tvPrice = findViewById(R.id.tvDetailPrice)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)
        tvQuantity = findViewById(R.id.tvQuantity)

        val btnBack = findViewById<TextView>(R.id.tvBackDetail)
        val btnDecrease = findViewById<Button>(R.id.btnDecrease)
        val btnIncrease = findViewById<Button>(R.id.btnIncrease)
        val btnAddToCart = findViewById<Button>(R.id.btnAddToCart)
        val rgSizes = findViewById<RadioGroup>(R.id.rgSizes)

        // Mostrar información del producto
        tvName.text = productName
        tvCategory.text = productCategory
        tvDescription.text = productDescription

        if (productImage != 0) {
            imgProduct.setImageResource(productImage)
        }
        if (productImageUrl.isNotBlank()) {
            com.bumptech.glide.Glide.with(imgProduct).load(productImageUrl).placeholder(productImage).into(imgProduct)
        }

        updatePrice()

        // Volver al catálogo
        btnBack.setOnClickListener {
            finish()
        }

        // Disminuir cantidad (mínimo 1)
        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updatePrice()
            }
        }

        // Aumentar cantidad (máximo 10)
        btnIncrease.setOnClickListener {
            if (quantity < 10) {
                quantity++
                updatePrice()
            }
        }

        // Agregar al carrito (demostración temporal)

        btnAddToCart.setOnClickListener {
            val selectedSizeId = rgSizes.checkedRadioButtonId

            if (selectedSizeId == -1) {
                Toast.makeText(
                    this,
                    "Selecciona una talla antes de continuar",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val selectedSize = findViewById<RadioButton>(
                selectedSizeId
            ).text.toString()

            val product = Product(
                id = intent.getIntExtra("product_id", 0),
                name = productName,
                category = productCategory,
                price = productPrice,
                imageRes = productImage,
                description = productDescription,
                imageUrl = productImageUrl,
                documentId = productDocumentId
            )

            lifecycleScope.launch {
                try {
                    CartRepository.addCloudItem(product, selectedSize, quantity)
                    Toast.makeText(this@ProductDetailActivity, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this@ProductDetailActivity, "No se pudo guardar el carrito en la nube.", Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    private fun updatePrice() {
        tvQuantity.text = quantity.toString()
        tvPrice.text = formatCurrency(productPrice * quantity)
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO")
        )
        return formatter.format(amount)
    }
}
