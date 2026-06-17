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
                val remoteListings = SharedSyncService.fetchSharedListings()
                val updatedList = remoteListings.filter { it.uuid != listing.uuid }
                SharedSyncService.uploadSharedListings(updatedList)
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
                    val remoteListings = SharedSyncService.fetchSharedListings()
                    val updatedList = remoteListings.filter { it.uuid != listing.uuid }
                    SharedSyncService.uploadSharedListings(updatedList)
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
            val remoteListings = SharedSyncService.fetchSharedListings()
            Log.d("RentalRepository", "Fetched ${remoteListings.size} listings_v2 from the cloud.")
            
            // 2. Fetch local listings_v2
            val localListings = rentalDao.getAllListings().firstOrNull() ?: emptyList()
            val localMap = localListings.associateBy { it.uuid }
            
            // Check which remote listings_v2 are not yet present in our local Room database
            var insertedCount = 0
            for (remote in remoteListings) {
                if (!localMap.containsKey(remote.uuid)) {
                    // Save as isUserCreated = false on this device (so callers cannot delete it)
                    // and start as not favorite (favorites are strictly local / single user preference)
                    val toInsert = remote.copy(
                        id = 0, // Auto-generate local Room primary key ID
                        isUserCreated = false,
                        isFavorite = false
                    )
                    rentalDao.insertListing(toInsert)
                    insertedCount++
                }
            }
            if (insertedCount > 0) {
                Log.d("RentalRepository", "Inserted $insertedCount new remote listings_v2 locally.")
            }

            // Sync departures/deletions: Prune local listings_v2 that are NOT user created and no longer exist on the cloud
            val remoteUuids = remoteListings.map { it.uuid }.toSet()
            var prunedCount = 0
            for (local in localListings) {
                if (!local.isUserCreated && !remoteUuids.contains(local.uuid)) {
                    rentalDao.deleteListing(local)
                    prunedCount++
                }
            }
            if (prunedCount > 0) {
                Log.d("RentalRepository", "Pruned $prunedCount listings_v2 locally that were removed from the cloud.")
            }

            // 3. Find our own local created listings_v2 that are not yet uploaded
            val localToUpload = localListings.filter { it.isUserCreated && !remoteUuids.contains(it.uuid) }
            
            if (localToUpload.isNotEmpty()) {
                Log.d("RentalRepository", "Uploading ${localToUpload.size} new local listings_v2 to cloud...")
                val updatedRemoteList = remoteListings.toMutableList()
                for (item in localToUpload) {
                    // Strip the custom local primary key ID to let other devices generate their own
                    updatedRemoteList.add(item.copy(id = 0))
                }
                
                val uploadSuccess = SharedSyncService.uploadSharedListings(updatedRemoteList)
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
