package com.example.data.repository

import android.util.Log
import com.example.data.model.RentalListing
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

object SharedSyncService {
    private const val TAG = "SharedSyncService"
    private val client = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun rest(path: String): Request.Builder = Request.Builder()
        .url(SupabaseConfig.URL + "/rest/v1/" + path)
        .addHeader("apikey", SupabaseConfig.PUBLISHABLE_KEY)
        .addHeader("Authorization", "Bearer " + SupabaseConfig.PUBLISHABLE_KEY)

    suspend fun fetchSharedListings(): List<RentalListing> = withContext(Dispatchers.IO) {
        try {
            val path = SupabaseConfig.LISTINGS_TABLE +
                "?select=*&is_active=eq.true&expires_at=gt." +
                java.net.URLEncoder.encode(Instant.now().toString(), "UTF-8") +
                "&order=created_at.desc"
            client.newCall(rest(path).get().build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Supabase GET failed: " + response.code)
                    return@withContext emptyList()
                }
                val body = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(body)
                buildList {
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        val images = o.optJSONArray("image_urls")
                        add(RentalListing(
                            id = 0,
                            category = o.optString("category"),
                            title = o.optString("title"),
                            description = o.optString("description"),
                            price = o.optDouble("price", 0.0),
                            currency = o.optString("currency"),
                            province = o.optString("province"),
                            municipality = o.optString("municipality"),
                            exactAddress = o.optString("address"),
                            contactPhone = o.optString("phone"),
                            contactWhatsApp = o.optString("whatsapp"),
                            contactEmail = o.optString("email"),
                            imageUrl = images?.optString(0, "") ?: "",
                            isFavorite = false,
                            isUserCreated = false,
                            uuid = o.optString("uuid"),
                            publishDate = parseDate(o.optString("created_at")),
                            pricePeriod = o.optString("price_period")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase fetch error", e)
            emptyList()
        }
    }

    suspend fun uploadSharedListings(listings: List<RentalListing>): Boolean = withContext(Dispatchers.IO) {
        var ok = true
        for (item in listings) {
            try {
                val now = Instant.now()
                val created = if (item.publishDate > 0) Instant.ofEpochMilli(item.publishDate) else now
                val expires = created.plusSeconds(30L * 24 * 60 * 60)
                val payload = JSONObject().apply {
                    put("uuid", if (item.uuid.isBlank()) UUID.randomUUID().toString() else item.uuid)
                    put("category", item.category)
                    put("title", item.title)
                    put("description", item.description)
                    put("price", item.price)
                    put("currency", item.currency)
                    put("province", item.province)
                    put("municipality", item.municipality)
                    put("address", item.exactAddress)
                    put("phone", item.contactPhone)
                    put("whatsapp", item.contactWhatsApp)
                    put("email", item.contactEmail)
                    put("price_period", item.pricePeriod)
                    put("is_active", true)
                    put("created_at", created.toString())
                    put("expires_at", expires.toString())
                    put("image_urls", JSONArray().apply {
                        if (item.imageUrl.isNotBlank()) put(item.imageUrl)
                    })
                }
                val request = rest(SupabaseConfig.LISTINGS_TABLE)
                    .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                    .post(payload.toString().toRequestBody(jsonType))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        ok = false
                        Log.e(TAG, "Supabase INSERT failed: " + response.code)
                    }
                }
            } catch (e: Exception) {
                ok = false
                Log.e(TAG, "Supabase upload error", e)
            }
        }
        ok
    }

    suspend fun deleteSharedListing(uuid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(uuid, "UTF-8")
            client.newCall(rest(SupabaseConfig.LISTINGS_TABLE + "?uuid=eq." + encoded)
                .delete().build()).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase delete error", e)
            false
        }
    }

    suspend fun uploadImage(imageBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val path = UUID.randomUUID().toString() + ".jpg"
            val request = Request.Builder()
                .url(SupabaseConfig.URL + "/storage/v1/object/" + SupabaseConfig.IMAGES_BUCKET + "/" + path)
                .addHeader("apikey", SupabaseConfig.PUBLISHABLE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.PUBLISHABLE_KEY)
                .addHeader("Content-Type", "image/jpeg")
                .post(imageBytes.toRequestBody("image/jpeg".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
            }
            SupabaseConfig.URL + "/storage/v1/object/public/" + SupabaseConfig.IMAGES_BUCKET + "/" + path
        } catch (e: Exception) {
            Log.e(TAG, "Supabase image upload error", e)
            null
        }
    }

    private fun parseDate(value: String): Long =
        try { Instant.parse(value).toEpochMilli() } catch (_: Exception) { 0L }
}
