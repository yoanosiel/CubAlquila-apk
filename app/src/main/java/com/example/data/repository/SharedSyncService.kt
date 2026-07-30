package com.example.data.repository

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.data.model.RentalListing
import com.parse.ParseObject
import com.parse.ParseQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SharedSyncService {
    private const val TAG = "SharedSyncService"
    private const val CLASS_NAME = "RentalListing"

    suspend fun fetchSharedListings(): List<RentalListing> = withContext(Dispatchers.IO) {
        try {
            val query = ParseQuery.getQuery<ParseObject>(CLASS_NAME)
            query.orderByDescending("createdAt")
            val objects = query.find()
            val resultList = mutableListOf<RentalListing>()
            for (obj in objects) {
                try {
                    resultList.add(RentalListing(
                        id = 0,
                        category = obj.getString("category") ?: "",
                        title = obj.getString("title") ?: "",
                        description = obj.getString("description") ?: "",
                        price = obj.getDouble("price"),
                        currency = obj.getString("currency") ?: "",
                        province = obj.getString("province") ?: "",
                        municipality = obj.getString("municipality") ?: "",
                        exactAddress = obj.getString("exactAddress") ?: "",
                        contactPhone = obj.getString("contactPhone") ?: "",
                        contactWhatsApp = obj.getString("contactWhatsApp") ?: "",
                        contactEmail = obj.getString("contactEmail") ?: "",
                        imageUrl = obj.getString("imageUrl") ?: "",
                        isFavorite = obj.getBoolean("isFavorite"),
                        isUserCreated = obj.getBoolean("isUserCreated"),
                        uuid = obj.getString("uuid") ?: "",
                        publishDate = obj.getLong("publishDate"),
                        pricePeriod = obj.getString("pricePeriod") ?: ""
                    ))
                } catch (e: Exception) { }
            }
            resultList
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadSharedListings(listings: List<RentalListing>): Boolean = withContext(Dispatchers.IO) {
        try {
            for (item in listings) {
                val parseObject = ParseObject(CLASS_NAME).apply {
                put("category", item.category)
                put("title", item.title)
                put("description", item.description)
                put("price", item.price)
                put("currency", item.currency)
                put("province", item.province)
                put("municipality", item.municipality)
                put("exactAddress", item.exactAddress)
                put("contactPhone", item.contactPhone)
                put("contactWhatsApp", item.contactWhatsApp)
                put("contactEmail", item.contactEmail)
                put("imageUrl", item.imageUrl)
                put("isFavorite", item.isFavorite)
                put("isUserCreated", item.isUserCreated)
                put("uuid", item.uuid)
                put("publishDate", item.publishDate)
                put("pricePeriod", item.pricePeriod)
                }
                parseObject.save()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteSharedListing(uuid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val query = ParseQuery.getQuery<ParseObject>(CLASS_NAME)
            query.whereEqualTo("uuid", uuid) 
            val objects = query.find()
            for (obj in objects) {
                obj.delete()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    

    suspend fun uploadImage(imageBytes: ByteArray): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val body = imageBytes.toRequestBody(mediaType)
            val requestBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("fileToUpload", "image.jpg", body)
                .build()
            val request = okhttp3.Request.Builder().url("https://catbox.moe/user/api.php").post(requestBody).build()
            okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                val url = response.body?.string()?.trim()
                if (url != null && url.startsWith("http")) url else null
            }
        } catch (e: Exception) {
            null
        }
    }
}