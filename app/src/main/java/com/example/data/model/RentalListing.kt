package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rental_listings")
data class RentalListing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Casa", "Garaje", "Carro", "Otros"
    val title: String,
    val description: String,
    val price: Double,
    val currency: String, // "CUP", "USD", "MLC"
    val province: String,
    val municipality: String,
    val exactAddress: String,
    val contactPhone: String,
    val contactWhatsApp: String,
    val contactEmail: String,
    val imageUrl: String, // Can be a URL, local file, or predefined placeholder keyword
    val isFavorite: Boolean = false,
    val isUserCreated: Boolean = false,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val publishDate: Long = System.currentTimeMillis(),
    val pricePeriod: String = "" // "Día", "Semana", "Mes" or ""
)
