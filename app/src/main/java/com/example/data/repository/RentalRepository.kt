package com.example.data.repository

import com.example.data.local.RentalDao
import com.example.data.model.RentalListing
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class RentalRepository(private val rentalDao: RentalDao) {

    val allListings: Flow<List<RentalListing>> = rentalDao.getAllListings()
    val favoriteListings: Flow<List<RentalListing>> = rentalDao.getFavoriteListings()

    fun getListingsByCategory(category: String): Flow<List<RentalListing>> {
        return rentalDao.getListingsByCategory(category)
    }

    fun getListingsByLocation(province: String, municipality: String): Flow<List<RentalListing>> {
        return rentalDao.getListingsByLocation(province, municipality)
    }

    suspend fun getListingById(id: Int): RentalListing? = withContext(Dispatchers.IO) {
        rentalDao.getListingById(id)
    }

    suspend fun insert(listing: RentalListing): Long = withContext(Dispatchers.IO) {
        rentalDao.insertListing(listing)
    }

    suspend fun update(listing: RentalListing) = withContext(Dispatchers.IO) {
        rentalDao.updateListing(listing)
    }

    suspend fun toggleFavorite(id: Int) = withContext(Dispatchers.IO) {
        val listing = rentalDao.getListingById(id)
        if (listing != null) {
            rentalDao.updateListing(listing.copy(isFavorite = !listing.isFavorite))
        }
    }

    suspend fun delete(listing: RentalListing) = withContext(Dispatchers.IO) {
        rentalDao.deleteListing(listing)
        if (listing.isUserCreated) {
            try {
                SharedSyncService.deleteSharedListing(listing.uuid)
            } catch (e: Exception) {
                Log.e("RentalRepository", "Error syncing delete with cloud: ${e.message}")
            }
        }
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        val listing = rentalDao.getListingById(id)
        if (listing != null) {
            rentalDao.deleteListing(listing)
            if (listing.isUserCreated) {
                try {
                    SharedSyncService.deleteSharedListing(listing.uuid)
                } catch (e: Exception) {
                    Log.e("RentalRepository", "Error syncing delete with cloud by id: ${e.message}")
                }
            }
        }
    }

    suspend fun syncWithCloud() = withContext(Dispatchers.IO) {
        try {
            Log.d("RentalRepository", "Starting bidirectional synchronization with free cloud database...")
            // 1. Fetch remote listings_v2
            SharedSyncService.deleteSharedListing(listing.uuid)
                if (uploadSuccess) {
                    Log.d("RentalRepository", "Successfully uploaded ${localToUpload.size} listings_v2!")
                } else {
                    Log.e("RentalRepository", "Failed to upload listings_v2.")
                }
            } else {
                Log.d("RentalRepository", "All user-created listings_v2 are already in sync on the cloud.")
            }
            
            Log.d("RentalRepository", "Synchronization completed successfully.")
        } catch (e: Exception) {
            Log.e("RentalRepository", "Synchronization failed: ${e.message}", e)
        }
    }

    suspend fun prepopulateIfNeeded() = withContext(Dispatchers.IO) {
        // Cached data is fully preserved on startup for perfect offline access and to prevent image disappearances
        Log.d("RentalRepository", "Preserving local cache for instant offline access.")
    }
}
