package com.example.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "rental_listings_v2")
data class RentalListing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String = "", // "Casa", "Garaje", "Carro", "Otros"
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val currency: String = "", // "CUP", "USD", "MLC"
    val province: String = "",
    val municipality: String = "",
    val exactAddress: String = "",
    val contactPhone: String = "",
    val contactWhatsApp: String = "",
    val contactEmail: String = "",
    val imageUrl: String = "", // Enlace de Telegram o marcador de posición
    val isFavorite: Boolean = false,
    val isUserCreated: Boolean = false,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val publishDate: Long = System.currentTimeMillis(),
    val pricePeriod: String = "" // "Día", "Semana", "Mes"
)
