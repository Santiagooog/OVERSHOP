package com.overshop.app.repositories

import android.net.Uri
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.overshop.app.R
import com.overshop.app.models.Product
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Firestore and Firebase Storage access for the shop catalog. */
object ProductRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val seedMutex = Mutex()
    private val collection get() = firestore.collection("products")

    /** Seeds the four demo products once, then always reads persisted catalog data. */
    suspend fun getProducts(): List<Product> {
        val result = collection.get().await()
        return result.documents.mapNotNull { doc ->
            doc.toObject(Product::class.java)?.copy(documentId = doc.id)
        }
    }

    /** Adds demo catalog entries only when an authorized manager opens product management. */
    suspend fun ensureDemoProducts() {
        seedMutex.withLock {
            if (collection.limit(1).get().await().isEmpty) seedDemoProducts()
        }
    }

    private suspend fun seedDemoProducts() {
        val samples = listOf(
            Product(name = "Hoodie Oversize Black", category = "Hoodies", price = 129900, imageRes = R.drawable.product_hoodie_black, description = "Hoodie oversize negro, estilo urbano.", documentId = "demo_hoodie_black"),
            Product(name = "Camiseta Oversize White", category = "Camisetas", price = 69900, imageRes = R.drawable.product_tshirt_white, description = "Camiseta oversize blanca.", documentId = "demo_tshirt_white"),
            Product(name = "Cargo Pants Black", category = "Pantalones", price = 149900, imageRes = R.drawable.product_cargo_black, description = "Pantalón cargo negro de estilo urbano.", documentId = "demo_cargo_black"),
            Product(name = "Hoodie Urban Grey", category = "Hoodies", price = 139900, imageRes = R.drawable.product_hoodie_grey, description = "Hoodie gris de estilo streetwear.", documentId = "demo_hoodie_grey")
        )
        samples.forEach { product -> saveProduct(product) }
    }

    /** Uploads a selected image to Storage and returns its download URL. */
    suspend fun uploadImage(uri: Uri): String {
        val ref = storage.reference.child("products/${System.currentTimeMillis()}_${uri.lastPathSegment ?: "image"}")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    /** Uploads a camera preview bitmap and returns its public download URL. */
    suspend fun uploadImage(bytes: ByteArray): String {
        val ref = storage.reference.child("products/${System.currentTimeMillis()}_camera.jpg")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    /** Creates or updates a Firestore catalog document. */
    suspend fun saveProduct(product: Product, imageUri: Uri? = null, imageBytes: ByteArray? = null): Product {
        val ref = if (product.documentId.isBlank()) collection.document() else collection.document(product.documentId)
        val imageUrl = when {
            imageUri != null -> uploadImage(imageUri)
            imageBytes != null -> uploadImage(imageBytes)
            else -> product.imageUrl
        }
        val saved = product.copy(id = product.id.takeIf { it != 0 } ?: (ref.id.hashCode() and Int.MAX_VALUE), documentId = ref.id, imageUrl = imageUrl)
        ref.set(saved).await()
        return saved
    }

    /** Removes a product and its image from cloud storage when present. */
    suspend fun deleteProduct(product: Product) {
        if (product.documentId.isNotBlank()) collection.document(product.documentId).delete().await()
        if (product.imageUrl.isNotBlank()) {
            try { storage.getReferenceFromUrl(product.imageUrl).delete().await() } catch (_: Exception) { }
        }
    }

    /** Returns the persisted catalog size. */
    suspend fun getProductCount(): Int = collection.get().await().size()
}
