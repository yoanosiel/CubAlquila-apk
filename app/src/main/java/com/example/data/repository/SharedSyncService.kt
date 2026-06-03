package com.example.data.repository

import android.util.Log
import com.example.data.model.RentalListing
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SharedSyncService {
    private const val TAG = "SharedSyncService"
    
    // Completely free-to-use, high performance, and unlimited public HTTPS key-value storage bucket
    private const val BASE_URL = "https://kvdb.io/AlquileresCubaAppletDB_2026_qmpwzx/listings"

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
     * Fetches listings from the shared cloud database.
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
                    Log.e(TAG, "Error fetching listings: ${response.code} ${response.message}")
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
            return emptyList()
        }
    }

    /**
     * Uploads the full updated listings to the shared cloud database.
     */
    fun uploadSharedListings(listings: List<RentalListing>): Boolean {
        val json = jsonAdapter.toJson(listings)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(BASE_URL)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error uploading listings: ${response.code} ${response.message}")
                    return false
                }
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during upload to cloud sync: ${e.message}", e)
            return false
        }
    }
}
