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
        suspend fun uploadImage(byteArray: ByteArray): String? = withContext(Dispatchers.IO) {
            val botToken = "8645688069:AAFvDt3ElYenUQOAyrVgXyszjGQKjQ6yljY"
            val chatId = "-1004371836968"
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart(
                        "photo", 
                        "imagen_alquiler.jpg", 
                        byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .build()

                val request1 = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendPhoto")
                    .post(requestBody)
                    .build()

                val response1 = client.newCall(request1).execute()
                val json1 = org.json.JSONObject(response1.body?.string() ?: "")
                if (!json1.optBoolean("ok", false)) return@withContext null

                val photoArray = json1.getJSONObject("result").getJSONArray("photo")
                val fileId = photoArray.getJSONObject(photoArray.length() - 1).getString("file_id")

                val request2 = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/getFile?file_id=$fileId")
                    .get()
                    .build()

                val response2 = client.newCall(request2).execute()
                val json2 = org.json.JSONObject(response2.body?.string() ?: "")
                if (!json2.optBoolean("ok", false)) return@withContext null

                val filePath = json2.getJSONObject("result").getString("file_path")

                return@withContext "https://api.telegram.org/file/bot$botToken/$filePath"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
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
    }
}
