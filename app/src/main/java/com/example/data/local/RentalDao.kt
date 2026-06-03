package com.example.data.local

import androidx.room.*
import com.example.data.model.RentalListing
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalDao {
    @Query("SELECT * FROM rental_listings ORDER BY publishDate DESC")
    fun getAllListings(): Flow<List<RentalListing>>

    @Query("SELECT * FROM rental_listings WHERE category = :category ORDER BY publishDate DESC")
    fun getListingsByCategory(category: String): Flow<List<RentalListing>>

    @Query("SELECT * FROM rental_listings WHERE province = :province AND (:municipality = '' OR municipality = :municipality) ORDER BY publishDate DESC")
    fun getListingsByLocation(province: String, municipality: String): Flow<List<RentalListing>>

    @Query("SELECT * FROM rental_listings WHERE isFavorite = 1 ORDER BY publishDate DESC")
    fun getFavoriteListings(): Flow<List<RentalListing>>

    @Query("SELECT * FROM rental_listings WHERE id = :id LIMIT 1")
    suspend fun getListingById(id: Int): RentalListing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: RentalListing): Long

    @Update
    suspend fun updateListing(listing: RentalListing)

    @Delete
    suspend fun deleteListing(listing: RentalListing)

    @Query("DELETE FROM rental_listings WHERE id = :id")
    suspend fun deleteListingById(id: Int)
}
