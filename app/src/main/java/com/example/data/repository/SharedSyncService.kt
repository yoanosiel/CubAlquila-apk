package com.example.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.data.model.RentalListing

class SharedSyncService {
    companion object {
        private val db = FirebaseFirestore.getInstance()
        private val client = OkHttpClient()

        // Sube las imágenes al Supergrupo privado de Telegram de forma gratuita y anónima
        suspend fun uploadImage(imageBytes: ByteArray): String? {
    val client = okhttp3.OkHttpClient()
    val imageBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), imageBytes)
    val requestBody = okhttp3.MultipartBody.Builder()
        .setType(okhttp3.MultipartBody.FORM)
        .addFormDataPart("file", "image.jpg", imageBody)
        .addFormDataPart("upload_preset", "dsjpuc7j")
        .build()

    val request = okhttp3.Request.Builder()
        .url("https://api.cloudinary.com/v1_1/mdmhprpj/image/upload")
        .post(requestBody)
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string()
            if (response.isSuccessful && bodyStr != null) {
                // Usamos substring para cortar el texto de forma segura y evitar errores de compilador
                val url = bodyStr.substringAfter("\"secure_url\":\"").substringBefore("\"")
                if (url != bodyStr) url else null
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

        suspend fun fetchSharedListings(): List<RentalListing> = withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection("anuncios").get().await()
                return@withContext snapshot.toObjects(RentalListing::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList<RentalListing>()
            }
        }

        suspend fun uploadSharedListings(listings: List<RentalListing>): Boolean = withContext(Dispatchers.IO) {
            try {
                for (item in listings) {
                    val uuid = item.uuid
                    db.collection("anuncios").document(uuid).set(item).await()
                }
                return@withContext true
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    
        suspend fun deleteSharedListing(uuid: String): Boolean = withContext(Dispatchers.IO) {
            try {
                db.collection("anuncios").document(uuid).delete().await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
