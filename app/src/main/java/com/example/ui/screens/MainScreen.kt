package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.model.LocationData
import com.example.data.model.RentalListing
import com.example.ui.viewmodel.LocationProfile
import com.example.ui.viewmodel.NavigationScreen
import com.example.ui.viewmodel.RentalViewModel
import com.example.ui.viewmodel.SortOption
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: RentalViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val favoriteListings by viewModel.favoriteListings.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (currentScreen !is NavigationScreen.Details) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = currentScreen is NavigationScreen.Home,
                        onClick = { viewModel.navigateTo(NavigationScreen.Home) },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explorar") },
                        label = { Text("Explorar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_explore")
                    )
                    NavigationBarItem(
                        selected = currentScreen is NavigationScreen.Favorites,
                        onClick = { viewModel.navigateTo(NavigationScreen.Favorites) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") },
                        label = { Text("Favoritos") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_favorites")
                    )
                    NavigationBarItem(
                        selected = currentScreen is NavigationScreen.Publish,
                        onClick = { viewModel.navigateTo(NavigationScreen.Publish) },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Publicar") },
                        label = { Text("Publicar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_publish")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is NavigationScreen.Home -> ExplorerTab(viewModel)
                    is NavigationScreen.Favorites -> FavoritesTab(viewModel)
                    is NavigationScreen.Publish -> PublishTab(viewModel)
                    is NavigationScreen.Details -> DetailScreen(
                        listingId = screen.listingId,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ExplorerTab(viewModel: RentalViewModel) {
    val filteredListings by viewModel.filteredListings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedProvince by viewModel.selectedProvince.collectAsStateWithLifecycle()
    val selectedMunicipality by viewModel.selectedMunicipality.collectAsStateWithLifecycle()
    val currentLocProfile by viewModel.currentLocationProfile.collectAsStateWithLifecycle()
    val onlyNearMe by viewModel.onlyNearMe.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    
    val isDetecting by viewModel.isDetectingLocation.collectAsStateWithLifecycle()
    val detectionMessage by viewModel.locationDetectionMessage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.autoDetectLocationWithGPS(useRealGPS = true)
        } else {
            Toast.makeText(context, "Permiso denegado. Usando simulación de satélites...", Toast.LENGTH_SHORT).show()
            viewModel.autoDetectLocationWithGPS(useRealGPS = false)
        }
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App Identity Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Custom Styled "CubAlquila" Red & White Logo Typography/Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF1E293B), // Sleek Slate container for high visibility
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Cub",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444), // Vibrant Red
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Alquila",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White, // White
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                Column {
                    Text(
                        text = if (onlyNearMe) {
                            "${currentLocProfile.municipality}, ${currentLocProfile.province}"
                        } else if (selectedProvince == "Todas") {
                            "Toda Cuba"
                        } else {
                            "$selectedProvince" + (if (selectedMunicipality != "Todos") ", $selectedMunicipality" else "")
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Cloud Sync Refresh Button with spinning/circular progress feedback
                IconButton(
                    onClick = { 
                        viewModel.syncWithCloud()
                        android.widget.Toast.makeText(context, "Sincronizando con base de datos en la nube...", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("cloud_sync_button"),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar base de datos",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Location Simulator Button styled
                FilledTonalButton(
                    onClick = {
                        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (hasFine || hasCoarse) {
                            viewModel.autoDetectLocationWithGPS(useRealGPS = true)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("gps_auto_detect"),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Detectar Ubicación",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDetecting) "Pulsando..." else "Auto-GPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // GPS Pulse indicator if active
        if (isDetecting) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = detectionMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Buscar...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("search_field"),
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Location Info Toolbar & Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showFilterDialog = true }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (onlyNearMe) {
                        "Cerca de: ${currentLocProfile.municipality}, ${currentLocProfile.province}"
                    } else if (selectedProvince == "Todas") {
                        "Toda Cuba (Filtrar...)"
                    } else {
                        "$selectedProvince" + (if (selectedMunicipality != "Todos") ", $selectedMunicipality" else "")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Location settings reset or custom locator switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "A mi alrededor",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = onlyNearMe,
                    onCheckedChange = { viewModel.onlyNearMe.value = it },
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("near_me_toggle")
                )
            }
        }

        // Category Selection Tab Bar (Scrollable chips)
        val categories = listOf("Todos", "Casa", "Garaje", "Carro", "Otros")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                
                // Theme specific colors for category highlight
                val (chipBg, chipText) = if (isSelected) {
                    when (category.lowercase(Locale.ROOT)) {
                        "casa" -> Pair(Color(0xFFE8DEF8), Color(0xFF21005D))
                        "carro" -> Pair(Color(0xFFFFE082), Color(0xFF452700))
                        "garaje" -> Pair(Color(0xFFC2E7FF), Color(0xFF001D35))
                        else -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else {
                    Pair(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedCategory.value = category },
                    label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        val icon = getCategoryIcon(category)
                        if (icon != null) {
                            Icon(icon, contentDescription = category, modifier = Modifier.size(16.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipBg,
                        selectedLabelColor = chipText,
                        selectedLeadingIconColor = chipText,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.testTag("chip_$category")
                )
            }
        }

        // Sort option header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${filteredListings.size} alquileres encontrados",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("sort_button")
                ) {
                    Icon(Icons.Default.Sort, contentDescription = "Ordenar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Orden: ${sortBy.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                viewModel.sortBy.value = option
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_${option.name}")
                        )
                    }
                }
            }
        }

        // Listings feed
        if (filteredListings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Sin resultados",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No se encontraron alquileres",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Intente limpiar sus filtros de búsqueda o cambiar de provincia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.searchQuery.value = ""
                            viewModel.selectedCategory.value = "Todos"
                            viewModel.selectedProvince.value = "Todas"
                            viewModel.selectedMunicipality.value = "Todos"
                            viewModel.onlyNearMe.value = false
                        }
                    ) {
                        Text("Restablecer Filtros")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredListings) { listing ->
                    RentalCardItem(
                        listing = listing,
                        onFavoriteClick = { viewModel.toggleFavorite(listing.id) },
                        onItemClick = { viewModel.navigateTo(NavigationScreen.Details(listing.id)) }
                    )
                }
            }
        }
    }

    // Dynamic Province / Municipality Filter dialog
    if (showFilterDialog) {
        var tempProvince by remember { mutableStateOf(selectedProvince) }
        var tempMunicipality by remember { mutableStateOf(selectedMunicipality) }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Text(
                    "Filtrar por ubicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Seleccione una provincia y municipio cubano para reducir su búsqueda.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Province Picker Dropdown
                    var provExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { provExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_prov_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Provincia: $tempProvince")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                            }
                        }
                        DropdownMenu(
                            expanded = provExpanded,
                            onDismissRequest = { provExpanded = false },
                            modifier = Modifier.widthIn(min = 240.dp).heightIn(max = 250.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todas") },
                                onClick = {
                                    tempProvince = "Todas"
                                    tempMunicipality = "Todos"
                                    provExpanded = false
                                }
                            )
                            LocationData.provinces.forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov) },
                                    onClick = {
                                        tempProvince = prov
                                        tempMunicipality = "Todos"
                                        provExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Municipality Picker Dropdown
                    if (tempProvince != "Todas") {
                        val availableMunis = LocationData.getMunicipalitiesForProvince(tempProvince)
                        var muniExpanded by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { muniExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_muni_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Municipio: $tempMunicipality")
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                                }
                            }
                            DropdownMenu(
                                expanded = muniExpanded,
                                onDismissRequest = { muniExpanded = false },
                                modifier = Modifier.widthIn(min = 240.dp).heightIn(max = 250.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos") },
                                    onClick = {
                                        tempMunicipality = "Todos"
                                        muniExpanded = false
                                    }
                                )
                                availableMunis.forEach { muni ->
                                    DropdownMenuItem(
                                        text = { Text(muni) },
                                        onClick = {
                                            tempMunicipality = muni
                                            muniExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.selectedProvince.value = tempProvince
                        viewModel.selectedMunicipality.value = tempMunicipality
                        viewModel.onlyNearMe.value = false // disable "Near me" when they manually pick location
                        showFilterDialog = false
                    },
                    modifier = Modifier.testTag("apply_location_filter")
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FavoritesTab(viewModel: RentalViewModel) {
    val favoriteListings by viewModel.favoriteListings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Mis Favoritos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )

        if (favoriteListings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Sin favoritos",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aún no tienes favoritos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Explora los alquileres disponibles y toca el corazón para guardar los que te interesen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(favoriteListings) { listing ->
                    RentalCardItem(
                        listing = listing,
                        onFavoriteClick = { viewModel.toggleFavorite(listing.id) },
                        onItemClick = { viewModel.navigateTo(NavigationScreen.Details(listing.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun PublishTab(viewModel: RentalViewModel) {
    val context = LocalContext.current

    var category by remember { mutableStateOf("Casa") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var province by remember { mutableStateOf(LocationData.provinces.first()) }
    var municipality by remember { mutableStateOf(LocationData.getMunicipalitiesForProvince(LocationData.provinces.first()).first()) }
    var exactAddress by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactWhatsApp by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var pricePeriod by remember { mutableStateOf("Día") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    // Hoisted dropdown expansion states
    var catExpanded by remember { mutableStateOf(false) }
    var provExpanded by remember { mutableStateOf(false) }
    var muniExpanded by remember { mutableStateOf(false) }
    var currExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Casa", "Garaje", "Carro", "Otros")
    val currencies = listOf("USD", "CUP", "MLC")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Publicar Alquiler",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Category Dropdown
            item {
                Column {
                    Text("Tipo de Alquiler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { catExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("publish_cat_picker"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = getCategoryIcon(category)
                                    if (icon != null) {
                                        Icon(icon, contentDescription = category)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(category)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                            }
                        }
                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false },
                            modifier = Modifier.widthIn(min = 280.dp)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        catExpanded = false
                                    },
                                    modifier = Modifier.testTag("publish_cat_$cat")
                                )
                            }
                        }
                    }
                }
            }

            // Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la publicación") },
                    placeholder = { Text("Ej: Apartamento acogedor con terraza") },
                    modifier = Modifier.fillMaxWidth().testTag("publish_title"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción detallada") },
                    placeholder = { Text("Describa el lugar, servicios, facilidades...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("publish_description"),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 5
                )
            }

            // Price and Currency Flow Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Precio") },
                        placeholder = { Text("50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f).testTag("publish_price"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Moneda", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { currExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("publish_curr_picker"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currency)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                                }
                            }
                            DropdownMenu(
                                expanded = currExpanded,
                                onDismissRequest = { currExpanded = false },
                                modifier = Modifier.widthIn(min = 120.dp)
                            ) {
                                currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr) },
                                        onClick = {
                                            currency = curr
                                            currExpanded = false
                                        },
                                        modifier = Modifier.testTag("publish_curr_$curr")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Period selector (Día, Semana, Mes)
            item {
                Column {
                    Text("Periodo de Pago (Para ofertas de Alquiler)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val periods = listOf("Día" to "Por Día", "Semana" to "Semanal", "Mes" to "Mensual")
                        periods.forEach { (value, label) ->
                            val isSelected = pricePeriod == value
                            Button(
                                onClick = { pricePeriod = value },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).testTag("price_period_$value")
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Province Selector
            item {
                Column {
                    Text("Provincia de Cuba", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { provExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("publish_province_picker"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(province)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                            }
                        }
                        DropdownMenu(
                            expanded = provExpanded,
                            onDismissRequest = { provExpanded = false },
                            modifier = Modifier.widthIn(min = 280.dp).heightIn(max = 250.dp)
                        ) {
                            LocationData.provinces.forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov) },
                                    onClick = {
                                        province = prov
                                        // Auto update default municipality based on province change
                                        municipality = LocationData.getMunicipalitiesForProvince(prov).first()
                                        provExpanded = false
                                    },
                                    modifier = Modifier.testTag("publish_province_$prov")
                                )
                            }
                        }
                    }
                }
            }

            // Municipality Selector
            item {
                Column {
                    Text("Municipio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    val availableMunis = LocationData.getMunicipalitiesForProvince(province)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { muniExpanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("publish_muni_picker"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(municipality)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                            }
                        }
                        DropdownMenu(
                            expanded = muniExpanded,
                            onDismissRequest = { muniExpanded = false },
                            modifier = Modifier.widthIn(min = 280.dp).heightIn(max = 250.dp)
                        ) {
                            availableMunis.forEach { muni ->
                                DropdownMenuItem(
                                    text = { Text(muni) },
                                    onClick = {
                                        municipality = muni
                                        muniExpanded = false
                                    },
                                    modifier = Modifier.testTag("publish_muni_$muni")
                                )
                            }
                        }
                    }
                }
            }

            // Exact Address
            item {
                OutlinedTextField(
                    value = exactAddress,
                    onValueChange = { exactAddress = it },
                    label = { Text("Dirección Exacta") },
                    placeholder = { Text("Ej: Calle Cuba #206 e/ O'Reilly y Empedrado") },
                    modifier = Modifier.fillMaxWidth().testTag("publish_address"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // Contact Info Header
            item {
                Text(
                    text = "Datos de Contacto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Phone
            item {
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Teléfono Móvil o Fijo") },
                    placeholder = { Text("Ej: +53 52834192") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("publish_phone"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // WhatsApp
            item {
                OutlinedTextField(
                    value = contactWhatsApp,
                    onValueChange = { contactWhatsApp = it },
                    label = { Text("WhatsApp (Opcional)") },
                    placeholder = { Text("Ej: 5352834192") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("publish_whatsapp"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // Email
            item {
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Email (Opcional)") },
                    placeholder = { Text("ejemplo@gmail.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("publish_email"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // Custom Image Selector & URL Option
            item {
                var showUrlField by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Foto de la Propiedad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { launcher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        if (imageUrl.isNotBlank()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Vista previa de la foto",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Clear image button
                                IconButton(
                                    onClick = { imageUrl = "" },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // Change image visual overlay badge
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Cambiar Foto",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Empty upload action prompt
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Añadir foto",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Toque aquí para subir una foto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Soporta formatos cotidianos (JPG, PNG, TIFF, etc.)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Flexible accordion to enter a URL input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUrlField = !showUrlField }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (showUrlField) "↑ Usar Selector de Foto" else "O prefiere ingresar un enlace web (URL)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showUrlField) {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Pegar Enlace de Foto (URL)") },
                            placeholder = { Text("https://ejemplo.com/mifoto.jpg") },
                            modifier = Modifier.fillMaxWidth().testTag("publish_image_url"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    Text(
                        text = "Si no selecciona ninguna foto, se utilizará una ilustración elegante predeterminada de acuerdo al tipo de alquiler.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val parsedPrice = priceStr.toDoubleOrNull()
                        if (title.isBlank() || description.isBlank() || exactAddress.isBlank() || contactPhone.isBlank()) {
                            Toast.makeText(context, "Por favor complete todos los datos obligatorios marcados.", Toast.LENGTH_LONG).show()
                        } else if (parsedPrice == null || parsedPrice <= 0) {
                            Toast.makeText(context, "Ingrese un precio correcto mayor que cero.", Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.publishListing(
                                category = category,
                                title = title,
                                description = description,
                                price = parsedPrice,
                                currency = currency,
                                province = province,
                                municipality = municipality,
                                exactAddress = exactAddress,
                                contactPhone = contactPhone,
                                contactWhatsApp = contactWhatsApp,
                                contactEmail = contactEmail,
                                imageUrl = imageUrl,
                                pricePeriod = pricePeriod,
                                onSuccess = {
                                    Toast.makeText(context, "¡Publicado con éxito en $municipality!", Toast.LENGTH_LONG).show()
                                    viewModel.navigateTo(NavigationScreen.Home)
                                    // Reset fields
                                    title = ""
                                    description = ""
                                    priceStr = ""
                                    exactAddress = ""
                                    contactPhone = ""
                                    contactWhatsApp = ""
                                    contactEmail = ""
                                    imageUrl = ""
                                    pricePeriod = "Día"
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("publish_submit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Publicar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publicar Ahora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun DetailScreen(listingId: Int, viewModel: RentalViewModel) {
    val context = LocalContext.current
    var listingState by remember { mutableStateOf<RentalListing?>(null) }

    // Fetch the listing
    LaunchedEffect(listingId) {
        viewModel.getListingLive(listingId).collect {
            listingState = it
        }
    }

    listingState?.let { listing ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant top menu bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(NavigationScreen.Home) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                        .testTag("back_button_detail")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }

                Row {
                    _FavoriteButton(
                        isFavorite = listing.isFavorite,
                        onClick = { viewModel.toggleFavorite(listing.id) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val periodText = if (listing.pricePeriod.isNotBlank()) " por ${listing.pricePeriod.lowercase()}" else ""
                            val shareBody = "Mira este alquiler en Cuba: ${listing.title} por ${listing.price} ${listing.currency}$periodText en ${listing.municipality}, ${listing.province}. Dirección: ${listing.exactAddress}. Contacto: ${listing.contactPhone}"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Alquiler Cuban")
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir Alquiler"))
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }
                    if (listing.isUserCreated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.deleteListing(listing)
                                Toast.makeText(context, "Anuncio eliminado correctamente", Toast.LENGTH_SHORT).show()
                                viewModel.navigateTo(NavigationScreen.Home)
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), shape = CircleShape)
                                .testTag("delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Large styled image header
                item {
                    val painter = rememberImageFallbackPainter(listing.imageUrl, listing.category)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = listing.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Decorative overlay gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                        startY = 200f
                                    )
                                )
                        )
                        // Category and price pill on card
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = getCategoryIcon(listing.category)
                                    if (icon != null) {
                                        Icon(icon, contentDescription = listing.category, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(listing.category, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            val periodSuffix = if (listing.pricePeriod.isNotBlank()) " / ${listing.pricePeriod.lowercase()}" else ""
                            Text(
                                text = "${listing.price} ${listing.currency}$periodSuffix",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // General Listing Info
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = listing.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Address Tag Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Ubicación", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${listing.municipality}, ${listing.province}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = listing.exactAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Detailed Description section
                        Column {
                            Text(
                                "Detalles del Alquiler",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = listing.description,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                        // Contact section
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Contactar al propietario",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Contact Action Buttons
                            // Call Button
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${listing.contactPhone}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No se pudo abrir el teclado telefónico", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("detail_call_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Llamar")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Llamar al ${listing.contactPhone}", fontWeight = FontWeight.Bold)
                            }

                            // WhatsApp Button
                            if (listing.contactWhatsApp.isNotBlank()) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${listing.contactWhatsApp}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("detail_whatsapp_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Escribir por WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Email Button
                            if (listing.contactEmail.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${listing.contactEmail}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No se pudo abrir la app de correo", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = "Email")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enviar Correo Electrónico", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RentalCardItem(
    listing: RentalListing,
    onFavoriteClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val painter = rememberImageFallbackPainter(listing.imageUrl, listing.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("listing_card_${listing.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painter,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Category and location overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = getCategoryIcon(listing.category)
                            if (icon != null) {
                                Icon(icon, contentDescription = listing.category, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = listing.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Favorite overlay button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), shape = CircleShape)
                            .size(36.dp)
                            .testTag("item_favorite_toggle_${listing.id}")
                    ) {
                        Icon(
                            imageVector = if (listing.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (listing.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Price tag overlay on image bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val periodSuffix = if (listing.pricePeriod.isNotBlank()) " / ${listing.pricePeriod.lowercase()}" else ""
                    Text(
                        text = "${listing.price} ${listing.currency}$periodSuffix",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Text Info Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = listing.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = listing.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Dirección",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${listing.municipality}, ${listing.province}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun _FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
            .testTag("detail_favorite_button")
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Guardar",
            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Category mappings helper
fun getCategoryIcon(category: String): ImageVector? {
    return when (category.lowercase(Locale.ROOT)) {
        "casa" -> Icons.Default.Home
        "garaje" -> Icons.Default.Garage
        "carro" -> Icons.Default.DirectionsCar
        "otros" -> Icons.Default.Place
        else -> null
    }
}

// Offline image fallback renderer
@Composable
fun rememberImageFallbackPainter(imageUrl: String, category: String): androidx.compose.ui.graphics.painter.Painter {
    // Elegant fallback visuals if empty or illustrative keyword
    if (imageUrl.isBlank() || imageUrl == "Casa" || imageUrl == "Garaje" || imageUrl == "Carro" || imageUrl == "Otros") {
        val drawableId = when (category) {
            "Casa" -> "ic_launcher_background" // generic color backgrounds we can tint
            else -> "ic_launcher_background"
        }
        // Let's draw an elegant synthetic visual vector using remember
        return rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(when (category) {
                    "Casa" -> "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=600&q=80"
                    "Garaje" -> "https://images.unsplash.com/photo-153187353123b-1f20defe5c8b?auto=format&fit=crop&w=600&q=80"
                    "Carro" -> "https://images.unsplash.com/photo-1589134149957-c8317e07eb42?auto=format&fit=crop&w=600&q=80"
                    else -> "https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=600&q=80"
                })
                .crossfade(true)
                .build()
        )
    }
    return rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build()
    )
}

// Extension to scale components lightly inside scaffold
fun Modifier.scale(scale: Float): Modifier = this
