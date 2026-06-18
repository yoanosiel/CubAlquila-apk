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

        // 1. ATIENDE A MAINSCREEN: Sube los bytes comprimidos directamente a Catbox de forma gratuita
        suspend fun uploadImage(byteArray: ByteArray): String? = withContext(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("reqtype", "fileupload")
                    .addFormDataPart(
                        "fileToUpload", 
                        "image.jpg", 
                        byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://catbox.moe/user/api.php")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext response.body?.string()?.trim()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }

        // 2. ATIENDE A RENTALREPOSITORY: Descarga todos los anuncios de Cloud Firestore
        suspend fun fetchSharedListings(): List<RentalListing> = withContext(Dispatchers.IO) {
            try {
                val snapshot = db.collection("anuncios").get().await()
                return@withContext snapshot.toObjects(RentalListing::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList<RentalListing>()
            }
        }

        // 3. ATIENDE A RENTALREPOSITORY: Sube o actualiza la lista de anuncios en Cloud Firestore
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
    }
}
