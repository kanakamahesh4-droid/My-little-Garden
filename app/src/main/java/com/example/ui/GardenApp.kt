package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.core.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import java.io.File
import android.util.Log
import com.example.R
import com.example.api.dto.DiseaseDiagnosisDTO
import com.example.api.dto.PersonalizedCarePlanDTO
import com.example.api.dto.PlantIdentificationDTO
import com.example.data.model.Diagnosis
import com.example.data.model.Plant
import com.example.data.model.PlantIdentification
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// Navigation Destinations
object Destinations {
    const val MY_GARDEN = "my_garden"
    const val IDENTIFY = "identify"
    const val DIAGNOSE = "diagnose"
    const val CARE_GUIDE = "care_guide"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenApp(viewModel: GardenViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destinations.MY_GARDEN

    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Observe error state and show snackbars
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorState) {
        errorState?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    val navigationColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )

                    NavigationBarItem(
                        selected = currentRoute == Destinations.MY_GARDEN,
                        onClick = {
                            navController.navigate(Destinations.MY_GARDEN) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Destinations.MY_GARDEN) Icons.Default.Spa else Icons.Outlined.Spa,
                                contentDescription = "My Garden"
                            )
                        },
                        label = { Text("My Garden", fontWeight = if (currentRoute == Destinations.MY_GARDEN) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = navigationColors,
                        modifier = Modifier.testTag("nav_my_garden")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Destinations.IDENTIFY,
                        onClick = {
                            navController.navigate(Destinations.IDENTIFY) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Destinations.IDENTIFY) Icons.Default.Search else Icons.Outlined.Search,
                                contentDescription = "Identify"
                            )
                        },
                        label = { Text("Identify", fontWeight = if (currentRoute == Destinations.IDENTIFY) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = navigationColors,
                        modifier = Modifier.testTag("nav_identify")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Destinations.DIAGNOSE,
                        onClick = {
                            navController.navigate(Destinations.DIAGNOSE) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Destinations.DIAGNOSE) Icons.Default.Healing else Icons.Outlined.Healing,
                                contentDescription = "Diagnose"
                            )
                        },
                        label = { Text("Diagnose", fontWeight = if (currentRoute == Destinations.DIAGNOSE) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = navigationColors,
                        modifier = Modifier.testTag("nav_diagnose")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Destinations.CARE_GUIDE,
                        onClick = {
                            navController.navigate(Destinations.CARE_GUIDE) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == Destinations.CARE_GUIDE) Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                                contentDescription = "Growth Guide"
                            )
                        },
                        label = { Text("Care Guide", fontWeight = if (currentRoute == Destinations.CARE_GUIDE) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = navigationColors,
                        modifier = Modifier.testTag("nav_care_guide")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.MY_GARDEN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destinations.MY_GARDEN) {
                MyGardenScreen(viewModel = viewModel, onNavigateToIdentify = {
                    navController.navigate(Destinations.IDENTIFY)
                })
            }
            composable(Destinations.IDENTIFY) {
                IdentifyScreen(viewModel = viewModel, onAddPlantDirect = { plant ->
                    viewModel.addPlantToGarden(plant)
                    navController.navigate(Destinations.MY_GARDEN)
                })
            }
            composable(Destinations.DIAGNOSE) {
                DiagnoseScreen(viewModel = viewModel)
            }
            composable(Destinations.CARE_GUIDE) {
                CareGuideScreen(viewModel = viewModel)
            }
        }
    }
}

// --- SCREEN 1: MY GARDEN SCREEN ---
@Composable
fun MyGardenScreen(viewModel: GardenViewModel, onNavigateToIdentify: () -> Unit) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocationFilter by remember { mutableStateOf("All") }

    val filteredPlants = remember(plants, searchQuery, selectedLocationFilter) {
        val searched = if (searchQuery.isBlank()) {
            plants
        } else {
            plants.filter {
                it.species.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.customName.contains(searchQuery, ignoreCase = true)
            }
        }
        if (selectedLocationFilter == "All") {
            searched
        } else {
            searched.filter { it.location.equals(selectedLocationFilter, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Natural Tones Spacious Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "GOOD MORNING,",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF414941)
                )
                Text(
                    text = "My Little Garden",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF112611)
                )
            }
            // Elegant circle container matching the HTML spec
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD7E8DE))
                    .clickable { showAddDialog = true }
                    .testTag("add_plant_fab")
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFFC2D1C7),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Plant",
                    tint = Color(0xFF386B4F),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (plants.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Yard,
                    contentDescription = "Empty Garden",
                    modifier = Modifier.size(96.dp),
                    tint = Color(0xFF386B4F).copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Garden is Empty",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF112611)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start by adding some potted buddies or identify a plant with AI to automatically build your custom care plan!",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF5C6258)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToIdentify,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Identify & Add with AI")
                }
            }
        } else {
            // Organic-Styled Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("garden_search_bar"),
                placeholder = { Text("Search by species or name...", color = Color(0xFF5C6258)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF386B4F)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF5C6258)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF386B4F),
                    unfocusedBorderColor = Color(0xFFE0E3DA),
                    focusedContainerColor = Color(0xFFF2F5EB),
                    unfocusedContainerColor = Color(0xFFF2F5EB)
                )
            )

            // Dynamic Location Grouping / Filter Chips
            val allCount = plants.size
            val indoorCount = plants.count { it.location.equals("Indoor", ignoreCase = true) }
            val balconyCount = plants.count { it.location.equals("Balcony", ignoreCase = true) }
            val gardenCount = plants.count { it.location.equals("Garden", ignoreCase = true) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationFilterChip(
                    label = "All",
                    count = allCount,
                    selected = selectedLocationFilter == "All",
                    onClick = { selectedLocationFilter = "All" }
                )
                LocationFilterChip(
                    label = "Indoor",
                    count = indoorCount,
                    selected = selectedLocationFilter == "Indoor",
                    onClick = { selectedLocationFilter = "Indoor" }
                )
                LocationFilterChip(
                    label = "Balcony",
                    count = balconyCount,
                    selected = selectedLocationFilter == "Balcony",
                    onClick = { selectedLocationFilter = "Balcony" }
                )
                LocationFilterChip(
                    label = "Garden",
                    count = gardenCount,
                    selected = selectedLocationFilter == "Garden",
                    onClick = { selectedLocationFilter = "Garden" }
                )
            }

            if (filteredPlants.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No results",
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFF386B4F).copy(alpha = 0.25f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matching plants",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF112611)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We couldn't find any saved plants matching \"$searchQuery\". Try checking the species name or clear the search.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF5C6258)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { searchQuery = "" }
                    ) {
                        Text("Clear Search", color = Color(0xFF386B4F), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Fast summary card (Identify/Status CTA layout from HTML)
                        GardenSummaryCard(plants = plants)
                    }

                    item {
                        // Today's Care Guide custom layout block from HTML design spec
                        TodayCareGuideCard()
                    }

                    item {
                        Text(
                            text = "My Plant Buddies",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF112611),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredPlants, key = { it.id }) { plant ->
                        PlantCardItem(
                            plant = plant,
                            onWaterClick = { viewModel.waterPlant(plant.id) },
                            onDeleteClick = { viewModel.deletePlantFromGarden(plant) },
                            onLocationChange = { newLoc -> viewModel.updatePlant(plant.copy(location = newLoc)) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlantManualDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, nickname, spec, interval, sun, diff, instructions, location ->
                viewModel.addPlantToGarden(
                    Plant(
                        name = name,
                        customName = nickname.ifBlank { name },
                        species = spec,
                        wateringIntervalDays = interval,
                        sunlight = sun,
                        difficulty = diff,
                        careInstructions = instructions,
                        location = location
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LocationFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFF386B4F) else Color(0xFFF2F5EB),
        border = BorderStroke(1.dp, if (selected) Color(0xFF386B4F) else Color(0xFFE0E3DA)),
        modifier = Modifier
            .height(38.dp)
            .testTag("filter_chip_${label.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val icon = when (label) {
                "Indoor" -> Icons.Default.Home
                "Balcony" -> Icons.Default.WbSunny
                "Garden" -> Icons.Default.Spa
                else -> Icons.Default.Home
            }
            if (label != "All") {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else Color(0xFF386B4F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = "$label ($count)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else Color(0xFF112611)
            )
        }
    }
}

@Composable
fun TodayCareGuideCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5EB)),
        border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TODAY'S CARE GUIDE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFF5C6258)
            )
            Spacer(modifier = Modifier.height(14.dp))
            
            // Care Guide Row 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFD7E8DE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFF386B4F),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Watering: Monsteras",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C19)
                    )
                    Text(
                        text = "Wait until top 2 inches of soil are dry",
                        fontSize = 11.sp,
                        color = Color(0xFF5C6258)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE0E3DA).copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Care Guide Row 2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFAE4D4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Healing,
                        contentDescription = null,
                        tint = Color(0xFF855428),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Treatment: Scale Insects",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C19)
                    )
                    Text(
                        text = "Neem oil remedy (Step 2 of 4)",
                        fontSize = 11.sp,
                        color = Color(0xFF5C6258)
                    )
                }
            }
        }
    }
}

@Composable
fun GardenSummaryCard(plants: List<Plant>) {
    val totalCount = plants.size
    val needsWater = plants.count {
        val daysSinceWatered = (System.currentTimeMillis() - it.lastWatered) / (1000 * 60 * 60 * 24)
        daysSinceWatered >= it.wateringIntervalDays
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCE5DD)),
        border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFF386B4F), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (needsWater > 0) Icons.Default.WaterDrop else Icons.Default.Spa,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Identify & Care Status",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF002111)
                )
                Text(
                    text = if (needsWater > 0) "$needsWater plant(s) need hydration!" else "All your plant buddies are hydrated!",
                    fontSize = 13.sp,
                    color = Color(0xFF414941)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF414941),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PlantCardItem(
    plant: Plant,
    onWaterClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLocationChange: (String) -> Unit,
    viewModel: GardenViewModel
) {
    val daysSinceWatered = (System.currentTimeMillis() - plant.lastWatered) / (1000 * 60 * 60 * 24)
    val isThirsty = daysSinceWatered >= plant.wateringIntervalDays
    var showInstructionsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var journalPhotoUri by remember { mutableStateOf<String?>(null) }
    val journalPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                journalPhotoUri = uri.toString()
            }
        }
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showInstructionsDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Themed leaf avatar container from HTML grid spec
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFF2F5EB), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = Color(0xFF8D9286),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plant.customName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C19),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = plant.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color(0xFF5C6258),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Dynamic color dot state and location badge matching HTML specs
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isThirsty) Color(0xFFE8A33F) else Color(0xFF386B4F), CircleShape)
                            )
                            Text(
                                text = if (isThirsty) "Needs Water" else "Healthy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5C6258)
                            )
                        }

                        // Dot Separator
                        Text("•", fontSize = 11.sp, color = Color(0xFFC2D1C7))

                        // Location badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val locIcon = when (plant.location) {
                                "Indoor" -> Icons.Default.Home
                                "Balcony" -> Icons.Default.WbSunny
                                else -> Icons.Default.Spa
                            }
                            Icon(
                                imageVector = locIcon,
                                contentDescription = null,
                                tint = Color(0xFF386B4F),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = plant.location,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF386B4F)
                            )
                        }
                    }
                }

                // Delete quick button
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Plant",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFE0E3DA).copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Information Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LAST WATERED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF5C6258).copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (daysSinceWatered == 0L) "Today" else "$daysSinceWatered day(s) ago",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isThirsty) Color(0xFFE8A33F) else Color(0xFF191C19)
                    )
                }

                Column {
                    Text(
                        text = "WATER CYCLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF5C6258).copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Every ${plant.wateringIntervalDays} days",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C19)
                    )
                }

                Button(
                    onClick = onWaterClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isThirsty) Color(0xFFE8A33F) else Color(0xFF386B4F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Water", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showInstructionsDialog) {
        var activeTab by remember { mutableStateOf("Profile") }

        Dialog(onDismissRequest = { showInstructionsDialog = false }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = plant.customName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = plant.species,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF2F5EB), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        listOf("Profile", "Journal").forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeTab = tab }
                                    .background(
                                        if (isSelected) Color(0xFF386B4F) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_button_${tab.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else Color(0xFF5C6258)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activeTab == "Profile") {
                        Text(
                            text = "Care Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ProfileDetailRow(label = "Common Name", value = plant.name)
                        ProfileDetailRow(label = "Sunlight Need", value = plant.sunlight)
                        ProfileDetailRow(label = "Care Difficulty", value = plant.difficulty)
                        ProfileDetailRow(label = "Watering Loop", value = "Every ${plant.wateringIntervalDays} days")
                        ProfileDetailRow(
                            label = "Last Watered",
                            value = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(plant.lastWatered))
                        )
                        ProfileDetailRow(label = "Location Group", value = plant.location)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Move Plant To:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF386B4F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Indoor", "Balcony", "Garden").forEach { loc ->
                                val isSelected = plant.location == loc
                                Surface(
                                    onClick = {
                                        if (!isSelected) {
                                            onLocationChange(loc)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF386B4F) else Color(0xFFF2F5EB),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF386B4F) else Color(0xFFE0E3DA)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("profile_location_$loc")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        val icon = when (loc) {
                                            "Indoor" -> Icons.Default.Home
                                            "Balcony" -> Icons.Default.WbSunny
                                            else -> Icons.Default.Spa
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF386B4F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = loc,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF112611)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (plant.careInstructions.isNotBlank()) {
                            Text(
                                text = "In-Depth Guide & Tips",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = plant.careInstructions,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showInstructionsDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Close Profile")
                        }
                    } else {
                        // Journal Tab
                        val journalEntries by viewModel.getJournalEntriesForPlant(plant.id).collectAsStateWithLifecycle(initialValue = emptyList())
                        var growthStatusInput by remember { mutableStateOf("Vegetative") }
                        var noteInput by remember { mutableStateOf("") }

                        Column {
                            Text(
                                text = "Plant Growth Journal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Track growth status, log photo updates, and add notes to monitor progress.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Form to Add Journal Entry
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5EB)),
                                border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Log Growth Update",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF112611)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Status Selector
                                    Text(
                                        text = "Growth Stage",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF5C6258)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Seedling", "Vegetative", "Flowering", "Fruiting", "Mature").forEach { status ->
                                            val isSelected = growthStatusInput == status
                                            Surface(
                                                onClick = { growthStatusInput = status },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) Color(0xFF386B4F) else Color(0xFFE0E3DA).copy(alpha = 0.5f),
                                                modifier = Modifier.testTag("journal_status_$status")
                                            ) {
                                                Text(
                                                    text = status,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) Color.White else Color(0xFF112611),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Notes input
                                    OutlinedTextField(
                                        value = noteInput,
                                        onValueChange = { noteInput = it },
                                        label = { Text("Add journal notes...", fontSize = 12.sp) },
                                        placeholder = { Text("E.g., new leaves sprouting, blooming, watered with nutrients", fontSize = 11.sp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("journal_note_input"),
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Photo picker
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Button(
                                            onClick = {
                                                journalPhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCE5DD)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("journal_photo_picker")
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF386B4F), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF386B4F))
                                        }

                                        if (journalPhotoUri != null) {
                                            val pickedBitmap = remember(journalPhotoUri) {
                                                journalPhotoUri?.let { uriStr ->
                                                    try {
                                                        uriToBitmap(context, Uri.parse(uriStr))
                                                    } catch (e: Exception) {
                                                        null
                                                    }
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (pickedBitmap != null) {
                                                    Image(
                                                        bitmap = pickedBitmap.asImageBitmap(),
                                                        contentDescription = "Picked Image",
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { journalPhotoUri = null },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, "Remove photo", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Save entry button
                                    Button(
                                        onClick = {
                                            if (noteInput.isNotBlank()) {
                                                viewModel.addJournalEntry(
                                                    plantId = plant.id,
                                                    growthStatus = growthStatusInput,
                                                    note = noteInput,
                                                    photoUri = journalPhotoUri
                                                )
                                                noteInput = ""
                                                journalPhotoUri = null
                                            }
                                        },
                                        enabled = noteInput.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .testTag("save_journal_entry_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Save Log Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Logs List
                            Text(
                                text = "History Logs (${journalEntries.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF112611)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (journalEntries.isEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFA)),
                                    border = BorderStroke(1.dp, Color(0xFFE0E3DA).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No logs yet. Add your first growth update above!",
                                            fontSize = 11.sp,
                                            color = Color(0xFF5C6258),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    journalEntries.forEach { entry ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Status badge
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF386B4F).copy(alpha = 0.1f)
                                                    ) {
                                                        Text(
                                                            text = entry.growthStatus,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF386B4F),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF5C6258)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = { viewModel.deleteJournalEntry(entry) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.DeleteOutline, "Delete Entry", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = entry.note,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF191C19),
                                                    lineHeight = 16.sp
                                                )

                                                if (entry.photoUri != null) {
                                                    val entryBitmap = remember(entry.photoUri) {
                                                        try {
                                                            uriToBitmap(context, Uri.parse(entry.photoUri))
                                                        } catch (e: Exception) {
                                                            null
                                                        }
                                                    }
                                                    if (entryBitmap != null) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Image(
                                                            bitmap = entryBitmap.asImageBitmap(),
                                                            contentDescription = "Journal Update Photo",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(120.dp)
                                                                .clip(RoundedCornerShape(10.dp)),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { showInstructionsDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Close Profile")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}


// --- SCREEN 2: PLANT IDENTIFICATION SCREEN ---
@Composable
fun IdentifyScreen(viewModel: GardenViewModel, onAddPlantDirect: (Plant) -> Unit) {
    val identificationResult by viewModel.identificationResult.collectAsStateWithLifecycle()
    val isIdentifying by viewModel.isIdentifying.collectAsStateWithLifecycle()
    val identificationsHistory by viewModel.identificationsHistory.collectAsStateWithLifecycle()

    var customNameInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                selectedBitmap = bitmap
                selectedUri = null
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(context, "Camera permission is required to scan/take plant photos.", Toast.LENGTH_LONG).show()
            }
        }
    )

    fun checkAndLaunchCamera() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Set up standard Android Photo Picker (zero permissions required!)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
                selectedBitmap = uriToBitmap(context, uri)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Natural Tones Spacious Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "PLANT IDENTIFIER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF414941)
                )
                Text(
                    text = "PlantAID Search",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF112611)
                )
            }
            // Elegant circle container matching the HTML spec
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD7E8DE))
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFFC2D1C7),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = Color(0xFF386B4F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Upload Image Section
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5EB)),
                border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showImageSourceDialog = true
                    }
                    .testTag("select_plant_image")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Plant",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap to Change Image",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Upload",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to Select Plant Photo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Supports Gallery, Photo Picker (Zero Permissions Required)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (showImageSourceDialog) {
                Dialog(onDismissRequest = { showImageSourceDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Add Plant Photo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF112611)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Scan your plant using the camera, or select a photo from your gallery.",
                                fontSize = 13.sp,
                                color = Color(0xFF5C6258),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Camera button
                            Button(
                                onClick = {
                                    showImageSourceDialog = false
                                    checkAndLaunchCamera()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Take Photo (Camera)", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Gallery button
                            OutlinedButton(
                                onClick = {
                                    showImageSourceDialog = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                border = BorderStroke(1.dp, Color(0xFF386B4F)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF386B4F)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose from Gallery", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = { showImageSourceDialog = false }
                            ) {
                                Text("Cancel", color = Color(0xFF5C6258))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extra Description
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Describe plant (Optional context, e.g. red flowers, split leaves)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plant_desc_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.identifyPlant(selectedBitmap, descriptionInput)
                },
                enabled = !isIdentifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("identify_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isIdentifying) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Identifying with PlantAID...")
                } else {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Identify Plant")
                }
            }

            // Results Box
            AnimatedVisibility(
                visible = identificationResult != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                identificationResult?.let { result ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = result.detectedName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = result.scientificName,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Confidence & Pet Safety indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Match: ${(result.confidence * 100).toInt()}%") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (result.petSafe) "Pet Friendly" else "Toxic to Pets") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = if (result.petSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (result.petSafe) Icons.Default.Pets else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (result.petSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = result.description,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Species Highlights Section
                            Text(
                                text = "Species Highlights",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GuideCard(icon = Icons.Default.Public, title = "Origin", desc = result.origin)
                            Spacer(modifier = Modifier.height(6.dp))
                            GuideCard(icon = Icons.Default.Eco, title = "Growth Habits", desc = result.growthHabits)
                            Spacer(modifier = Modifier.height(6.dp))
                            GuideCard(icon = Icons.Default.Favorite, title = "Key Care Needs", desc = result.careNeeds)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom AI Growth guide
                            Text(
                                text = "Growth Guide",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GuideCard(icon = Icons.Outlined.LightMode, title = "Light Require", desc = result.lightGuide)
                            Spacer(modifier = Modifier.height(6.dp))
                            GuideCard(icon = Icons.Outlined.WaterDrop, title = "Watering Require", desc = result.waterGuide)
                            Spacer(modifier = Modifier.height(6.dp))
                            GuideCard(icon = Icons.Outlined.Terrain, title = "Soil & Potting", desc = result.soilGuide)

                            Spacer(modifier = Modifier.height(20.dp))

                            // Add to Garden Form
                            Text(
                                text = "Add to My Garden?",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customNameInput,
                                onValueChange = { customNameInput = it },
                                label = { Text("Give it a nickname (e.g., 'Sprouty')") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("nickname_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onAddPlantDirect(
                                        Plant(
                                            name = result.detectedName,
                                            customName = customNameInput.ifBlank { result.detectedName },
                                            species = result.scientificName,
                                            wateringIntervalDays = 7, // Default
                                            sunlight = result.lightGuide.take(30) + "...",
                                            careInstructions = "Light: ${result.lightGuide}\n\nWater: ${result.waterGuide}\n\nSoil: ${result.soilGuide}"
                                        )
                                    )
                                    // Reset inputs
                                    customNameInput = ""
                                    viewModel.clearIdentificationResult()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_to_garden_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add to My Garden")
                            }
                        }
                    }
                }
            }

            // History Log
            if (identificationsHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Identification Log",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                identificationsHistory.forEach { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.setIdentificationResult(
                                    PlantIdentificationDTO(
                                        detectedName = log.detectedName,
                                        scientificName = log.scientificName,
                                        confidence = log.confidence,
                                        description = log.description,
                                        origin = log.origin,
                                        growthHabits = log.growthHabits,
                                        careNeeds = log.careNeeds,
                                        lightGuide = log.lightGuide,
                                        waterGuide = log.waterGuide,
                                        soilGuide = log.soilGuide,
                                        petSafe = log.petSafe
                                    )
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Spa, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.detectedName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(log.scientificName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text(
                                text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(log.timestamp)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), lineHeight = 18.sp)
        }
    }
}


// --- SCREEN 3: PLANT DISEASE DIAGNOSIS SCREEN ---
@Composable
fun DiagnoseScreen(viewModel: GardenViewModel) {
    val diagnosisResult by viewModel.diagnosisResult.collectAsStateWithLifecycle()
    val isDiagnosing by viewModel.isDiagnosing.collectAsStateWithLifecycle()
    val diagnosesHistory by viewModel.diagnosesHistory.collectAsStateWithLifecycle()

    var symptomsInput by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var activeDiagnoseTab by remember { mutableStateOf("Live Scanner") }
    var isCapturing by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Camera permission is required to scan/diagnose plant.", Toast.LENGTH_LONG).show()
            }
        }
    )

    val imageCapture = remember { ImageCapture.Builder().build() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
                selectedBitmap = uriToBitmap(context, uri)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Natural Tones Spacious Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "DISEASE ASSISTANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF855428)
                )
                Text(
                    text = "Disease Diagnosis",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF112611)
                )
            }
            // Elegant terracotta warning highlight profile bubble
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFAE4D4))
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFFF3CBB5),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Healing,
                    contentDescription = "Healing icon",
                    tint = Color(0xFF855428),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color(0xFFF2F5EB), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            listOf("Live Scanner", "Import Photo").forEach { tab ->
                val isSelected = activeDiagnoseTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeDiagnoseTab = tab }
                        .background(
                            if (isSelected) Color(0xFF386B4F) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 10.dp)
                        .testTag("diagnose_tab_${tab.lowercase().replace(" ", "_")}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (tab == "Live Scanner") Icons.Default.CameraAlt else Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF5C6258),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else Color(0xFF5C6258)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (activeDiagnoseTab == "Live Scanner") {
                if (hasCameraPermission) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = BorderStroke(1.dp, Color(0xFFF3CBB5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .testTag("live_camera_preview_card")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CameraPreview(
                                imageCapture = imageCapture,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Live scanning animation overlay
                            val infiniteTransition = rememberInfiniteTransition(label = "scanner")
                            val scanProgress by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scan_line"
                            )

                            // Scanning box overlay
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .align(Alignment.Center)
                                    .border(2.dp, Color(0xFFFAE4D4).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = "Align plant leaf/spots",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Scanner line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = (320 * scanProgress).dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xFF855428),
                                                Color(0xFFFAE4D4),
                                                Color(0xFF855428),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Small badge showing "LIVE VIEW"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Red.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .padding(12.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE SCANNER",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAE4D4).copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, Color(0xFFF3CBB5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .testTag("camera_permission_required_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF855428),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Camera Permission Required",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF112611)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Let PlantAID scan your live plant directly through your camera to diagnose diseases and recommend treatments.",
                                fontSize = 12.sp,
                                color = Color(0xFF5C6258),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF855428)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Enable Live Camera", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // Upload Spot photo
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAE4D4).copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, Color(0xFFF3CBB5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("select_sick_image")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Sick plant",
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap to Change Image",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Coronavirus,
                                contentDescription = "Sick Icon",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to Add Spot / leaf Photo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Helps PlantAID analyze brown spots, yellow leaves, or mold",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = symptomsInput,
                onValueChange = { symptomsInput = it },
                label = { Text("Describe plant symptoms (optional)") },
                placeholder = { Text("E.g., brown spots on lower leaves, fine white webbing") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("symptoms_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (activeDiagnoseTab == "Live Scanner") {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            isCapturing = true
                            captureImage(
                                context = context,
                                imageCapture = imageCapture,
                                onSuccess = { bitmap ->
                                    isCapturing = false
                                    selectedBitmap = bitmap
                                    viewModel.diagnoseDisease(bitmap, symptomsInput)
                                },
                                onError = { error ->
                                    isCapturing = false
                                    Toast.makeText(context, "Failed to capture image: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    enabled = !isDiagnosing && !isCapturing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("capture_diagnose_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isDiagnosing || isCapturing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isCapturing) "Capturing image..." else "Analyzing Live Plant...")
                    } else {
                        Icon(imageVector = Icons.Default.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture & Diagnose Live Plant")
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.diagnoseDisease(selectedBitmap, symptomsInput)
                    },
                    enabled = !isDiagnosing && selectedBitmap != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("diagnose_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isDiagnosing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing Disease symptoms...")
                    } else {
                        Icon(imageVector = Icons.Default.Healing, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Diagnose Plant Photo")
                    }
                }
            }

            // Results Section
            AnimatedVisibility(
                visible = diagnosisResult != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                diagnosisResult?.let { result ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = result.diseaseName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Plant: ${result.plantName} • Confidence: ${(result.confidence * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Identified Symptoms",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = result.symptoms,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Steps checklist
                            Text(
                                text = "Detailed Treatment Steps",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            result.treatmentSteps.forEachIndexed { i, step ->
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(step, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Home Remedies
                            Text(
                                text = "Home Remedies & Solutions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            result.homeRemedies.forEach { remedy ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Spa, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(remedy, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Future Prevention",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = result.prevention,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // History log
            if (diagnosesHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Past Diagnosis Reports",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                diagnosesHistory.forEach { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.setDiagnosisResult(
                                    com.example.api.dto.DiseaseDiagnosisDTO(
                                        plantName = report.plantName,
                                        diseaseName = report.diseaseName,
                                        confidence = report.confidence,
                                        symptoms = report.symptoms,
                                        treatmentSteps = report.treatmentSteps.split("\n").filter { it.isNotBlank() },
                                        homeRemedies = report.homeRemedies.split("\n").filter { it.isNotBlank() },
                                        prevention = report.prevention
                                    )
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Coronavirus, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(report.diseaseName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                Text("Plant: ${report.plantName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text(
                                text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(report.timestamp)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- SCREEN 4: CARE GUIDE & GROWTH PLANNER SCREEN ---
@Composable
fun CareGuideScreen(viewModel: GardenViewModel) {
    val carePlanResult by viewModel.carePlanResult.collectAsStateWithLifecycle()
    val isGeneratingCarePlan by viewModel.isGeneratingCarePlan.collectAsStateWithLifecycle()

    var plantSearchInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Natural Tones Spacious Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "PERSONALIZED CARE GUIDE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF414941)
                )
                Text(
                    text = "Growth Guide",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF112611)
                )
            }
            // Elegant circle container matching the HTML spec
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD7E8DE))
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFFC2D1C7),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "MenuBook icon",
                    tint = Color(0xFF386B4F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Generate Growth Guide",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Write any plant name to get customized sunlight, humidity, fertilizer, and soil recipes instantly.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = plantSearchInput,
                onValueChange = { plantSearchInput = it },
                label = { Text("E.g. Monstera, Peace Lily, Lavender") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("care_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (plantSearchInput.isNotBlank()) {
                        viewModel.generateCarePlan(plantSearchInput)
                    }
                },
                enabled = !isGeneratingCarePlan && plantSearchInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_care_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGeneratingCarePlan) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Formulating Guide...")
                } else {
                    Icon(Icons.Default.MenuBook, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Guide")
                }
            }

            AnimatedVisibility(
                visible = carePlanResult != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                carePlanResult?.let { plan ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Yard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = plan.plantName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Core Growth Conditions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))

                            ConditionRow(icon = Icons.Default.WaterDrop, title = "Watering Loop", value = plan.wateringFrequency)
                            ConditionRow(icon = Icons.Default.LightMode, title = "Sunlight", value = plan.sunlightRequirements)
                            ConditionRow(icon = Icons.Default.Cloud, title = "Humidity", value = plan.humidity)
                            ConditionRow(icon = Icons.Default.Thermostat, title = "Temperature Range", value = plan.temperature)
                            ConditionRow(icon = Icons.Default.Terrain, title = "Ideal Soil", value = plan.soilType)
                            ConditionRow(icon = Icons.Default.Grass, title = "Fertilization", value = plan.fertilizer)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Actionable Care Tips", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))

                            plan.careTips.forEach { tip ->
                                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tip, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Fast save template
                            Button(
                                onClick = {
                                    viewModel.addPlantToGarden(
                                        Plant(
                                            name = plan.plantName,
                                            customName = plan.plantName,
                                            species = plan.plantName,
                                            wateringIntervalDays = plan.wateringIntervalDays,
                                            sunlight = plan.sunlightRequirements.take(40) + "...",
                                            careInstructions = "Light: ${plan.sunlightRequirements}\n\nWater: ${plan.wateringFrequency}\n\nSoil: ${plan.soilType}\n\nFertilizer: ${plan.fertilizer}"
                                        )
                                    )
                                    viewModel.clearCarePlanResult()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_care_plan_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Custom Buddy with this Plan")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFE0E3DA), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Live Seasonal Care Advice",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Fetch current seasonal care advice grounded in real-time Google Search based on your location and environment.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            var locationInput by remember { mutableStateOf("New York, USA") }
            var seasonInput by remember { mutableStateOf("Summer (June)") }

            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = { Text("Your Location / Region") },
                placeholder = { Text("E.g., Seattle, WA or London, UK") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seasonal_location_input"),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF386B4F))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = seasonInput,
                onValueChange = { seasonInput = it },
                label = { Text("Season or Current Month") },
                placeholder = { Text("E.g., Summer (June) or Autumn") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seasonal_season_input"),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFF386B4F))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val isFetchingSeasonalCare by viewModel.isFetchingSeasonalCare.collectAsStateWithLifecycle()
            val seasonalCareTips by viewModel.seasonalCareTips.collectAsStateWithLifecycle()

            Button(
                onClick = {
                    if (locationInput.isNotBlank() && seasonInput.isNotBlank()) {
                        viewModel.fetchSeasonalCareTips(locationInput, seasonInput)
                    }
                },
                enabled = !isFetchingSeasonalCare && locationInput.isNotBlank() && seasonInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("fetch_seasonal_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isFetchingSeasonalCare) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Searching Google & Analyzing...")
                } else {
                    Icon(Icons.Default.CloudSync, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetch Grounded Tips")
                }
            }

            AnimatedVisibility(
                visible = seasonalCareTips != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                seasonalCareTips?.let { tips ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Current Care Advice",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$locationInput • $seasonInput",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            GroundedTextRenderer(text = tips.content)

                            if (tips.queries.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Google Search Queries:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF386B4F)
                                )
                                tips.queries.forEach { query ->
                                    Text(
                                        text = "🔍 \"$query\"",
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF5C6258),
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                            }

                            if (tips.sources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Grounded Sources Used:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF386B4F)
                                )
                                val uriHandler = LocalUriHandler.current
                                tips.sources.forEach { (title, uri) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                try {
                                                    uriHandler.openUri(uri)
                                                } catch (e: Exception) {
                                                    // Ignore
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color(0xFF386B4F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            color = Color(0xFF22553B),
                                            textDecoration = TextDecoration.Underline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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
fun GroundedTextRenderer(text: String) {
    val lines = text.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("###")) {
                Text(
                    text = trimmed.removePrefix("###").trim(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF112611),
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            } else if (trimmed.startsWith("##")) {
                Text(
                    text = trimmed.removePrefix("##").trim(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF112611),
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
            } else if (trimmed.startsWith("*") || trimmed.startsWith("-")) {
                val bulletText = trimmed.substring(1).trim()
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text("•", fontWeight = FontWeight.Bold, color = Color(0xFF386B4F), modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = bulletText,
                        fontSize = 13.sp,
                        color = Color(0xFF3E443F)
                    )
                }
            } else if (trimmed.isNotEmpty()) {
                Text(
                    text = trimmed,
                    fontSize = 13.sp,
                    color = Color(0xFF3E443F),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ConditionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


// --- SCREEN SUB-COMPONENTS: ADD PLANT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantManualDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, nickname: String, spec: String, interval: Int, sun: String, diff: String, instructions: String, location: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var intervalInput by remember { mutableStateOf("7") }
    var sunlightInput by remember { mutableStateOf("Bright Indirect Light") }
    var difficultyInput by remember { mutableStateOf("Easy") }
    var instructionsInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("Indoor") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Plant Buddy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Common Name (e.g. Monstera)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_plant_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (e.g. Monty)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text("Species (e.g. Monstera deliciosa)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = intervalInput,
                    onValueChange = { intervalInput = it },
                    label = { Text("Watering Frequency (Every X Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sunlightInput,
                    onValueChange = { sunlightInput = it },
                    label = { Text("Sunlight Requirements") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Location Group",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5C6258).copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Indoor", "Balcony", "Garden").forEach { loc ->
                        val isSelected = locationInput == loc
                        Surface(
                            onClick = { locationInput = loc },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF386B4F) else Color(0xFFF2F5EB),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF386B4F) else Color(0xFFE0E3DA)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("dialog_location_$loc")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val icon = when (loc) {
                                    "Indoor" -> Icons.Default.Home
                                    "Balcony" -> Icons.Default.WbSunny
                                    else -> Icons.Default.Spa
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF386B4F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = loc,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Color(0xFF112611)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = instructionsInput,
                    onValueChange = { instructionsInput = it },
                    label = { Text("Custom Care Instructions (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    name,
                                    nickname,
                                    species.ifBlank { name },
                                    intervalInput.toIntOrNull() ?: 7,
                                    sunlightInput,
                                    difficultyInput,
                                    instructionsInput,
                                    locationInput
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("dialog_save_button")
                    ) {
                        Text("Save Plant")
                    }
                }
            }
        }
    }
}

// --- UTILITY HELPER ---
fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: java.io.InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

// --- CameraX Helpers ---
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture,
    onViewfinderReady: (PreviewView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                onViewfinderReady(this)
            }
        },
        modifier = modifier,
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (Bitmap) -> Unit,
    onError: (Throwable) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "plant_scan_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    onSuccess(bitmap)
                    try { photoFile.delete() } catch (e: Exception) {}
                } else {
                    onError(Exception("Failed to decode bitmap from captured file"))
                }
            }

            override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
