
package com.overshop.app.repositories

import com.overshop.app.R
import com.overshop.app.models.Product

object ProductRepository {

    private val products = mutableListOf(
        Product(
            id = 1,
            name = "Hoodie Oversize Black",
            category = "Hoodies",
            price = 129900,
            imageRes = R.drawable.product_hoodie_black,
            description = "Hoodie oversize negro, estilo urbano."
        ),
        Product(
            id = 2,
            name = "Camiseta Oversize White",
            category = "Camisetas",
            price = 69900,
            imageRes = R.drawable.product_tshirt_white,
            description = "Camiseta oversize blanca."
        ),
        Product(
            id = 3,
            name = "Cargo Pants Black",
            category = "Pantalones",
            price = 149900,
            imageRes = R.drawable.product_cargo_black,
            description = "Pantalón cargo negro de estilo urbano."
        ),
        Product(
            id = 4,
            name = "Hoodie Urban Grey",
            category = "Hoodies",
            price = 139900,
            imageRes = R.drawable.product_hoodie_grey,
            description = "Hoodie gris de estilo streetwear."
        )
    )

    private var nextId = 5

    fun getProducts(): List<Product> {
        return products.toList()
    }

    fun getProductById(id: Int): Product? {
        return products.find { it.id == id }
    }

    fun addProduct(product: Product) {
        val newProduct = product.copy(id = nextId++)
        products.add(newProduct)
    }

    fun updateProduct(updatedProduct: Product) {
        val index = products.indexOfFirst {
            it.id == updatedProduct.id
        }

        if (index != -1) {
            products[index] = updatedProduct
        }
    }

    fun deleteProduct(productId: Int) {
        products.removeAll {
            it.id == productId
        }
    }

    fun getProductCount(): Int {
        return products.size
    }
}
