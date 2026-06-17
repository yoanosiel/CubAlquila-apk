package com.example.data.repository
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import android.util.Log
import com.example.data.model.RentalListing
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SharedSyncService {
            private const val TAG = "SharedSyncService"
                
                    // Completely free-to-use, high performance, and unlimited public HTTPS key-value storage bucket
                        private const val BASE_URL = "https://kvdb.io/WX5bfrTwcw2morZFDF4AMK/listings_v2_v2"

                            private val client = OkHttpClient.Builder()
                                    .connectTimeout(15, TimeUnit.SECONDS)
                                            .readTimeout(15, TimeUnit.SECONDS)
                                                    .build()

                                                        private val moshi = Moshi.Builder()
                                                                .add(KotlinJsonAdapterFactory())
                                                                        .build()

                                                                            private val listType = Types.newParameterizedType(List::class.java, RentalListing::class.java)
                                                                                private val jsonAdapter = moshi.adapter<List<RentalListing>>(listType)

                                                                                    /**
                                                                                         * Fetches listings_v2 from the shared cloud database.
                                                                                              */
                                                                                                  fun fetchSharedListings(): List<RentalListing> {
                                                                                                                val request = Request.Builder()
                                                                                                                            .url(BASE_URL)
                                                                                                                                        .get()
                                                                                                                                                    .build()

                                                                                                                                                            try {
                                                                                                                                                                            client.newCall(request).execute().use { response ->
                                                                                                                                                                                            if (response.code == 404) {
                                                                                                                                                                                                                    Log.d(TAG, "First-time fetch (404). Cloud database is currently empty.")
                                                                                                                                                                                                                                        return emptyList()
                                                                                                                                                                                            }
                                                                                                                                                                                                            if (!response.isSuccessful) {
                                                                                                                                                                                                                                    Log.e(TAG, "Error fetching listings_v2: ${response.code} ${response.message}")
                                                                                                                                                                                                                                                        return emptyList()
                                                                                                                                                                                                            }
                                                                                                                                                                                                                            val bodyString = response.body?.string() ?: return emptyList()
                                                                                                                                                                                                                                            if (bodyString.isBlank() || bodyString == "null") {
                                                                                                                                                                                                                                                                    return emptyList()
                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                                            return jsonAdapter.fromJson(bodyString) ?: emptyList()
                                                                                                                                                                            }
                                                                                                                                                            } catch (e: Exception) {
                                                                                                                                                                            Log.e(TAG, "Exception during fetch from cloud sync: ${e.message}", e)
                                                                                                                                                                                    try {
                                                                                                                                                                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                                                                                                                                                    }
                                                                                                                                                                                    } catch(t: Exception) {}
                                                                                                                                                                                                return emptyList()
                                                                                                                                                            }
                                                                                                  }

                                                                                                      /**
                                                                                                           * Uploads the full updated listings_v2 to the shared cloud database.
                                                                                                                */
                                                                                                                    fun uploadSharedListings(listings_v2: List<RentalListing>): Boolean {
                                                                                                                                val json = jsonAdapter.toJson(listings_v2)
                                                                                                                                        val mediaType = "application/json; charset=utf-8".toMediaType()
                                                                                                                                                val body = json.toRequestBody(mediaType)

                                                                                                                                                        val request = Request.Builder()
                                                                                                                                                                    .url(BASE_URL)
                                                                                                                                                                                .post(body)
                                                                                                                                                                                            .build()

                                                                                                                                                                                                    try {
                                                                                                                                                                                                                    client.newCall(request).execute().use { response ->
                                                                                                                                                                                                                                    if (!response.isSuccessful) {
                                                                                                                                                                                                                                                            Log.e(TAG, "Error uploading listings_v2: ${response.code} ${response.message}")
                                                                                                                                                                                                                                                                                return false
                                                                                                                                                                                                                                    }
                                                                                                                                                                                                                                                    return true
                                                                                                                                                                                                                    }
                                                                                                                                                                                                    } catch (e: Exception) {
                                                                                                                                                                                                                    Log.e(TAG, "Exception during upload to cloud sync: ${e.message}", e)
    return false
                                                                                                                                                                                                    }
                                                                                                                    }

                                                                                                                        /**
                                                                                                                             * Uploads an image byte array to Catbox and returns the public web URL.
                                                                                                                                  * If failed, returns null.
                                                                                                                                       */
                                                                                                                                           fun uploadImage(imageBytes: ByteArray): String? {
        // 1. INTENTO CON CATBOX (Con User-Agent real para evadir protecciones antibot de Cloudflare)
        try {
            val imageBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("fileToUpload", "image.jpg", imageBody)
                .build()

            val request = Request.Builder()
                .url("https://catbox.moe/user/api.php")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val resStr = response.body?.string()?.trim()
                    if (!resStr.isNullOrBlank() && resStr.startsWith("http")) {
                        Log.d(TAG, "Successfully uploaded image to Catbox: $resStr")
                        return resStr
                    }
                }
                Log.e(TAG, "Catbox failed or returned unsuccessful code, trying fallback...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Catbox exception: ${e.message}, trying fallback...")
        }

        // 2. PLAN B: INTENTO CON TELEGRA.PH (Plataforma de Telegram, libre de bloqueos en Cuba)
        try {
            val imageBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "image.jpg", imageBody)
                .build()

            val request = Request.Builder()
                .url("https://telegra.ph/upload")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val resStr = response.body?.string()?.trim()
                    if (!resStr.isNullOrBlank() && resStr.contains("file")) {
                        val fileIdx = resStr.indexOf("file")
                        val quoteIdx = resStr.indexOf("\"", fileIdx)
                        if (fileIdx != -1 && quoteIdx != -1) {
                            val rawPath = resStr.substring(fileIdx, quoteIdx)
                            val cleanPath = rawPath.replace("\\/", "/")
                            val fullUrl = "https://telegra.ph/" + cleanPath
                            Log.d(TAG, "Successfully uploaded image to Telegra.ph: $fullUrl")
                            return fullUrl
                        }
                    }
                }
                Log.e(TAG, "Telegra.ph upload failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegra.ph exception: ${e.message}")
        }

        return null
    }
}