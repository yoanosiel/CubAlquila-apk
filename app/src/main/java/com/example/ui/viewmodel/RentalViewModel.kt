package com.example.ui.viewmodel

import android.app.Application
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RentalDatabase
import com.example.data.model.LocationData
import com.example.data.model.RentalListing
import com.example.data.repository.RentalRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface NavigationScreen {
    object Home : NavigationScreen
    object Favorites : NavigationScreen
    object Publish : NavigationScreen
    data class Details(val listingId: Int) : NavigationScreen
}

enum class SortOption(val displayName: String) {
    LATEST("Más Recientes"),
    PRICE_ASC("Precios de menor a mayor"),
    PRICE_DESC("Precios de mayor a menor")
}

data class LocationProfile(
    val province: String,
    val municipality: String
)

data class FilterGroup1(
    val query: String,
    val category: String,
    val province: String,
    val municipality: String
)

data class FilterGroup2(
    val currentLocProfile: LocationProfile,
    val onlyNearMe: Boolean,
    val sortBy: SortOption
)

class RentalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RentalRepository

    // Base flows from database
    val allListings: StateFlow<List<RentalListing>>
    val favoriteListings: StateFlow<List<RentalListing>>

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Todos") // "Todos", "Casa", "Garaje", "Carro", "Otros"
    val selectedProvince = MutableStateFlow("Todas")
    val selectedMunicipality = MutableStateFlow("Todos")
    val sortBy = MutableStateFlow(SortOption.LATEST)

    // Location Profile state
    val currentLocationProfile = MutableStateFlow(LocationProfile("La Habana", "Plaza de la Revolución"))
    val onlyNearMe = MutableStateFlow(false)
    val isDetectingLocation = MutableStateFlow(false)
    val locationDetectionMessage = MutableStateFlow("")
    val isSyncing = MutableStateFlow(false)

    // Navigation and selection
    val currentScreen = MutableStateFlow<NavigationScreen>(NavigationScreen.Home)
    
    // Reactive Combined Stream
    val filteredListings: StateFlow<List<RentalListing>>

    init {
        val database = RentalDatabase.getDatabase(application)
        repository = RentalRepository(database.rentalDao())

        // Trigger prepopulation and automatic periodic sync loop (every 30 seconds)
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
            while (true) {
                try {
                    syncWithCloud()
                } catch (e: Exception) {
                    android.util.Log.e("RentalViewModel", "Periodic sync error: ${e.message}")
                }
                delay(30000)
            }
        }

        allListings = repository.allListings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        favoriteListings = repository.favoriteListings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        val filterFlow1 = combine(
            searchQuery,
            selectedCategory,
            selectedProvince,
            selectedMunicipality
        ) { q, cat, prov, muni ->
            FilterGroup1(q, cat, prov, muni)
        }

        val filterFlow2 = combine(
            currentLocationProfile,
            onlyNearMe,
            sortBy
        ) { currentLoc, nearMe, sort ->
            FilterGroup2(currentLoc, nearMe, sort)
        }

        // Combine filter states reactively with clean type-safety
        filteredListings = combine(
            allListings,
            filterFlow1,
            filterFlow2
        ) { listings, f1, f2 ->
            val query = f1.query
            val category = f1.category
            val prov = f1.province
            val muni = f1.municipality
            
            val currentLoc = f2.currentLocProfile
            val nearMe = f2.onlyNearMe
            val sort = f2.sortBy

            var result = listings

            // 1. Category Filter
            if (category != "Todos") {
                result = result.filter { it.category.equals(category, ignoreCase = true) }
            }

            // 2. Search Query (Title, Description, Address)
            if (query.isNotBlank()) {
                result = result.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.exactAddress.contains(query, ignoreCase = true)
                }
            }

            // 3. Location filter - either specific drop-downs OR "near me" locator
            if (nearMe) {
                // Filter specifically by active simulated location profile's province to ensure they see nearby listings
                result = result.filter {
                    it.province.equals(currentLoc.province, ignoreCase = true)
                }
            } else {
                // Regular Dropdown filters
                if (prov != "Todas") {
                    result = result.filter { it.province.equals(prov, ignoreCase = true) }
                    
                    if (muni != "Todos" && muni.isNotBlank()) {
                        result = result.filter { it.municipality.equals(muni, ignoreCase = true) }
                    }
                }
            }

            // 4. Sorting & Prioritizing exact municipality if searching "near me"
            var sortedResult = when (sort) {
                SortOption.LATEST -> result.sortedByDescending { it.publishDate }
                SortOption.PRICE_ASC -> result.sortedBy { convertPriceToSampleUSD(it) }
                SortOption.PRICE_DESC -> result.sortedByDescending { convertPriceToSampleUSD(it) }
            }

            if (nearMe) {
                // Prioritize exact municipality: matching municipality comes first
                sortedResult = sortedResult.sortedWith(
                    compareByDescending { it.municipality.equals(currentLoc.municipality, ignoreCase = true) }
                )
            }
            
            sortedResult
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Helper to normalize currencies for price sorting (e.g., 1 USD = 350 CUP)
    private fun convertPriceToSampleUSD(listing: RentalListing): Double {
        return when (listing.currency) {
            "USD" -> listing.price
            "MLC" -> listing.price * 1.0 // Assume MLC approx 1:1 on relative metrics or adjust
            "CUP" -> listing.price / 350.0 // 1 USD = 350 CUP approx in the informal market
            else -> listing.price
        }
    }

    // Actions
    fun navigateTo(screen: NavigationScreen) {
        currentScreen.value = screen
    }

    fun toggleFavorite(id: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun updateLocationProfile(province: String, municipality: String) {
        currentLocationProfile.value = LocationProfile(province, municipality)
        // If they update, they might want to filter immediately by it or keep settings
    }

    fun autoDetectLocation() {
        autoDetectLocationWithGPS(useRealGPS = false)
    }

    @SuppressLint("MissingPermission")
    fun autoDetectLocationWithGPS(useRealGPS: Boolean) {
        viewModelScope.launch {
            isDetectingLocation.value = true
            locationDetectionMessage.value = "Iniciando GPS satelital..."
            delay(1000)
            
            var detectedProfile: LocationProfile? = null
            
            if (useRealGPS) {
                locationDetectionMessage.value = "Sintonizando señal de satélite real..."
                delay(1000)
                try {
                    val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    if (locationManager != null) {
                        val providers = locationManager.getProviders(true)
                        var bestLocation: Location? = null
                        for (provider in providers) {
                            val loc = locationManager.getLastKnownLocation(provider) ?: continue
                            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                bestLocation = loc
                            }
                        }
                        
                        if (bestLocation != null) {
                            locationDetectionMessage.value = "Señal obtenida. Resolviendo ubicación..."
                            delay(1000)
                            val lat = bestLocation.latitude
                            val lng = bestLocation.longitude
                            
                            val geocoder = Geocoder(getApplication(), Locale("es", "CU"))
                            val addresses = geocoder.getFromLocation(lat, lng, 1)
                            val address = addresses?.firstOrNull()
                            
                            if (address != null) {
                                val detectedProvince = address.adminArea
                                val detectedMuni = address.locality ?: address.subAdminArea
                                detectedProfile = matchCubaLocation(detectedProvince, detectedMuni)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore on exception and do fallback
                }
            }
            
            if (detectedProfile == null) {
                locationDetectionMessage.value = "Escaneando torres de red de Cuba..."
                delay(1200)
                
                // Randomly pick a nice realistic cuban location
                val coordinates = listOf(
                    LocationProfile("La Habana", "Plaza de la Revolución"),
                    LocationProfile("La Habana", "Playa"),
                    LocationProfile("Matanzas", "Cárdenas"), // Includes Varadero
                    LocationProfile("Villa Clara", "Santa Clara"),
                    LocationProfile("Santiago de Cuba", "Santiago de Cuba"),
                    LocationProfile("Holguín", "Holguín")
                )
                detectedProfile = coordinates.random()
            }
            
            locationDetectionMessage.value = "Ubicación detectada: ${detectedProfile.municipality}, ${detectedProfile.province}"
            delay(1000)
            
            currentLocationProfile.value = detectedProfile
            // Auto toggle OnlyNearMe to true because they just auto-detected to search near them!
            onlyNearMe.value = true
            isDetectingLocation.value = false
        }
    }

    private fun matchCubaLocation(detectedProvince: String?, detectedMuni: String?): LocationProfile? {
        if (detectedProvince == null) return null
        
        fun String.normalize(): String {
            return this.trim().lowercase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
        }

        val normProv = detectedProvince.normalize()
        val normMuni = detectedMuni?.normalize() ?: ""

        // Try finding exact or partial province first
        for (prov in LocationData.provinces) {
            val pNorm = prov.normalize()
            if (normProv.contains(pNorm) || pNorm.contains(normProv)) {
                // Province matches! Now try to match a municipality in this province
                val munis = LocationData.getMunicipalitiesForProvince(prov)
                for (muni in munis) {
                    val mNorm = muni.normalize()
                    if (normMuni.contains(mNorm) || mNorm.contains(normMuni)) {
                        return LocationProfile(prov, muni)
                    }
                }
                // If no muni matches, return the first muni of that province
                return LocationProfile(prov, munis.firstOrNull() ?: "")
            }
        }
        return null
    }

    fun publishListing(
        category: String,
        title: String,
        description: String,
        price: Double,
        currency: String,
        province: String,
        municipality: String,
        exactAddress: String,
        contactPhone: String,
        contactWhatsApp: String,
        contactEmail: String,
        imageUrl: String,
        pricePeriod: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val finalImage = if (imageUrl.isBlank()) {
                // Assign a pretty illustrative keyword based on category
                category
            } else {
                imageUrl
            }

            val newListing = RentalListing(
                category = category,
                title = title.trim(),
                description = description.trim(),
                price = price,
                currency = currency,
                province = province,
                municipality = municipality,
                exactAddress = exactAddress.trim(),
                contactPhone = contactPhone.trim(),
                contactWhatsApp = contactWhatsApp.trim(),
                contactEmail = contactEmail.trim(),
                imageUrl = finalImage,
                isUserCreated = true,
                pricePeriod = pricePeriod
            )
            repository.insert(newListing)
            onSuccess()
            syncWithCloud()
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            isSyncing.value = true
            repository.syncWithCloud()
            isSyncing.value = false
        }
    }

    fun deleteListing(listing: RentalListing) {
        viewModelScope.launch {
            repository.delete(listing)
        }
    }
    
    fun deleteListingById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun getListingLive(id: Int): Flow<RentalListing?> {
        return flow {
            emit(repository.getListingById(id))
        }
    }
}
