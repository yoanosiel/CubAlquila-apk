package com.example.data.repository

import android.util.Log
import com.example.data.model.RentalListing
import com.parse.ParseObject
import com.parse.ParseQuery

object SharedSyncService {
    private const val TAG = "SharedSyncService"
    private const val CLASS_NAME = "RentalListing"

    fun publishListing(listing: RentalListing, onResult: (Boolean) -> Unit) {
        val parseObject = ParseObject(CLASS_NAME).apply {
            put("category", listing.category)
            put("title", listing.title)
            put("description", listing.description)
            put("price", listing.price)
            put("currency", listing.currency)
            put("province", listing.province)
            put("municipality", listing.municipality)
            put("exactAddress", listing.exactAddress)
            put("contactPhone", listing.contactPhone)
            put("contactWhatsApp", listing.contactWhatsApp)
            put("contactEmail", listing.contactEmail)
            put("imageUrl", listing.imageUrl)
            put("isFavorite", listing.isFavorite)
            put("isUserCreated", listing.isUserCreated)
            put("uuid", listing.uuid)
            put("publishDate", listing.publishDate)
            put("pricePeriod", listing.pricePeriod)
        }

        parseObject.saveInBackground { e ->
            if (e == null) {
                Log.d(TAG, "Anuncio publicado exitosamente en Back4App")
                onResult(true)
            } else {
                Log.e(TAG, "Error al publicar en Back4App: ${e.message}", e)
                onResult(false)
            }
        }
    }

    fun fetchSharedListings(onResult: (List<RentalListing>) -> Unit) {
        val query = ParseQuery.getQuery<ParseObject>(CLASS_NAME)
        query.orderByDescending("createdAt")
        query.findInBackground { objects, e ->
            if (e == null && objects != null) {
                val resultList = mutableListOf<RentalListing>()
                for (obj in objects) {
                    try {
                        val listing = RentalListing(
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
                        publishDate = obj.getString("publishDate") ?: "",
                        pricePeriod = obj.getString("pricePeriod") ?: ""
                        )
                        resultList.add(listing)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error parseando anuncio de Back4App", ex)
                    }
                }
                onResult(resultList)
            } else {
                Log.e(TAG, "Error obteniendo anuncios de Back4App: ${e?.message}", e)
                onResult(emptyList())
            }
        }
    }
}
