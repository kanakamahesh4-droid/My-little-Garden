package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.window.DialogProperties
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
    val showSubscriptionRequired by viewModel.showSubscriptionRequired.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFeedbackModal by remember { mutableStateOf(false) }

    if (isSyncing) {
        SyncingOverlay(statusText = "Syncing with cloud...".localized(appLanguage))
    }

    val currentError = errorState
    if (currentError != null) {
        ApiErrorOverlay(
            errorMsg = currentError,
            onDismiss = { viewModel.clearError() },
            onRetry = { viewModel.clearError() }
        )
    }

    if (showSubscriptionRequired) {
        SubscriptionModal(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissSubscriptionScreen() }
        )
    }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var hasLoadedProfile by remember { mutableStateOf(false) }
    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            hasLoadedProfile = true
        }
    }

    if (!hasLoadedProfile) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF386B4F))
        }
    } else if (userProfile?.isLoggedIn == false) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
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
                        label = { Text("My Garden".localized(appLanguage), fontWeight = if (currentRoute == Destinations.MY_GARDEN) FontWeight.SemiBold else FontWeight.Medium) },
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
                        label = { Text("Identify".localized(appLanguage), fontWeight = if (currentRoute == Destinations.IDENTIFY) FontWeight.SemiBold else FontWeight.Medium) },
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
                        label = { Text("Diagnose".localized(appLanguage), fontWeight = if (currentRoute == Destinations.DIAGNOSE) FontWeight.SemiBold else FontWeight.Medium) },
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
                        label = { Text("Care Guide".localized(appLanguage), fontWeight = if (currentRoute == Destinations.CARE_GUIDE) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = navigationColors,
                        modifier = Modifier.testTag("nav_care_guide")
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFeedbackModal = true },
                containerColor = Color(0xFF386B4F),
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("feedback_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Get in Touch & Feedback",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        if (showFeedbackModal) {
            FeedbackModal(onDismiss = { showFeedbackModal = false })
        }
        NavHost(
            navController = navController,
            startDestination = Destinations.MY_GARDEN,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            }
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
}

@Composable
fun ApiErrorOverlay(
    errorMsg: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val isRateLimit = errorMsg.contains("429")
    var cooldown by remember { mutableStateOf(if (isRateLimit) 10 else 0) }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            kotlinx.coroutines.delay(1000L)
            cooldown -= 1
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isRateLimit) Icons.Default.HourglassEmpty else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isRateLimit) "Too Many Requests" else "Something went wrong",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (cooldown == 0) {
                            onRetry()
                        }
                    },
                    enabled = cooldown == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                        contentColor = MaterialTheme.colorScheme.errorContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (cooldown > 0) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cooling down... ${cooldown}s")
                    } else {
                        Text("Try Again")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

// --- SCREEN 1: MY GARDEN SCREEN ---
@Composable
fun MyGardenScreen(viewModel: GardenViewModel, onNavigateToIdentify: () -> Unit) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFertilizersDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }
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
                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val displayName = userProfile?.displayName?.uppercase()?.let { if (it.isNotBlank()) " $it" else "" } ?: ""
                Text(
                    text = "GOOD MORNING$displayName,".localized(appLanguage),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF414941)
                )
                Text(
                    text = "Maddy's Garden".localized(appLanguage),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF112611)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2EBE4))
                        .clickable { showQRScanner = true }
                        .testTag("qr_scanner_button")
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFC2D1C7),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = Color(0xFF386B4F),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2EBE4))
                        .clickable { showSettingsModal = true }
                        .testTag("settings_button")
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFC2D1C7),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF386B4F),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Elegant About Developer circle container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2EBE4))
                        .clickable { showAboutDialog = true }
                        .testTag("about_app_button")
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFC2D1C7),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About Developer",
                        tint = Color(0xFF386B4F),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Elegant Membership/Subscription circle container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3E0))
                        .clickable { viewModel.triggerSubscriptionScreen() }
                        .testTag("subscription_menu_button")
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFFFB74D),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Subscription",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(20.dp)
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
        }

        if (plants.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
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

                Spacer(modifier = Modifier.height(24.dp))
                EasyHouseholdFertilizersCard(onClick = { showFertilizersDialog = true })

                Spacer(modifier = Modifier.height(32.dp))
                AboutDeveloperSection()
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
                        .weight(1f)
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        // Fast summary card (Identify/Status CTA layout from HTML)
                        GardenSummaryCard(plants = plants)
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        // Today's Care Guide custom layout block from HTML design spec
                        TodayCareGuideCard()
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        WeeklyReminderCard(plants = plants)
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EasyHouseholdFertilizersCard(onClick = { showFertilizersDialog = true })
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "My Plant Buddies",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF112611),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredPlants, key = { it.id }) { plant ->
                        PlantGridItem(
                            plant = plant,
                            onWaterClick = { viewModel.waterPlant(plant.id) },
                            onDeleteClick = { viewModel.deletePlantFromGarden(plant) },
                            onLocationChange = { newLoc -> viewModel.updatePlant(plant.copy(location = newLoc)) },
                            viewModel = viewModel
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AboutDeveloperSection()
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            AboutDeveloperSection(onDismiss = { showAboutDialog = false })
        }
    }

    if (showSettingsModal) {
        SettingsModal(
            viewModel = viewModel,
            onDismiss = { showSettingsModal = false }
        )
    }

    if (showQRScanner) {
        QRScannerScreen(
            onDismiss = { showQRScanner = false },
            onQrCodeScanned = { qrResult ->
                showQRScanner = false
                searchQuery = qrResult
            }
        )
    }

    if (showAddDialog) {
        AddPlantManualDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, nickname, spec, interval, sun, diff, instructions, location, addedDate ->
                viewModel.addPlantToGarden(
                    Plant(
                        name = name,
                        customName = nickname.ifBlank { name },
                        species = spec,
                        wateringIntervalDays = interval,
                        sunlight = sun,
                        difficulty = diff,
                        careInstructions = instructions,
                        location = location,
                        addedDate = addedDate
                    )
                )
                showAddDialog = false
            }
        )
    }

    if (showFertilizersDialog) {
        EasyHouseholdFertilizersDialog(onDismiss = { showFertilizersDialog = false })
    }
}

@Composable
fun AboutDeveloperSection(modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5EB)),
        border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
        modifier = modifier.fillMaxWidth().testTag("about_developer_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ABOUT THE DEVELOPER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF5C6258)
                )
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF5C6258),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful 3D-effect Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF386B4F), Color(0xFF2B523C))
                        )
                    )
                    .drawBehind {
                        // Glossy 3D overlay
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = size.minDimension / 2f,
                            center = Offset(size.width * 0.35f, size.height * 0.35f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Developer Portrait",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Kanaka Mahesh",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF112611)
            )
            Text(
                text = "Developer of Maddy's Garden",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF386B4F)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE0E3DA).copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Brief Description
            Text(
                text = "Hey User,\n\nMy Name is Kanaka Mahesh, I built \"Maddy's Garden\". Thank you for trying out the app!\n\nI’m trying to build the best Garden app ever, so any feedback you have would be greatly appreciated.\n\nYou can email me at kanakamahesh4@gmail.com, and feel free to follow me on Instagram : @maddy_pspk_official",
                fontSize = 13.sp,
                color = Color(0xFF414941),
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeveloperInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email Feedback",
                    value = "kanakamahesh4@gmail.com"
                )
                DeveloperInfoRow(
                    icon = Icons.Default.AlternateEmail,
                    label = "Instagram",
                    value = "maddy_pspk_official"
                )
                DeveloperInfoRow(
                    icon = Icons.Default.Code,
                    label = "App Framework",
                    value = "Jetpack Compose & Kotlin"
                )
                DeveloperInfoRow(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI Engine",
                    value = "Gemini Pro / Flash REST"
                )
                DeveloperInfoRow(
                    icon = Icons.Default.Settings,
                    label = "App Version",
                    value = "v1.0.0 (Stable)"
                )
            }
        }
    }
}

@Composable
fun DeveloperInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF386B4F),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5C6258),
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF112611),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
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

data class FertilizerRemedy(
    val name: String,
    val description: String,
    val bestFor: String,
    val tips: String,
    val colorTheme: Color,
    val iconTint: Color
)

@Composable
fun FertilizerRemedyGraphic(remedyName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E3DA), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height
            val center = center

            when (remedyName) {
                "Aquarium Water" -> {
                    val path = Path().apply {
                        moveTo(w * 0.15f, h * 0.2f)
                        lineTo(w * 0.15f, h * 0.8f)
                        quadraticTo(w * 0.15f, h * 0.85f, w * 0.2f, h * 0.85f)
                        lineTo(w * 0.8f, h * 0.85f)
                        quadraticTo(w * 0.85f, h * 0.85f, w * 0.85f, h * 0.8f)
                        lineTo(w * 0.85f, h * 0.2f)
                    }
                    drawPath(path, color = Color(0xFFD0E1FD), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
                    val wavePath = Path().apply {
                        moveTo(w * 0.18f, h * 0.45f)
                        quadraticTo(w * 0.35f, h * 0.4f, w * 0.5f, h * 0.45f)
                        quadraticTo(w * 0.65f, h * 0.5f, w * 0.82f, h * 0.45f)
                    }
                    drawPath(wavePath, color = Color(0xFF3498DB), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    val fishPath = Path().apply {
                        moveTo(w * 0.4f, h * 0.6f)
                        quadraticTo(w * 0.55f, h * 0.5f, w * 0.7f, h * 0.6f)
                        lineTo(w * 0.75f, h * 0.53f)
                        lineTo(w * 0.75f, h * 0.67f)
                        lineTo(w * 0.7f, h * 0.6f)
                        quadraticTo(w * 0.55f, h * 0.7f, w * 0.4f, h * 0.6f)
                    }
                    drawPath(fishPath, color = Color(0xFFE67E22))
                }
                "Bananas" -> {
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.3f)
                        quadraticTo(w * 0.5f, h * 0.8f, w * 0.8f, h * 0.6f)
                        quadraticTo(w * 0.5f, h * 0.65f, w * 0.25f, h * 0.35f)
                    }
                    drawPath(path, color = Color(0xFFF1C40F))
                    
                    val stem = Path().apply {
                        moveTo(w * 0.2f, h * 0.3f)
                        lineTo(w * 0.15f, h * 0.25f)
                    }
                    drawPath(stem, color = Color(0xFF7E5109), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                }
                "Coffee Grounds" -> {
                    drawCircle(color = Color(0xFFE2EBE4), radius = w * 0.35f, center = center)
                    drawCircle(color = Color(0xFF5D4037), radius = w * 0.28f, center = center)
                    drawCircle(color = Color(0xFF3E2723), radius = 3.dp.toPx(), center = Offset(w * 0.45f, h * 0.4f))
                    drawCircle(color = Color(0xFF3E2723), radius = 2.dp.toPx(), center = Offset(w * 0.55f, h * 0.52f))
                    drawCircle(color = Color(0xFF3E2723), radius = 2.5.dp.toPx(), center = Offset(w * 0.38f, h * 0.6f))
                }
                "Cooking Water" -> {
                    val potPath = Path().apply {
                        moveTo(w * 0.25f, h * 0.45f)
                        lineTo(w * 0.75f, h * 0.45f)
                        lineTo(w * 0.7f, h * 0.8f)
                        lineTo(w * 0.3f, h * 0.8f)
                        close()
                    }
                    drawPath(potPath, color = Color(0xFFBDC3C7))
                    drawCircle(color = Color(0xFF7F8C8D), radius = w * 0.06f, center = Offset(w * 0.22f, h * 0.55f))
                    drawCircle(color = Color(0xFF7F8C8D), radius = w * 0.06f, center = Offset(w * 0.78f, h * 0.55f))
                    val steam1 = Path().apply {
                        moveTo(w * 0.38f, h * 0.38f)
                        quadraticTo(w * 0.33f, h * 0.28f, w * 0.38f, h * 0.18f)
                    }
                    drawPath(steam1, color = Color(0xFF3498DB), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    val steam2 = Path().apply {
                        moveTo(w * 0.62f, h * 0.38f)
                        quadraticTo(w * 0.57f, h * 0.28f, w * 0.62f, h * 0.18f)
                    }
                    drawPath(steam2, color = Color(0xFF3498DB), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
                "Egg Shells" -> {
                    val shellLeft = Path().apply {
                        moveTo(w * 0.25f, h * 0.5f)
                        quadraticTo(w * 0.25f, h * 0.8f, w * 0.45f, h * 0.8f)
                        lineTo(w * 0.48f, h * 0.65f)
                        lineTo(w * 0.42f, h * 0.55f)
                        lineTo(w * 0.48f, h * 0.45f)
                        close()
                    }
                    drawPath(shellLeft, color = Color(0xFFF2F4F4))
                    drawPath(shellLeft, color = Color(0xFFBDC3C7), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

                    val shellRight = Path().apply {
                        moveTo(w * 0.75f, h * 0.5f)
                        quadraticTo(w * 0.75f, h * 0.8f, w * 0.55f, h * 0.8f)
                        lineTo(w * 0.52f, h * 0.65f)
                        lineTo(w * 0.58f, h * 0.55f)
                        lineTo(w * 0.52f, h * 0.45f)
                        close()
                    }
                    drawPath(shellRight, color = Color(0xFFF2F4F4))
                    drawPath(shellRight, color = Color(0xFFBDC3C7), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                }
                "Epsom Salts" -> {
                    val crystal1 = Path().apply {
                        moveTo(w * 0.5f, h * 0.25f)
                        lineTo(w * 0.62f, h * 0.42f)
                        lineTo(w * 0.5f, h * 0.58f)
                        lineTo(w * 0.38f, h * 0.42f)
                        close()
                    }
                    drawPath(crystal1, color = Color(0xFFEBDEF0))
                    drawPath(crystal1, color = Color(0xFFAF7AC5), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

                    val crystal2 = Path().apply {
                        moveTo(w * 0.3f, h * 0.55f)
                        lineTo(w * 0.4f, h * 0.68f)
                        lineTo(w * 0.3f, h * 0.8f)
                        lineTo(w * 0.2f, h * 0.68f)
                        close()
                    }
                    drawPath(crystal2, color = Color(0xFFEBDEF0))
                    drawPath(crystal2, color = Color(0xFFAF7AC5), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

                    val crystal3 = Path().apply {
                        moveTo(w * 0.7f, h * 0.52f)
                        lineTo(w * 0.78f, h * 0.63f)
                        lineTo(w * 0.7f, h * 0.75f)
                        lineTo(w * 0.62f, h * 0.63f)
                        close()
                    }
                    drawPath(crystal3, color = Color(0xFFEBDEF0))
                    drawPath(crystal3, color = Color(0xFFAF7AC5), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                }
                "Wood Ash (From Your Fireplace or Fire Pit)" -> {
                    val log1 = Path().apply {
                        moveTo(w * 0.25f, h * 0.75f)
                        lineTo(w * 0.75f, h * 0.55f)
                    }
                    drawPath(log1, color = Color(0xFF873600), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                    val log2 = Path().apply {
                        moveTo(w * 0.25f, h * 0.55f)
                        lineTo(w * 0.75f, h * 0.75f)
                    }
                    drawPath(log2, color = Color(0xFF873600), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                    
                    drawCircle(color = Color(0xFFBDC3C7).copy(alpha = 0.5f), radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.45f))
                    drawCircle(color = Color(0xFF95A5A6).copy(alpha = 0.5f), radius = w * 0.12f, center = Offset(w * 0.4f, h * 0.38f))
                }
                "Gelatin" -> {
                    val jellyPath = Path().apply {
                        moveTo(w * 0.3f, h * 0.75f)
                        quadraticTo(w * 0.5f, h * 0.8f, w * 0.7f, h * 0.75f)
                        lineTo(w * 0.65f, h * 0.45f)
                        quadraticTo(w * 0.5f, h * 0.4f, w * 0.35f, h * 0.45f)
                        close()
                    }
                    drawPath(jellyPath, color = Color(0xFFFADBD8))
                    drawPath(jellyPath, color = Color(0xFFEC7063), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    
                    val wiggle = Path().apply {
                        moveTo(w * 0.45f, h * 0.55f)
                        quadraticTo(w * 0.5f, h * 0.58f, w * 0.55f, h * 0.55f)
                    }
                    drawPath(wiggle, color = Color(0xFFEC7063), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
                "Green Tea" -> {
                    val cupPath = Path().apply {
                        moveTo(w * 0.25f, h * 0.45f)
                        lineTo(w * 0.75f, h * 0.45f)
                        quadraticTo(w * 0.75f, h * 0.75f, w * 0.5f, h * 0.75f)
                        quadraticTo(w * 0.25f, h * 0.75f, w * 0.25f, h * 0.45f)
                    }
                    drawPath(cupPath, color = Color(0xFFD4EFDF))
                    drawPath(cupPath, color = Color(0xFF27AE60), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))

                    val leafPath = Path().apply {
                        moveTo(w * 0.42f, h * 0.58f)
                        quadraticTo(w * 0.5f, h * 0.5f, w * 0.58f, h * 0.58f)
                        quadraticTo(w * 0.5f, h * 0.66f, w * 0.42f, h * 0.58f)
                    }
                    drawPath(leafPath, color = Color(0xFF2ECC71))
                }
                "Hair" -> {
                    val wave1 = Path().apply {
                        moveTo(w * 0.3f, h * 0.25f)
                        quadraticTo(w * 0.45f, h * 0.5f, w * 0.35f, h * 0.75f)
                    }
                    drawPath(wave1, color = Color(0xFF855428), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

                    val wave2 = Path().apply {
                        moveTo(w * 0.5f, h * 0.25f)
                        quadraticTo(w * 0.35f, h * 0.5f, w * 0.55f, h * 0.75f)
                    }
                    drawPath(wave2, color = Color(0xFFBA4A00), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

                    val wave3 = Path().apply {
                        moveTo(w * 0.7f, h * 0.25f)
                        quadraticTo(w * 0.6f, h * 0.5f, w * 0.65f, h * 0.75f)
                    }
                    drawPath(wave3, color = Color(0xFFD35400), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                }
                "Matchsticks" -> {
                    val stick1 = Path().apply {
                        moveTo(w * 0.35f, h * 0.75f)
                        lineTo(w * 0.55f, h * 0.35f)
                    }
                    drawPath(stick1, color = Color(0xFFF5CBA7), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx(), cap = StrokeCap.Square))
                    drawCircle(color = Color(0xFFC0392B), radius = 8.dp.toPx(), center = Offset(w * 0.55f, h * 0.35f))

                    val stick2 = Path().apply {
                        moveTo(w * 0.55f, h * 0.75f)
                        lineTo(w * 0.65f, h * 0.45f)
                    }
                    drawPath(stick2, color = Color(0xFFF5CBA7), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx(), cap = StrokeCap.Square))
                    drawCircle(color = Color(0xFFC0392B), radius = 8.dp.toPx(), center = Offset(w * 0.65f, h * 0.45f))
                }
                "Powdered Milk" -> {
                    val carton = Path().apply {
                        moveTo(w * 0.35f, h * 0.35f)
                        lineTo(w * 0.5f, h * 0.22f)
                        lineTo(w * 0.65f, h * 0.35f)
                        lineTo(w * 0.65f, h * 0.8f)
                        lineTo(w * 0.35f, h * 0.8f)
                        close()
                    }
                    drawPath(carton, color = Color(0xFFF2F4F4))
                    drawPath(carton, color = Color(0xFF95A5A6), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))

                    drawRect(color = Color(0xFF5DADE2), topLeft = Offset(w * 0.35f, h * 0.48f), size = Size(w * 0.3f, h * 0.16f))
                }
                else -> {
                    drawCircle(color = Color(0xFFF7DC6F), radius = w * 0.12f, center = center)
                    for (i in 0 until 5) {
                        val angle = (i * 2 * Math.PI / 5).toFloat()
                        val petalOffset = Offset(
                            center.x + (w * 0.22f) * Math.cos(angle.toDouble()).toFloat(),
                            center.y + (w * 0.22f) * Math.sin(angle.toDouble()).toFloat()
                        )
                        drawCircle(color = Color(0xFFF5B041), radius = w * 0.14f, center = petalOffset)
                    }
                    drawCircle(color = Color(0xFFF7DC6F), radius = w * 0.12f, center = center)
                }
            }
        }
    }
}

fun scheduleWeeklyReminder(
    context: Context,
    enabled: Boolean,
    dayOfWeek: Int,
    hour: Int,
    minute: Int,
    plantName: String
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, com.example.GrowthReminderReceiver::class.java).apply {
        putExtra("plant_name", if (plantName == "All Plants") "" else plantName)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (!enabled) {
        alarmManager.cancel(pendingIntent)
        return
    }

    val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        
        // If the scheduled time is in the past, schedule for next week
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.WEEK_OF_YEAR, 1)
        }
    }

    try {
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    } catch (e: Exception) {
        Log.e("WeeklyReminder", "Error scheduling alarm: ${e.message}")
    }
}

fun sendImmediateTestNotification(context: Context, plantName: String) {
    val intent = Intent(context, com.example.GrowthReminderReceiver::class.java).apply {
        putExtra("plant_name", if (plantName == "All Plants") "" else plantName)
    }
    context.sendBroadcast(intent)
}

@Composable
fun WeeklyReminderCard(plants: List<Plant>) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("garden_reminder_prefs", Context.MODE_PRIVATE) }

    var isEnabled by remember { mutableStateOf(prefs.getBoolean("reminder_enabled", false)) }
    var selectedDay by remember { mutableStateOf(prefs.getInt("reminder_day", Calendar.SUNDAY)) }
    var selectedHour by remember { mutableStateOf(prefs.getInt("reminder_hour", 9)) }
    var selectedMinute by remember { mutableStateOf(prefs.getInt("reminder_minute", 0)) }
    var selectedPlantName by remember { mutableStateOf(prefs.getString("reminder_plant_name", "All Plants") ?: "All Plants") }
    
    var showSavedMessage by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    var permissionGranted by remember { mutableStateOf(hasPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notifications permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications permission denied. Settings will save, but notifications may not show.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
        border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_reminder_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF386B4F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WEEKLY GROWTH TRACKER REMINDER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF386B4F)
                        )
                        Text(
                            text = "Log Height & Leaf Count",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF112611)
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        isEnabled = checked
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF386B4F),
                        uncheckedThumbColor = Color(0xFF5C6258),
                        uncheckedTrackColor = Color(0xFFE0E3DA)
                    ),
                    modifier = Modifier.testTag("reminder_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Keep your plant growth history updated! Enable weekly reminders to measure height or leaf count on your chosen schedule.",
                fontSize = 12.sp,
                color = Color(0xFF414941),
                lineHeight = 16.sp
            )

            AnimatedVisibility(visible = isEnabled) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color(0xFFC2D1C7).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Choose Reminder Day:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF386B4F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val days = listOf(
                            "Sun" to Calendar.SUNDAY,
                            "Mon" to Calendar.MONDAY,
                            "Tue" to Calendar.TUESDAY,
                            "Wed" to Calendar.WEDNESDAY,
                            "Thu" to Calendar.THURSDAY,
                            "Fri" to Calendar.FRIDAY,
                            "Sat" to Calendar.SATURDAY
                        )
                        days.forEach { (name, value) ->
                            val isDaySelected = selectedDay == value
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDaySelected) Color(0xFF386B4F) else Color(0xFFE2EBE4))
                                    .clickable { selectedDay = value }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("reminder_day_chip_${name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isDaySelected) Color.White else Color(0xFF386B4F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Choose Reminder Time:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF386B4F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val times = listOf(
                            Triple("Morning (9 AM)", 9, 0),
                            Triple("Noon (12 PM)", 12, 0),
                            Triple("Evening (6 PM)", 18, 0)
                        )
                        times.forEach { (label, h, m) ->
                            val isTimeSelected = selectedHour == h && selectedMinute == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTimeSelected) Color(0xFF386B4F) else Color(0xFFE2EBE4))
                                    .clickable {
                                        selectedHour = h
                                        selectedMinute = m
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("reminder_time_chip_${h}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isTimeSelected) Color.White else Color(0xFF386B4F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Choose Target Plant Buddy:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF386B4F)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF386B4F)),
                            border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("reminder_plant_select_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedPlantName == "All Plants") "All Potted Buddies" else selectedPlantName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF386B4F))
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Potted Buddies", fontSize = 12.sp) },
                                onClick = {
                                    selectedPlantName = "All Plants"
                                    dropdownExpanded = false
                                },
                                modifier = Modifier.testTag("reminder_plant_option_all")
                            )
                            plants.forEach { plant ->
                                val displayName = plant.customName.ifBlank { plant.name }
                                DropdownMenuItem(
                                    text = { Text(displayName, fontSize = 12.sp) },
                                    onClick = {
                                        selectedPlantName = displayName
                                        dropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("reminder_plant_option_${plant.id}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    sendImmediateTestNotification(context, selectedPlantName)
                                    Toast.makeText(context, "Test notification broadcasted!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF386B4F)),
                            border = BorderStroke(1.dp, Color(0xFF386B4F)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("send_test_reminder_button")
                        ) {
                            Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                prefs.edit().apply {
                                    putBoolean("reminder_enabled", isEnabled)
                                    putInt("reminder_day", selectedDay)
                                    putInt("reminder_hour", selectedHour)
                                    putInt("reminder_minute", selectedMinute)
                                    putString("reminder_plant_name", selectedPlantName)
                                    apply()
                                }
                                scheduleWeeklyReminder(
                                    context = context,
                                    enabled = isEnabled,
                                    dayOfWeek = selectedDay,
                                    hour = selectedHour,
                                    minute = selectedMinute,
                                    plantName = selectedPlantName
                                )
                                showSavedMessage = true
                                Toast.makeText(context, "Weekly Reminder Schedule Saved!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .testTag("save_reminder_button")
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Schedule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showSavedMessage) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EDE4)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF386B4F), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Successfully scheduled for every ${
                                        when (selectedDay) {
                                            Calendar.SUNDAY -> "Sunday"
                                            Calendar.MONDAY -> "Monday"
                                            Calendar.TUESDAY -> "Tuesday"
                                            Calendar.WEDNESDAY -> "Wednesday"
                                            Calendar.THURSDAY -> "Thursday"
                                            Calendar.FRIDAY -> "Friday"
                                            Calendar.SATURDAY -> "Saturday"
                                            else -> "chosen day"
                                        }
                                    } at ${if (selectedHour < 10) "0" else ""}$selectedHour:00.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1B3827)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EasyHouseholdFertilizersCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EDE4)),
        border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("easy_fertilizers_dashboard_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EASY HOUSEHOLD FERTILIZERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF386B4F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "12 Simple Kitchen & Home Items to Boost Plant Growth Naturally!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF112611),
                        lineHeight = 20.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF386B4F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFlorist,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Turn everyday waste like coffee grounds, banana peels, and cooking water into rich nutrients for your soil. Save money and grow organically.",
                fontSize = 12.sp,
                color = Color(0xFF414941),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Explore 12 Remedies",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386B4F)
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF386B4F),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EasyHouseholdFertilizersDialog(onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    val remedies = remember {
        listOf(
            FertilizerRemedy(
                name = "Aquarium Water",
                description = "Water your plants with the aquarium water taken right out of the tank when cleaning it. Use only fresh water and not Saltwater. The fish waste makes a great plant fertiliser.",
                bestFor = "Foliage plants, leafy greens, & all indoor houseplants",
                tips = "Use only freshwater aquarium water. Avoid saltwater entirely.",
                colorTheme = Color(0xFFE3F2FD),
                iconTint = Color(0xFF1E88E5)
            ),
            FertilizerRemedy(
                name = "Bananas",
                description = "Bananas are not only tasty and healthy for humans, but they also benefit many different plants. When planting roses, bury a banana (or just the peel) in the hole alongside the rose. As the rose grows, bury bananas or banana peels into the top layer of the soil. Both of these methods will provide the much-needed potassium that plants need for proper growth.",
                bestFor = "Roses, flowering shrubs, & potassium-hungry plants",
                tips = "Bury peels deep enough to avoid attracting gnats or rodents.",
                colorTheme = Color(0xFFFFFDE7),
                iconTint = Color(0xFFFBC02D)
            ),
            FertilizerRemedy(
                name = "Coffee Grounds",
                description = "Coffee grounds are particularly useful for plants such as blueberries, evergreens, roses, camellias, avocados, and many fruit trees. Use dry coffee grounds and scatter them on the soil. Wet coffee grounds may lead to the growth of algae.",
                bestFor = "Acid-loving plants, blueberries, roses, & fruit trees",
                tips = "Use fully dried grounds to prevent mold and algae growth.",
                colorTheme = Color(0xFFEFEBE9),
                iconTint = Color(0xFF6D4C41)
            ),
            FertilizerRemedy(
                name = "Cooking Water",
                description = "Several nutrients are released into the water in which the food is cooked. Water that is used to boil potatoes, vegetables, eggs, and even pasta can be utilized as a fertiliser. Just cool the water before applying it to soil.",
                bestFor = "Container houseplants, garden beds, & foliage plants",
                tips = "Ensure water has cooled fully and contains no added salt or oils.",
                colorTheme = Color(0xFFE0F2F1),
                iconTint = Color(0xFF00897B)
            ),
            FertilizerRemedy(
                name = "Egg Shells",
                description = "Egg shells contain nitrogen, trace elements, and calcium. Calcium is an essential plant nutrient which plays a fundamental part in cell manufacture and growth. Plant growth removes large quantities of calcium from the soil, and calcium must be replenished, so the use of egg shells can help the plant. Crush the egg shells and sprinkle on the ground.",
                bestFor = "Tomatoes (prevents end rot), peppers, & eggplants",
                tips = "Bake and crush eggshells as finely as possible for faster root uptake.",
                colorTheme = Color(0xFFF5F5F5),
                iconTint = Color(0xFF616161)
            ),
            FertilizerRemedy(
                name = "Epsom Salts",
                description = "It contains magnesium and sulphur. Add salt to the soil combined with water to provide soil with magnesium and sulphur. A dose of an Epsom salts solution increases fruit and flower production in roses, tomatoes, potatoes, peppers, and houseplants.",
                bestFor = "Roses, tomatoes, potatoes, peppers, & houseplants",
                tips = "Dissolve 1 tablespoon in 4L water to spray directly onto leaves.",
                colorTheme = Color(0xFFF3E5F5),
                iconTint = Color(0xFF8E24AA)
            ),
            FertilizerRemedy(
                name = "Wood Ash (From Your Fireplace or Fire Pit)",
                description = "Ashes can be sprinkled onto your soil to supply potassium and calcium carbonate. Hardwood is best. Ashes are alkaline and can increase alkalinity in the ground.",
                bestFor = "Alkaline-loving plants, root vegetables, lawns, & shrubs",
                tips = "Apply sparingly and avoid using on acid-loving plants.",
                colorTheme = Color(0xFFECEFF1),
                iconTint = Color(0xFF546E7A)
            ),
            FertilizerRemedy(
                name = "Gelatin",
                description = "Gelatin is a great nitrogen source. Dissolve in hot water, then add cold water and pour on soil once a month.",
                bestFor = "Foliage house plants, monsteras, & ferns",
                tips = "Use only plain, unflavored, and sugar-free gelatin.",
                colorTheme = Color(0xFFFBE9E7),
                iconTint = Color(0xFFF4511E)
            ),
            FertilizerRemedy(
                name = "Green Tea",
                description = "Green tea is especially beneficial for raspberry plants as raspberry plants require a great amount of iron.",
                bestFor = "Raspberries, ferns, & acid-preferring plants",
                tips = "Dilute cold green tea 1:1 with clean water before application.",
                colorTheme = Color(0xFFE8F5E9),
                iconTint = Color(0xFF43A047)
            ),
            FertilizerRemedy(
                name = "Hair",
                description = "Hair is a good source of nitrogen. Add human or animal hair to soil for good results.",
                bestFor = "Outdoor garden beds & slow-release organic compost",
                tips = "Cut hair finely and mix deeply into soil near the root zone.",
                colorTheme = Color(0xFFFBEBE9),
                iconTint = Color(0xFF8D6E63)
            ),
            FertilizerRemedy(
                name = "Matchsticks",
                description = "The old fashioned matches are an excellent source of magnesium. To use this as a fertiliser, just place the whole in the hole with the plant, or soak them in water. The magnesium will dissolve into the water and make application easier.",
                bestFor = "Peppers, tomatoes, roses, & magnesium-hungry plants",
                tips = "Old-fashioned paper matches work best. Soak in water overnight.",
                colorTheme = Color(0xFFFFEBEE),
                iconTint = Color(0xFFE53935)
            ),
            FertilizerRemedy(
                name = "Powdered Milk",
                description = "Powdered milk is not only good for human consumption but also for plants. This source of calcium needs to be mixed into the soil before planting. Since the milk is in powder form, it is ready for use by your plants. Diluted milk can also act as an excellent fertiliser. To keep fungus from affecting your tomato plants, try watering it with diluted cold milk.",
                bestFor = "Tomatoes, peppers, squashes, & calcium-needing plants",
                tips = "Spray diluted milk onto foliage to help prevent leaf spot fungus.",
                colorTheme = Color(0xFFFFF8E1),
                iconTint = Color(0xFFFFB300)
            )
        )
    }

    val filteredRemedies = remember(searchQuery) {
        if (searchQuery.isBlank()) remedies
        else remedies.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.bestFor.contains(searchQuery, ignoreCase = true)
        }
    }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Easy Household Fertilizers",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF112611)
                        )
                        Text(
                            text = "12 natural remedies from your kitchen & home",
                            fontSize = 12.sp,
                            color = Color(0xFF5C6258)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFFF1F6F2), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = Color(0xFF386B4F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search fertilizers... e.g. Banana, Milk", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF386B4F)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null, tint = Color(0xFF386B4F))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fertilizers_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF386B4F),
                        unfocusedBorderColor = Color(0xFFC2D1C7)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Intro Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF386B4F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "There are quite a few common items found in your house, that can be used as plant fertilizer.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1B3827),
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    items(filteredRemedies, key = { it.name }) { remedy ->
                        var isExpanded by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Beautiful Graphic (Reference Image)
                                    FertilizerRemedyGraphic(
                                        remedyName = remedy.name,
                                        modifier = Modifier.size(64.dp)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = remedy.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF112611)
                                        )
                                        Text(
                                            text = if (isExpanded) "Tap to hide details" else "Tap to view instructions",
                                            fontSize = 11.sp,
                                            color = Color(0xFF386B4F),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Expand/Collapse",
                                        tint = Color(0xFF386B4F),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 16.dp)) {
                                        HorizontalDivider(color = Color(0xFFF1F6F2))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = remedy.description,
                                            fontSize = 13.sp,
                                            color = Color(0xFF414941),
                                            lineHeight = 18.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(remedy.colorTheme, RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Column {
                                                    Text("BEST FOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = remedy.iconTint)
                                                    Text(remedy.bestFor, fontSize = 11.sp, color = Color(0xFF112611), fontWeight = FontWeight.Medium)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color(0xFFF1F6F2), RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Column {
                                                    Text("PRO TIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF386B4F))
                                                    Text(remedy.tips, fontSize = 11.sp, color = Color(0xFF112611), fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Button(
                                            onClick = {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                                    data = android.provider.CalendarContract.Events.CONTENT_URI
                                                    putExtra(android.provider.CalendarContract.Events.TITLE, "Fertilize with ${remedy.name}")
                                                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "${remedy.description}\n\nBest for: ${remedy.bestFor}\n\nPro Tip: ${remedy.tips}")
                                                    putExtra(android.provider.CalendarContract.Events.RRULE, "FREQ=MONTHLY")
                                                    putExtra(android.provider.CalendarContract.Events.AVAILABILITY, android.provider.CalendarContract.Events.AVAILABILITY_FREE)
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3EB), contentColor = Color(0xFF386B4F)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add Monthly Reminder to Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD7E8DE)),
                            border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Spa,
                                        contentDescription = null,
                                        tint = Color(0xFF386B4F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Naturally Beautiful & Affordable",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF112611)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Consider using this household natural garden fertilisers and feel good about the fact that you are doing things naturally and saving money in the process. Also making it aptly right that beautiful gardens are not expensive.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF414941),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
fun PlantGridItem(
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
            .aspectRatio(0.85f)
            .clickable { showInstructionsDialog = true }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: Avatar and Health Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFF2F5EB), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = Color(0xFF8D9286),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Health Status Indicator
                Box(
                    modifier = Modifier
                        .background(
                            if (isThirsty) Color(0xFFFFF4E5) else Color(0xFFE8F3EB),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isThirsty) Color(0xFFE8A33F) else Color(0xFF386B4F), CircleShape)
                        )
                        Text(
                            text = if (isThirsty) "Needs Water" else "Healthy",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isThirsty) Color(0xFFE8A33F) else Color(0xFF386B4F)
                        )
                    }
                }
            }
            
            // Middle section: Names
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = plant.customName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF191C19),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = plant.name,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF5C6258),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Added: " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(plant.addedDate)),
                    fontSize = 10.sp,
                    color = Color(0xFF8D9286),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Bottom section: Water button
            Button(
                onClick = onWaterClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isThirsty) Color(0xFFE8A33F) else Color(0xFF386B4F)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Water", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        listOf("Profile", "Journal", "Growth Tracker").forEach { tab ->
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

                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "ProfileJournalTabAnimation"
                    ) { targetTab ->
                        if (targetTab == "Profile") {
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
                        ProfileDetailRow(
                            label = "Date Added",
                            value = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(plant.addedDate))
                        )

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

                        // Retrieve the care tasks and compute metrics from manual logs
                        val allTasks by viewModel.careTasks.collectAsStateWithLifecycle(initialValue = emptyList())
                        val plantTasks = remember(allTasks, plant) {
                            allTasks.filter {
                                it.plantId == plant.id ||
                                (it.plantName.isNotBlank() && (
                                    it.plantName.contains(plant.customName, ignoreCase = true) ||
                                    it.plantName.contains(plant.name, ignoreCase = true) ||
                                    plant.customName.contains(it.plantName, ignoreCase = true)
                                ))
                            }
                        }

                        // Calculate Water Level
                        val lastWateringTask = remember(plantTasks) {
                            plantTasks
                                .filter { it.taskType.equals("Watering", ignoreCase = true) && it.isCompleted && it.completedDate != null }
                                .maxByOrNull { it.completedDate ?: 0L }
                        }
                        val lastWateredTime = remember(plant.lastWatered, lastWateringTask) {
                            maxOf(plant.lastWatered, lastWateringTask?.completedDate ?: 0L)
                        }
                        val waterDaysSince = remember(lastWateredTime) {
                            ((System.currentTimeMillis() - lastWateredTime).coerceAtLeast(0L) / (1000 * 60 * 60 * 24))
                        }
                        val waterLevel = remember(waterDaysSince, plant.wateringIntervalDays) {
                            (1.0f - waterDaysSince.toFloat() / plant.wateringIntervalDays.coerceAtLeast(1)).coerceIn(0.0f, 1.0f)
                        }

                        // Calculate Sunlight Level
                        val lastSunlightTask = remember(plantTasks) {
                            plantTasks
                                .filter { it.taskType.equals("Sunlight/Rotation", ignoreCase = true) && it.isCompleted && it.completedDate != null }
                                .maxByOrNull { it.completedDate ?: 0L }
                        }
                        val sunDaysSince = remember(lastSunlightTask) {
                            if (lastSunlightTask?.completedDate != null) {
                                ((System.currentTimeMillis() - lastSunlightTask.completedDate).coerceAtLeast(0L) / (1000 * 60 * 60 * 24))
                            } else {
                                3L // Assume completed 3 days ago if never done
                            }
                        }
                        val sunLevel = remember(sunDaysSince) {
                            (1.0f - sunDaysSince.toFloat() / 7f).coerceIn(0.15f, 1.0f)
                        }

                        // Calculate Nutrient Level
                        val lastFertilizingTask = remember(plantTasks) {
                            plantTasks
                                .filter { (it.taskType.equals("Fertilizing", ignoreCase = true) || it.taskType.equals("Vitamins", ignoreCase = true)) && it.isCompleted && it.completedDate != null }
                                .maxByOrNull { it.completedDate ?: 0L }
                        }
                        val fertDaysSince = remember(lastFertilizingTask) {
                            if (lastFertilizingTask?.completedDate != null) {
                                ((System.currentTimeMillis() - lastFertilizingTask.completedDate).coerceAtLeast(0L) / (1000 * 60 * 60 * 24))
                            } else {
                                12L // Assume completed 12 days ago if never done
                            }
                        }
                        val nutrientLevel = remember(fertDaysSince) {
                            (1.0f - fertDaysSince.toFloat() / 30f).coerceIn(0.10f, 1.0f)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Real-time Care Metrics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dynamic stats powered by your manual care actions and checklist completions.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        // 1. Radar Chart Display
                        Spacer(modifier = Modifier.height(12.dp))
                        PlantRadarChart(
                            water = waterLevel,
                            sun = sunLevel,
                            nutrients = nutrientLevel,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // 2. Linear Progress Indicators
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5EB)),
                            border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val waterStatus = when {
                                    waterLevel > 0.7f -> "Optimal"
                                    waterLevel > 0.4f -> "Adequate"
                                    else -> "Thirsty!"
                                }
                                CareProgressBar(
                                    icon = Icons.Default.WaterDrop,
                                    label = "Water Level",
                                    value = waterLevel,
                                    color = Color(0xFF2B6CB0),
                                    trackColor = Color(0xFFE2E8F0),
                                    statusText = waterStatus
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val sunStatus = when {
                                    sunLevel > 0.7f -> "Sufficient"
                                    sunLevel > 0.4f -> "Moderate"
                                    else -> "Rotate Soon"
                                }
                                CareProgressBar(
                                    icon = Icons.Default.WbSunny,
                                    label = "Sunlight Level",
                                    value = sunLevel,
                                    color = Color(0xFFDD6B20),
                                    trackColor = Color(0xFFE2E8F0),
                                    statusText = sunStatus
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val nutrientStatus = when {
                                    nutrientLevel > 0.7f -> "Rich"
                                    nutrientLevel > 0.4f -> "Sufficient"
                                    else -> "Needs Food"
                                }
                                CareProgressBar(
                                    icon = Icons.Default.Eco,
                                    label = "Nutrient Level",
                                    value = nutrientLevel,
                                    color = Color(0xFF319795),
                                    trackColor = Color(0xFFE2E8F0),
                                    statusText = nutrientStatus
                                )
                            }
                        }

                        // 3. Quick Log Actions
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Log Manual Care",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF386B4F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.waterPlant(plant.id)
                                    viewModel.addCareTask(
                                        com.example.data.model.CareTask(
                                            plantId = plant.id,
                                            plantName = plant.customName,
                                            taskType = "Watering",
                                            scheduledDate = System.currentTimeMillis(),
                                            notes = "Logged via Plant Profile Quick Action",
                                            isCompleted = true,
                                            completedDate = System.currentTimeMillis()
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEBF8FF), contentColor = Color(0xFF2B6CB0)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Water", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.addCareTask(
                                        com.example.data.model.CareTask(
                                            plantId = plant.id,
                                            plantName = plant.customName,
                                            taskType = "Sunlight/Rotation",
                                            scheduledDate = System.currentTimeMillis(),
                                            notes = "Logged via Plant Profile Quick Action",
                                            isCompleted = true,
                                            completedDate = System.currentTimeMillis()
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEEBC8), contentColor = Color(0xFFDD6B20)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sunlight", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.addCareTask(
                                        com.example.data.model.CareTask(
                                            plantId = plant.id,
                                            plantName = plant.customName,
                                            taskType = "Fertilizing",
                                            scheduledDate = System.currentTimeMillis(),
                                            notes = "Logged via Plant Profile Quick Action",
                                            isCompleted = true,
                                            completedDate = System.currentTimeMillis()
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6FFFA), contentColor = Color(0xFF319795)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nutrient", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

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
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                    data = android.provider.CalendarContract.Events.CONTENT_URI
                                    putExtra(android.provider.CalendarContract.Events.TITLE, "Watering: ${plant.customName}")
                                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Reminder to water ${plant.customName} (${plant.species}). It needs watering every ${plant.wateringIntervalDays} days.\n\n${plant.careInstructions}")
                                    putExtra(android.provider.CalendarContract.Events.RRULE, "FREQ=DAILY;INTERVAL=${plant.wateringIntervalDays}")
                                    putExtra(android.provider.CalendarContract.Events.AVAILABILITY, android.provider.CalendarContract.Events.AVAILABILITY_FREE)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("export_calendar_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F5EB), contentColor = Color(0xFF386B4F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Care Schedule to Calendar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showInstructionsDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Close Profile")
                        }
                    } else if (targetTab == "Journal") {
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
                    } else {
                        // Growth Tracker Tab
                        val growthMetrics by viewModel.getGrowthMetricsForPlant(plant.id).collectAsStateWithLifecycle(initialValue = emptyList())
                        var heightInput by remember { mutableStateOf("") }
                        var leafCountInput by remember { mutableStateOf("") }
                        var growthNoteInput by remember { mutableStateOf("") }

                        Column {
                            Text(
                                text = "Plant Growth Tracker",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log height, leaf count, and visual developments to monitor progress over time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Form to Add Growth Metric
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5EB)),
                                border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "New Growth Log",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF112611)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = heightInput,
                                            onValueChange = { heightInput = it },
                                            label = { Text("Height (cm)", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f).testTag("growth_height_input"),
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = TextStyle(fontSize = 12.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        OutlinedTextField(
                                            value = leafCountInput,
                                            onValueChange = { leafCountInput = it },
                                            label = { Text("Leaf Count", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f).testTag("growth_leaf_count_input"),
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = TextStyle(fontSize = 12.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = growthNoteInput,
                                        onValueChange = { growthNoteInput = it },
                                        label = { Text("Notes (e.g. Sprouted a new branch)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("growth_note_input"),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = TextStyle(fontSize = 12.sp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            val h = heightInput.toFloatOrNull() ?: 0f
                                            val l = leafCountInput.toIntOrNull() ?: 0
                                            viewModel.addGrowthMetric(plant.id, h, l, growthNoteInput)
                                            heightInput = ""
                                            leafCountInput = ""
                                            growthNoteInput = ""
                                        },
                                        enabled = heightInput.isNotBlank() || leafCountInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("growth_log_submit_button"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Save Growth Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Visualize with Line Chart
                            GrowthLineChart(
                                metrics = growthMetrics,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Growth Logs List
                            if (growthMetrics.isNotEmpty()) {
                                Text(
                                    text = "Growth History",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF112611)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                growthMetrics.sortedByDescending { it.timestamp }.forEach { metric ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Timeline,
                                                        contentDescription = null,
                                                        tint = Color(0xFF386B4F),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(metric.timestamp)),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF112611)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Height: ${metric.heightCm} cm • Leaves: ${metric.leafCount}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF5C6258)
                                                )
                                                if (metric.note.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = metric.note,
                                                        fontSize = 11.sp,
                                                        color = Color.Gray,
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteGrowthMetric(metric) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete entry",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
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

@Composable
fun PlantRadarChart(
    water: Float,
    sun: Float,
    nutrients: Float,
    modifier: Modifier = Modifier
) {
    // Smooth state animations for changing metrics
    val animatedWater by androidx.compose.animation.core.animateFloatAsState(
        targetValue = water,
        animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "WaterAnim"
    )
    val animatedSun by androidx.compose.animation.core.animateFloatAsState(
        targetValue = sun,
        animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "SunAnim"
    )
    val animatedNutrients by androidx.compose.animation.core.animateFloatAsState(
        targetValue = nutrients,
        animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "NutrientsAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val width = size.width
            val height = size.height
            val cx = width / 2f
            val cy = height / 2f
            val maxRadius = width / 2.2f

            val angleWater = -Math.PI / 2
            val angleSun = -Math.PI / 2 + 2 * Math.PI / 3
            val angleNutrients = -Math.PI / 2 + 4 * Math.PI / 3

            // Draw concentric web triangles (0.33, 0.66, 1.0)
            val webScales = listOf(0.33f, 0.66f, 1.0f)
            webScales.forEach { scale ->
                val r = maxRadius * scale
                val path = androidx.compose.ui.graphics.Path().apply {
                    val x0 = cx + r * kotlin.math.cos(angleWater).toFloat()
                    val y0 = cy + r * kotlin.math.sin(angleWater).toFloat()
                    moveTo(x0, y0)
                    
                    val x1 = cx + r * kotlin.math.cos(angleSun).toFloat()
                    val y1 = cy + r * kotlin.math.sin(angleSun).toFloat()
                    lineTo(x1, y1)

                    val x2 = cx + r * kotlin.math.cos(angleNutrients).toFloat()
                    val y2 = cy + r * kotlin.math.sin(angleNutrients).toFloat()
                    lineTo(x2, y2)

                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFFC2D1C7),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.dp.toPx()
                    )
                )
            }

            // Draw axes lines from center to outer web vertices
            val axesAngles = listOf(angleWater, angleSun, angleNutrients)
            axesAngles.forEach { angle ->
                val x = cx + maxRadius * kotlin.math.cos(angle).toFloat()
                val y = cy + maxRadius * kotlin.math.sin(angle).toFloat()
                drawLine(
                    color = Color(0xFFC2D1C7),
                    start = Offset(cx, cy),
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw data polygon
            val p0X = cx + maxRadius * animatedWater * kotlin.math.cos(angleWater).toFloat()
            val p0Y = cy + maxRadius * animatedWater * kotlin.math.sin(angleWater).toFloat()

            val p1X = cx + maxRadius * animatedSun * kotlin.math.cos(angleSun).toFloat()
            val p1Y = cy + maxRadius * animatedSun * kotlin.math.sin(angleSun).toFloat()

            val p2X = cx + maxRadius * animatedNutrients * kotlin.math.cos(angleNutrients).toFloat()
            val p2Y = cy + maxRadius * animatedNutrients * kotlin.math.sin(angleNutrients).toFloat()

            val dataPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(p0X, p0Y)
                lineTo(p1X, p1Y)
                lineTo(p2X, p2Y)
                close()
            }

            // Translucent primary-colored fill for the area
            drawPath(
                path = dataPath,
                color = Color(0x4D386B4F)
            )

            // Solid high-contrast green outline
            drawPath(
                path = dataPath,
                color = Color(0xFF386B4F),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx()
                )
            )

            // Small glowing node circular markers at data points
            drawCircle(color = Color(0xFF386B4F), radius = 5.dp.toPx(), center = Offset(p0X, p0Y))
            drawCircle(color = Color(0xFF386B4F), radius = 5.dp.toPx(), center = Offset(p1X, p1Y))
            drawCircle(color = Color(0xFF386B4F), radius = 5.dp.toPx(), center = Offset(p2X, p2Y))
        }

        // Draw clear, readable labels around the radar canvas
        Text(
            text = "💧 Water",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B3D2F),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
        )
        
        Text(
            text = "☀️ Sunlight",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B3D2F),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 6.dp, end = 6.dp)
        )

        Text(
            text = "🌱 Nutrient",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B3D2F),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 6.dp, start = 6.dp)
        )
    }
}

@Composable
fun CareProgressBar(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    color: Color,
    trackColor: Color,
    statusText: String
) {
    val animatedValue by androidx.compose.animation.core.animateFloatAsState(
        targetValue = value,
        animationSpec = androidx.compose.animation.core.tween(700),
        label = "ProgressBarAnim"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112611))
            }
            Text(
                text = "${(value * 100).toInt()}% ($statusText)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (value < 0.4f) Color(0xFFB3261E) else Color(0xFF386B4F)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedValue },
            color = color,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}


// --- SCREEN 2: PLANT IDENTIFICATION SCREEN ---
@Composable
fun IdentifyScreen(viewModel: GardenViewModel, onAddPlantDirect: (Plant) -> Unit) {
    val identificationResult by viewModel.identificationResult.collectAsStateWithLifecycle()
    val isIdentifying by viewModel.isIdentifying.collectAsStateWithLifecycle()
    val loadingMessage by viewModel.loadingMessage.collectAsStateWithLifecycle()
    val identificationsHistory by viewModel.identificationsHistory.collectAsStateWithLifecycle()

    var customNameInput by remember { mutableStateOf("") }
    var addedDateInput by remember { mutableStateOf(System.currentTimeMillis()) }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    
    var isLiveCameraActive by remember { mutableStateOf(false) }
    val imageCapture = remember { androidx.camera.core.ImageCapture.Builder().build() }
    var isCapturingByCameraX by remember { mutableStateOf(false) }

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
                isLiveCameraActive = true
            } else {
                Toast.makeText(context, "Camera permission is required to take a plant photo.", Toast.LENGTH_LONG).show()
                // Fallback to system camera preview
                cameraLauncher.launch(null)
            }
        }
    )

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

        // Beautiful unified image selection card with live viewfinder support
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5EB)),
            border = BorderStroke(1.dp, Color(0xFFE0E3DA)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(300.dp)
                .testTag("identify_image_container_card")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLiveCameraActive) {
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
                                .size(180.dp)
                                .align(Alignment.Center)
                                .border(2.dp, Color(0xFFFAE4D4).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        ) {
                            Text(
                                text = "Align plant leaf",
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
                                .offset(y = (300 * scanProgress).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF386B4F),
                                            Color(0xFFFAE4D4),
                                            Color(0xFF386B4F),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Control overlay: Capture and Cancel buttons
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cancel button
                            IconButton(
                                onClick = { isLiveCameraActive = false },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(44.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                            }

                            // Capture button
                            FloatingActionButton(
                                onClick = {
                                    if (!isCapturingByCameraX) {
                                        isCapturingByCameraX = true
                                        captureImage(
                                            context = context,
                                            imageCapture = imageCapture,
                                            onSuccess = { bitmap ->
                                                selectedBitmap = bitmap
                                                selectedUri = null
                                                isLiveCameraActive = false
                                                isCapturingByCameraX = false
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Failed to capture image: ${err.message}", Toast.LENGTH_LONG).show()
                                                isCapturingByCameraX = false
                                            }
                                        )
                                    }
                                },
                                containerColor = Color(0xFF386B4F),
                                contentColor = Color.White,
                                modifier = Modifier.size(56.dp)
                            ) {
                                if (isCapturingByCameraX) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(28.dp))
                                }
                            }

                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                } else {
                    val currentBitmap = selectedBitmap
                    if (currentBitmap != null) {
                        // Display selected/captured photo
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "Captured Plant Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Overlay a "Clear/Retake" action button at the top right
                            IconButton(
                                onClick = {
                                    selectedBitmap = null
                                    selectedUri = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(36.dp)
                                    .testTag("clear_selected_image")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        // If no image is selected yet, offer two clear modern options: Take Photo or Import Photo
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = "Plant",
                                tint = Color(0xFF386B4F),
                                modifier = Modifier.size(56.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Add a Plant Photo to Identify",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF112611)
                            )
                            
                            Text(
                                text = "Capture with your camera or select from your gallery",
                                fontSize = 13.sp,
                                color = Color(0xFF5C6258),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // "Take Photo" button
                                Button(
                                    onClick = {
                                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                        
                                        if (hasCameraPermission) {
                                            isLiveCameraActive = true
                                        } else {
                                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(48.dp)
                                        .testTag("take_photo_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Camera Icon",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Take Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                // "Gallery" button
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF386B4F)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF386B4F)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("select_from_gallery_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = "Gallery Icon",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
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
                    if (selectedBitmap != null) {
                        viewModel.identifyPlant(selectedBitmap, descriptionInput)
                    } else {
                        Toast.makeText(context, "Please take a photo or select an image from gallery first.", Toast.LENGTH_LONG).show()
                    }
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
                    Text(
                        text = loadingMessage ?: "Identifying with PlantAID...",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
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
                                    result.familyName?.let { familyName ->
                                        Text(
                                            text = "Family: $familyName",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
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

                            val refImages = remember(result.detectedName, result.referenceImages) {
                                val list = result.referenceImages?.filterNot { it.contains("example.com") } ?: emptyList()
                                if (list.isEmpty()) {
                                    getReferenceImagesForPlant(result.detectedName)
                                } else {
                                    list
                                }
                            }

                            var selectedLightboxImageUrl by remember { mutableStateOf<String?>(null) }

                            if (selectedLightboxImageUrl != null) {
                                PlantImageLightbox(
                                    imageUrl = selectedLightboxImageUrl!!,
                                    onDismiss = { selectedLightboxImageUrl = null }
                                )
                            }

                            // High-fidelity accuracy comparison panel showing Captured image vs Botanical Reference image
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Match Comparison (Accuracy Verification)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Captured Photo Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (selectedBitmap != null) {
                                            Image(
                                                bitmap = selectedBitmap!!.asImageBitmap(),
                                                contentDescription = "Your Captured Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Spa, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        ) {
                                            Text(
                                                text = "Your Scan",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Reference Image Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (refImages.isNotEmpty()) {
                                                    selectedLightboxImageUrl = refImages.first()
                                                }
                                            }
                                    ) {
                                        if (refImages.isNotEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = refImages.first(),
                                                contentDescription = "Botanical Reference",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Spa, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                            shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        ) {
                                            Text(
                                                text = "Reference",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (refImages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Reference Images",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    refImages.forEachIndexed { index, imageUrl ->
                                        coil.compose.AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "Reference Image",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { selectedLightboxImageUrl = imageUrl }
                                                .testTag("ref_image_thumbnail_$index")
                                        )
                                    }
                                }
                            }

                            if (!result.remediesForGrowth.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Growth Remedies & Tips",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                result.remediesForGrowth.forEach { remedy ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Eco, null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(remedy, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            // 1. Retrieve the plant’s profile from the database lookup
                            val gardenPlants by viewModel.plants.collectAsStateWithLifecycle()
                            val matchingProfile = remember(result.detectedName, result.scientificName, gardenPlants) {
                                // Search garden plants
                                val gardenMatch = gardenPlants.firstOrNull {
                                    it.species.equals(result.detectedName, ignoreCase = true) ||
                                    it.species.equals(result.scientificName, ignoreCase = true) ||
                                    it.name.equals(result.detectedName, ignoreCase = true)
                                }
                                if (gardenMatch != null) {
                                    Triple("My Garden Profile", gardenMatch.species, "• Sunlight: ${gardenMatch.sunlight}\n• Watering Interval: Every ${gardenMatch.wateringIntervalDays} days\n• Location: ${gardenMatch.location}\n• Care Log: ${gardenMatch.careInstructions}")
                                } else {
                                    // Search static catalog database
                                    val catalogMatch = listOf(
                                        "Fiddle Leaf Fig", "Monstera Deliciosa", "Aloe Vera", 
                                        "English Lavender", "Snake Plant", "Peace Lily", 
                                        "Golden Pothos", "Jade Plant", "Spider Plant"
                                    ).firstOrNull {
                                        it.equals(result.detectedName, ignoreCase = true) ||
                                        it.contains(result.detectedName, ignoreCase = true) ||
                                        result.detectedName.contains(it, ignoreCase = true)
                                    }
                                    if (catalogMatch != null) {
                                        val details = when (catalogMatch) {
                                            "Fiddle Leaf Fig" -> "• Sunlight: Bright indirect sunlight. Rotate regularly.\n• Watering: Water only when top 2 inches feel dry.\n• Soil: Well-draining peat-based soil.\n• Fertilizer: Apply general houseplant fertilizer once a month in spring/summer."
                                            "Monstera Deliciosa" -> "• Sunlight: Medium to bright indirect. Avoid hot direct sun.\n• Watering: Every 7-10 days. Allow soil to dry out between drinks.\n• Soil: Chunky, well-aerated soil mix with organic bark.\n• Fertilizer: Apply balanced liquid feed every 4 weeks in growing season."
                                            "Aloe Vera" -> "• Sunlight: Bright direct sunlight. Great for sunny sills.\n• Watering: Sparingly. Every 3 weeks. Prone to overwatering.\n• Soil: Sandy, fast-draining cactus mix.\n• Fertilizer: Feed once in early spring with succulent fertilizer."
                                            "English Lavender" -> "• Sunlight: Full, hot, direct sun. Needs 6+ hours daily.\n• Watering: Low moisture. Allow soil to dry out completely.\n• Soil: Lean, rocky, highly porous alkaline soil.\n• Fertilizer: Avoid heavy feeding; needs minimal nutrients."
                                            "Snake Plant" -> "• Sunlight: Extremely adaptable. Low, medium, or bright light.\n• Watering: Very low watering. Every 3-4 weeks. Let dry completely.\n• Soil: Loose, well-aerated sandy cactus substrate.\n• Fertilizer: Feed once or twice in spring with diluted foliage fertilizer."
                                            "Peace Lily" -> "• Sunlight: Low to medium indirect light. Tolerates shade.\n• Watering: Keep soil evenly moist. Will droop dramatically when dry.\n• Soil: Rich, organic-rich moisture-retaining soil mix.\n• Fertilizer: Feed every 6 weeks with mild water-soluble organic food."
                                            "Golden Pothos" -> "• Sunlight: Adaptable. Thrives in bright indirect, tolerates low light.\n• Watering: Let top 50% dry out. Water when leaves look slightly soft.\n• Soil: General high-quality multipurpose potting soil.\n• Fertilizer: Apply liquid food every 2 months in spring/summer."
                                            "Jade Plant" -> "• Sunlight: Bright direct or indirect light. Needs hours of sun.\n• Watering: Water thoroughly when leaves feel slightly soft to gentle squeeze.\n• Soil: Well-draining organic succulent mix.\n• Fertilizer: Feed with low-nitrogen liquid fertilizer once a month in summer."
                                            else -> "• Sunlight: Bright indirect to partial shade.\n• Watering: Keep moist but never waterlogged.\n• Soil: Well-draining organic loam.\n• Fertilizer: Apply balanced organic compost once a season."
                                        }
                                        Triple("Predefined Species Catalog", catalogMatch, details)
                                    } else {
                                        null
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Show local database profile match status if found
                            if (matchingProfile != null) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EDE4)),
                                    border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = Color(0xFF234B34),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Retrieved Database Profile",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF234B34)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Verified match in '${matchingProfile.first}' for species: ${matchingProfile.second}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF414941)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = matchingProfile.third,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = Color(0xFF1F2B22)
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Species profile not in local database. Falling back to AI Expert Care Plan.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 2. Structured Care Plan (Watering, Sunlight, Soil, Fertilizer)
                            Text(
                                text = "Care Plan Guide",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                GuideCard(
                                    icon = Icons.Outlined.LightMode,
                                    title = "Sunlight Requirement",
                                    desc = result.lightGuide
                                )
                                GuideCard(
                                    icon = Icons.Outlined.WaterDrop,
                                    title = "Watering Needs",
                                    desc = result.waterGuide
                                )
                                GuideCard(
                                    icon = Icons.Outlined.Terrain,
                                    title = "Soil & Potting",
                                    desc = result.soilGuide
                                )
                                GuideCard(
                                    icon = Icons.Default.LocalFlorist,
                                    title = "Fertilizer Guide",
                                    desc = result.fertilizerGuide ?: "Apply balanced organic or liquid foliage fertilizer monthly during the spring and summer growing season. Avoid feeding in winter."
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 3. Health Diagnosis, Pests & Deficiency Analysis Section
                            Text(
                                text = "Image Health Report",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (result.hasIssue == true && !result.diseaseName.isNullOrBlank() && result.diseaseName != "None") {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Diagnosis: ${result.diseaseName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = "Issue Detected with ~${((result.confidence - 0.05f).coerceIn(0.7f, 0.99f) * 100).toInt()}% confidence",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Observed Symptoms:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = result.symptoms ?: "Foliage issues detected in scanned image.",
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Healing,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Recommended Treatments:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val steps = result.treatmentSteps ?: listOf("Isolate the plant to prevent spreading.", "Trim affected foliage using clean shears.", "Adjust water/sunlight as per care plan guidelines.")
                                        steps.forEachIndexed { idx, step ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "${idx + 1}.",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.width(18.dp)
                                                )
                                                Text(
                                                    text = step,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        if (!result.homeRemedies.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Home,
                                                    contentDescription = null,
                                                    tint = Color(0xFF7A5901),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Home Remedies & DIY Solutions:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF7A5901)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            result.homeRemedies.forEach { remedy ->
                                                Row(
                                                    modifier = Modifier.padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = "•",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF7A5901),
                                                        modifier = Modifier.width(12.dp)
                                                    )
                                                    Text(
                                                        text = remedy,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFF1E5235),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Preventive Tips:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1E5235)
                                            )
                                        }
                                        Text(
                                            text = result.prevention ?: "Ensure adequate aeration and soil dryness before watering.",
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    border = BorderStroke(1.5.dp, Color(0xFF81C784)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFFC8E6C9), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Health Status: Healthy",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                                Text(
                                                    text = "No signs of disease, pests, or deficiencies",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF43A047)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Preventive & Wellness Tips:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            text = result.prevention ?: "Keep the foliage clean by wiping leaves with a damp cloth occasionally. Maintain the care plan watering frequency and check soil moisture before adding water.",
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

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

                            Text(
                                text = "Date Added",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5C6258).copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                            OutlinedButton(
                                onClick = {
                                    val calendar = Calendar.getInstance().apply { timeInMillis = addedDateInput }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            }
                                            addedDateInput = selectedCal.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF386B4F)),
                                border = BorderStroke(1.dp, Color(0xFFE0E3DA))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateFormatter.format(Date(addedDateInput)),
                                        color = Color(0xFF112611),
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Choose Date Added",
                                        tint = Color(0xFF386B4F)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedBitmap = null
                                        selectedUri = null
                                        viewModel.clearIdentificationResult()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("retake_identification_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retake")
                                }

                                Button(
                                    onClick = {
                                        onAddPlantDirect(
                                            Plant(
                                                name = result.detectedName,
                                                customName = customNameInput.ifBlank { result.detectedName },
                                                species = result.scientificName,
                                                wateringIntervalDays = 7, // Default
                                                sunlight = result.lightGuide.take(30) + "...",
                                                careInstructions = "Light: ${result.lightGuide}\n\nWater: ${result.waterGuide}\n\nSoil: ${result.soilGuide}",
                                                addedDate = addedDateInput
                                            )
                                        )
                                        // Reset inputs
                                        customNameInput = ""
                                        addedDateInput = System.currentTimeMillis()
                                        viewModel.clearIdentificationResult()
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(50.dp)
                                        .testTag("add_to_garden_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Garden")
                                }
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
    val loadingMessage by viewModel.loadingMessage.collectAsStateWithLifecycle()
    val diagnosesHistory by viewModel.diagnosesHistory.collectAsStateWithLifecycle()

    var symptomsInput by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

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
                Toast.makeText(context, "Camera permission is required to take a plant photo.", Toast.LENGTH_LONG).show()
            }
        }
    )

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

        // Beautiful unified image selection card (no live scanner)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAE4D4).copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, Color(0xFFF3CBB5)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(300.dp)
                .testTag("diagnose_image_container_card")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = selectedBitmap
                if (currentBitmap != null) {
                    // Display selected/captured photo
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Captured Sick Leaf Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Overlay a "Clear/Retake" action button at the top right
                        IconButton(
                            onClick = {
                                selectedBitmap = null
                                selectedUri = null
                                viewModel.clearDiagnosisResult()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(36.dp)
                                .testTag("clear_diagnose_image")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Offer Take Photo or Import Photo options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Coronavirus,
                            contentDescription = "Sick Leaf",
                            tint = Color(0xFF855428),
                            modifier = Modifier.size(56.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Add a Leaf Photo to Diagnose",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF112611)
                        )
                        
                        Text(
                            text = "Take a photo of brown spots, wilted or yellowing leaves to analyze",
                            fontSize = 13.sp,
                            color = Color(0xFF5C6258),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // "Take Photo" button
                            Button(
                                onClick = {
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (hasCameraPermission) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF855428)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("diagnose_take_photo_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera Icon",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Take Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            // "Gallery" button
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                border = BorderStroke(1.dp, Color(0xFF855428)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF855428)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("diagnose_gallery_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery Icon",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            var hasCameraPermission by remember { mutableStateOf(false) }
            val imageCapture = remember { androidx.camera.core.ImageCapture.Builder().build() }
            var isCapturing by remember { mutableStateOf(false) }
            val showLiveScannerPlaceholder = false
            if (showLiveScannerPlaceholder) {
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
                        val currentBitmap = selectedBitmap
                        if (currentBitmap != null) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
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

            val showLiveScannerButtonPlaceholder = false
            if (showLiveScannerButtonPlaceholder) {
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
                        Text(
                            text = if (isCapturing) "Capturing image..." else loadingMessage ?: "Analyzing Live Plant...",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
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
                        Text(
                            text = loadingMessage ?: "Analyzing Disease symptoms...",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
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
    val loadingMessage by viewModel.loadingMessage.collectAsStateWithLifecycle()
    val careTasks by viewModel.careTasks.collectAsStateWithLifecycle()

    var activeMainTab by remember { mutableStateOf("Plant Care") } // "Plant Care", "Remedy"
    var activeTab by remember { mutableStateOf("Care Planner") } // "Care Planner", "Plant Database", "Lacking Checker", "Weather Advisor"
    var activeRemedyTab by remember { mutableStateOf("Household Fertilizers") } // "Household Fertilizers", "Home Remedies"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Natural Tones Spacious Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "GARDEN ASSISTANT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF414941)
                )
                Text(
                    text = when (activeMainTab) {
                        "Plant Care" -> when (activeTab) {
                            "Care Planner" -> "Care Planner"
                            "Plant Database" -> "Plant Database"
                            "Lacking Checker" -> "Lacking Checker"
                            else -> "Weather Advisor"
                        }
                        else -> activeRemedyTab
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF112611)
                )
            }
            // Elegant circle container
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
                    imageVector = when (activeMainTab) {
                        "Plant Care" -> when (activeTab) {
                            "Care Planner" -> Icons.Default.CalendarMonth
                            "Plant Database" -> Icons.Default.MenuBook
                            "Lacking Checker" -> Icons.Default.FactCheck
                            else -> Icons.Default.CloudQueue
                        }
                        else -> if (activeRemedyTab == "Household Fertilizers") Icons.Default.LocalFlorist else Icons.Default.Spa
                    },
                    contentDescription = "Active Tab Icon",
                    tint = Color(0xFF386B4F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Top-level main tab selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(Color(0xFFE9EFEA), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Plant Care", "Remedy").forEach { mainTab ->
                val isSelected = activeMainTab == mainTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF386B4F) else Color.Transparent)
                        .clickable { activeMainTab = mainTab }
                        .padding(vertical = 10.dp)
                        .testTag(
                            when (mainTab) {
                                "Plant Care" -> "tab_plant_care"
                                else -> "tab_remedy"
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mainTab,
                        color = if (isSelected) Color.White else Color(0xFF386B4F),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        if (activeMainTab == "Plant Care") {
            // Beautiful Sub-Tab Selector Row for Plant Care
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(Color(0xFFF1F6F2), RoundedCornerShape(12.dp))
                    .padding(3.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Care Planner", "Plant Database", "Lacking Checker", "Weather Advisor").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF5C8E72) else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag(
                                when (tab) {
                                    "Care Planner" -> "subtab_care_planner"
                                    "Plant Database" -> "subtab_plant_database"
                                    "Lacking Checker" -> "subtab_lacking_checker"
                                    else -> "subtab_weather_advisor"
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.White else Color(0xFF386B4F),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Beautiful Sub-Tab Selector Row for Remedy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(Color(0xFFF1F6F2), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Household Fertilizers", "Home Remedies").forEach { rTab ->
                    val isSelected = activeRemedyTab == rTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF5C8E72) else Color.Transparent)
                            .clickable { activeRemedyTab = rTab }
                            .padding(vertical = 8.dp)
                            .testTag(if (rTab == "Household Fertilizers") "subtab_household_fertilizers" else "subtab_home_remedies"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rTab,
                            color = if (isSelected) Color.White else Color(0xFF386B4F),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Tab Contents
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = activeMainTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "CareGuideMainTabAnimation"
            ) { targetMainTab ->
                when (targetMainTab) {
                    "Plant Care" -> {
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                            },
                            label = "CareGuideSubTabAnimation"
                        ) { targetSubTab ->
                            when (targetSubTab) {
                                "Care Planner" -> CarePlannerTab(
                                    viewModel = viewModel,
                                    careTasks = careTasks,
                                    carePlanResult = carePlanResult,
                                    isGeneratingCarePlan = isGeneratingCarePlan,
                                    loadingMessage = loadingMessage
                                )
                                "Plant Database" -> PlantDatabaseTab()
                                "Lacking Checker" -> LackingCheckerTab(viewModel = viewModel, loadingMessage = loadingMessage)
                                "Weather Advisor" -> WeatherAdvisorTab(viewModel = viewModel, loadingMessage = loadingMessage)
                            }
                        }
                    }
                    "Remedy" -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (activeRemedyTab == "Household Fertilizers") {
                                EasyHouseholdFertilizersTab()
                            } else {
                                HomeRemediesTab()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarePlannerTab(
    viewModel: GardenViewModel,
    careTasks: List<com.example.data.model.CareTask>,
    carePlanResult: com.example.api.dto.PersonalizedCarePlanDTO?,
    isGeneratingCarePlan: Boolean,
    loadingMessage: String?
) {
    var plantSearchInput by remember { mutableStateOf("") }
    var showAddTaskForm by remember { mutableStateOf(false) }

    // Task addition form state
    var taskPlantName by remember { mutableStateOf("") }
    var selectedTaskType by remember { mutableStateOf("Watering") }
    val taskTypes = listOf("Watering", "Fertilizing", "Sunlight/Rotation", "Vitamins", "Medicine/Pesticides", "Pruning")
    var taskNotes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Active Checklist section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Care Planner",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF112611)
                        )
                        Text(
                            text = "Schedule and check off tasks to keep plants thriving.",
                            fontSize = 12.sp,
                            color = Color(0xFF5C6258)
                        )
                    }
                    IconButton(
                        onClick = { showAddTaskForm = !showAddTaskForm }
                    ) {
                        Icon(
                            imageVector = if (showAddTaskForm) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "Add Task",
                            tint = Color(0xFF386B4F),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (showAddTaskForm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Add Scheduled Care Task", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF112611))
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskPlantName,
                        onValueChange = { taskPlantName = it },
                        label = { Text("Plant Name / Nickname") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Care Action Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF386B4F))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        taskTypes.forEach { type ->
                            val isTypeSelected = selectedTaskType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isTypeSelected) Color(0xFF386B4F) else Color(0xFFE2EDE4))
                                    .clickable { selectedTaskType = type }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 12.sp,
                                    color = if (isTypeSelected) Color.White else Color(0xFF386B4F),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskNotes,
                        onValueChange = { taskNotes = it },
                        label = { Text("Care Notes (e.g. 2 cups, spray under leaves)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (taskPlantName.isNotBlank()) {
                                viewModel.addCareTask(
                                    com.example.data.model.CareTask(
                                        plantName = taskPlantName,
                                        taskType = selectedTaskType,
                                        scheduledDate = System.currentTimeMillis(),
                                        notes = taskNotes
                                    )
                                )
                                taskPlantName = ""
                                taskNotes = ""
                                showAddTaskForm = false
                            }
                        },
                        enabled = taskPlantName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Task to Planner")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (careTasks.isEmpty()) {
                    Text(
                        text = "No upcoming tasks! Your leafy friends are fully content.",
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF5C6258),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    careTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val taskIcon = when (task.taskType) {
                                    "Watering" -> Icons.Default.WaterDrop
                                    "Fertilizing" -> Icons.Default.Grass
                                    "Sunlight/Rotation" -> Icons.Default.LightMode
                                    "Vitamins" -> Icons.Default.Spa
                                    "Medicine/Pesticides" -> Icons.Default.Healing
                                    else -> Icons.Default.Build
                                }

                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.toggleCareTask(task) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF386B4F))
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Column {
                                    Text(
                                        text = task.plantName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (task.isCompleted) Color.Gray else Color(0xFF112611),
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(taskIcon, null, tint = Color(0xFF386B4F), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = task.taskType,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF386B4F)
                                        )
                                    }
                                    if (task.notes.isNotBlank()) {
                                        Text(
                                            text = task.notes,
                                            fontSize = 12.sp,
                                            color = Color(0xFF5C6258)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteCareTask(task) }
                            ) {
                                Icon(Icons.Default.DeleteOutline, "Delete Task", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Existing Core Care Guide generator
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
                Text(
                    text = loadingMessage ?: "Formulating Guide...",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
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

        // Live Seasonal Section (Preserved!)
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
        
        fun getAutoDetectedSeason(): String {
            val calendar = java.util.Calendar.getInstance()
            val month = calendar.get(java.util.Calendar.MONTH)
            val monthName = when (month) {
                java.util.Calendar.JANUARY -> "January"
                java.util.Calendar.FEBRUARY -> "February"
                java.util.Calendar.MARCH -> "March"
                java.util.Calendar.APRIL -> "April"
                java.util.Calendar.MAY -> "May"
                java.util.Calendar.JUNE -> "June"
                java.util.Calendar.JULY -> "July"
                java.util.Calendar.AUGUST -> "August"
                java.util.Calendar.SEPTEMBER -> "September"
                java.util.Calendar.OCTOBER -> "October"
                java.util.Calendar.NOVEMBER -> "November"
                java.util.Calendar.DECEMBER -> "December"
                else -> "Current Month"
            }
            
            val seasonName = when (month) {
                java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY -> "Winter"
                java.util.Calendar.MARCH, java.util.Calendar.APRIL, java.util.Calendar.MAY -> "Spring"
                java.util.Calendar.JUNE, java.util.Calendar.JULY, java.util.Calendar.AUGUST -> "Summer"
                java.util.Calendar.SEPTEMBER, java.util.Calendar.OCTOBER, java.util.Calendar.NOVEMBER -> "Autumn"
                else -> "Current Season"
            }
            return "$seasonName ($monthName)"
        }

        var seasonInput by remember { mutableStateOf(getAutoDetectedSeason()) }
        val context = LocalContext.current
        var isLocating by remember { mutableStateOf(false) }
        var locationErrorMsg by remember { mutableStateOf<String?>(null) }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                isLocating = true
                detectLocation(
                    context = context,
                    onSuccess = { detectedLoc ->
                        locationInput = detectedLoc
                        locationErrorMsg = null
                        isLocating = false
                    },
                    onError = { error ->
                        locationErrorMsg = error
                        isLocating = false
                    }
                )
            } else {
                locationErrorMsg = "Location permissions denied. Please enter your location manually or enable permissions in App Settings."
            }
        }

        LaunchedEffect(Unit) {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val coarseGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (fineGranted || coarseGranted) {
                isLocating = true
                detectLocation(
                    context = context,
                    onSuccess = { detectedLoc ->
                        locationInput = detectedLoc
                        locationErrorMsg = null
                        isLocating = false
                    },
                    onError = { error ->
                        isLocating = false
                    }
                )
            }
        }

        OutlinedTextField(
            value = locationInput,
            onValueChange = { 
                locationInput = it
                locationErrorMsg = null
            },
            label = { Text("Your Location / Region") },
            placeholder = { Text("E.g., Seattle, WA or London, UK") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("seasonal_location_input"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF386B4F))
            },
            trailingIcon = {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF386B4F)
                    )
                } else {
                    IconButton(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                           ) == PackageManager.PERMISSION_GRANTED
                            
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                isLocating = true
                                detectLocation(
                                    context = context,
                                    onSuccess = { detectedLoc ->
                                        locationInput = detectedLoc
                                        locationErrorMsg = null
                                        isLocating = false
                                    },
                                    onError = { error ->
                                        locationErrorMsg = error
                                        isLocating = false
                                    }
                                )
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Auto-detect location",
                            tint = Color(0xFF386B4F)
                        )
                    }
                }
            }
        )

        locationErrorMsg?.let { error ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

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
                Text(
                    text = loadingMessage ?: "Searching Google & Analyzing...",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
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

data class HomeRemedy(
    val title: String,
    val category: String,
    val summary: String,
    val instructions: String
)

val homeRemediesDatabase = listOf(
    HomeRemedy(
        title = "Coffee Grounds",
        category = "Fertilizer / Nitrogen Boost",
        summary = "Rich in nitrogen, which is excellent for leafy green growth. Sprinkle a light layer directly onto the soil, or steep used grounds in water (1:3 ratio) for a few weeks to create a liquid feed.",
        instructions = "Sprinkle a light layer directly onto the soil, or steep used grounds in water (1:3 ratio) for a few weeks to create a liquid feed.\n\nBest for: Roses, tomatoes, and blueberries."
    ),
    HomeRemedy(
        title = "Banana Peel Water",
        category = "Flowering & Root Stimulator",
        summary = "Bananas are packed with potassium and phosphorus.",
        instructions = "Chop peels and let them soak in water for 2-3 days. Strain the liquid and use it to water your plants to encourage flowering and robust root systems."
    ),
    HomeRemedy(
        title = "Crushed Eggshells",
        category = "Calcium Supplement",
        summary = "Provides essential calcium, which prevents blossom end rot and builds strong cell walls.",
        instructions = "Bake, dry, and crush the shells as finely as possible before working them directly into the soil."
    ),
    HomeRemedy(
        title = "Weed or Plant Clipping Tea",
        category = "Nutrient-Dense Liquid Feed",
        summary = "Gather healthy weeds or yard clippings, submerge them in a bucket of rainwater, and cover loosely for 2 weeks.",
        instructions = "Gather healthy weeds or yard clippings, submerge them in a bucket of rainwater, and cover loosely for 2 weeks. Dilute this nutrient-dense liquid (usually 1 part tea to 5 parts water) to drench the soil around your plants."
    ),
    HomeRemedy(
        title = "Honey and Shallot Water",
        category = "Root Development Tonic",
        summary = "Shallots are packed with natural minerals and sulfur compounds that promote root development.",
        instructions = "Chop shallots, steep them in hot water, and add a spoonful of honey (which offers quick energy). Dilute with clean water before applying to the soil."
    ),
    HomeRemedy(
        title = "Neem Leaf Tea",
        category = "Natural Pesticide & Repellent",
        summary = "Neem (Azadirachta indica) is India’s go-to natural pesticide. Its seeds contain azadirachtin, a compound that reduces insect feeding and disrupts their hormone systems.",
        instructions = "To make neem tea:\n1. Soak 1 kg of fresh neem leaves in 5 litres of water overnight.\n2. Grind and strain the mixture.\n3. Dilute the extract 1:10 with water and spray onto affected plants.\n\nThis spray repels sucking pests like mealybugs and aphids. MAX Plant Pest Control contains neem extracts for ready made convenience."
    ),
    HomeRemedy(
        title = "Chilli Garlic Ginger Spray",
        category = "Insect & Caterpillar Deterrent",
        summary = "Chillies, garlic, and ginger contain natural compounds that deter insects.",
        instructions = "The Urban Gardeners recommends blending one chilli, a clove of garlic, and a small piece of ginger into a paste, squeezing out the liquid and diluting a few drops in one litre of water.\n\nSpray lightly on foliage; excessive chilli can burn leaves. This mixture is especially effective against aphids and caterpillars."
    ),
    HomeRemedy(
        title = "Garlic Pepper Olive Oil Spray",
        category = "Aphid & Mite Smotherer",
        summary = "Combine crushed garlic, ground pepper, and olive oil, then dilute the mixture with water (1:10) and spray to deter aphids and mites.",
        instructions = "Combine crushed garlic, ground pepper, and olive oil, then dilute the mixture with water (1:10) and spray to deter aphids and mites. The oil smothers small insects, while pepper irritates them."
    ),
    HomeRemedy(
        title = "Turmeric Dusting",
        category = "Bactericidal & Antifungal Powder",
        summary = "Turmeric powder has bactericidal and antifungal properties.",
        instructions = "Sprinkle a pinch on leaves or soil to protect plants from ants and flies."
    ),
    HomeRemedy(
        title = "Coffee Grounds for Fungus Control",
        category = "Fungicide & Mulch",
        summary = "Mix spent coffee grounds into your potting mix as mulch. Coffee slightly acidifies the soil and discourages fungal growth.",
        instructions = "Mix spent coffee grounds into your potting mix as mulch. Coffee slightly acidifies the soil and discourages fungal growth. It decomposes slowly, enriching the soil while keeping gnats at bay."
    ),
    HomeRemedy(
        title = "Neem Oil Spray",
        category = "Broad-Spectrum Pest Management",
        summary = "For convenience, many gardeners prefer neem oil over neem leaf tea. Neem oil is extracted from seeds and contains azadirachtin and other insecticidal compounds.",
        instructions = "Neem oil reduces insect feeding, acts as a repellent, and interferes with insect growth.\n\nDilute according to product directions—typically 5 ml oil with 2 ml mild soap as an emulsifier per litre of water. Spray on leaves, underside, and stems weekly until pests disappear.\n\nAvoid applying during intense sunlight. Mealy Guard – Natural Plant Protection Spray contains neem oil and herbal extracts, offering a mess free solution."
    ),
    HomeRemedy(
        title = "Soap Water Boost",
        category = "Efficacy Multiplier",
        summary = "Adding 10 g of mild soap (preferably non detergent) to any homemade spray increases its efficacy.",
        instructions = "Soap breaks down waxy coatings and helps the mixture stick to insects. However, the gardening expert warns against mixing household soaps yourself because some detergents can burn leaves. Use castile soap or a ready made insecticidal soap instead."
    ),
    HomeRemedy(
        title = "Tobacco Leaf Soak",
        category = "Nicotine Pest Control (Ornamentals Only)",
        summary = "Soak 100 g of dried tobacco leaves in 4 litres of water overnight, grind and filter the extract and spray it on ornamental plants.",
        instructions = "Tobacco contains nicotine, which is toxic to many insects. Avoid using it on edible plants or solanaceous vegetables (tomatoes, peppers, brinjal, capsicum) because nicotine can harm them."
    ),
    HomeRemedy(
        title = "Custard Apple Seed Powder",
        category = "Soil-Borne Pest Control",
        summary = "Powdered custard apple (sitaphal) seeds can be sprinkled on soil or made into a paste and applied around stems.",
        instructions = "The seeds have strong insecticidal properties and are effective against soil borne pests like grubs."
    )
)

// --- SUB-TAB 2: PLANT DATABASE ---
@Composable
fun PlantDatabaseTab() {
    var dbSection by remember { mutableStateOf("Species Catalog") } // "Species Catalog", "Diseases & Remedies", "Home Remedies"
    var dbQuery by remember { mutableStateOf("") }

    val speciesDatabase = listOf(
        Pair("Fiddle Leaf Fig", "Ficus lyrata"),
        Pair("Monstera Deliciosa", "Monstera deliciosa"),
        Pair("Aloe Vera", "Aloe barbadensis"),
        Pair("English Lavender", "Lavandula angustifolia"),
        Pair("Snake Plant", "Sansevieria trifasciata"),
        Pair("Peace Lily", "Spathiphyllum wallisii"),
        Pair("Golden Pothos", "Epipremnum aureum"),
        Pair("Jade Plant", "Crassula ovata"),
        Pair("Spider Plant", "Chlorophytum comosum")
    ).map { (name, sciName) ->
        val description = when (name) {
            "Fiddle Leaf Fig" -> "A dramatic interior tree characterized by broad, violin-shaped green leaves with strong prominent veins."
            "Monstera Deliciosa" -> "Iconic tropical climber featuring split leaves and beautiful visual fenestrations. Prefers aerial support."
            "Aloe Vera" -> "Stemless evergreen succulent valued for medicinal gel inside its thick, fleshy upward-pointing spear leaves."
            "English Lavender" -> "Fragrant Mediterranean herb producing lovely purple flower spikes. Highly aromatic and demands excellent drainage."
            "Snake Plant" -> "Sturdy, sculptural succulent with upright sword-like leaves banded in silver and yellow. Highly resilient to dark spaces."
            "Peace Lily" -> "Graceful foliage plant producing beautiful white spathe flowers. Excellent air filter and drops visibly when thirsty."
            "Golden Pothos" -> "Versatile, fast-growing heart-leaved trailing vine splashed in gold variegation. Nearly indestructible."
            "Jade Plant" -> "Branching succulent tree with thick, glossy spoon-shaped leaves. Symbolic of good fortune."
            else -> "Graceful arching clump forming plant with narrow green-and-white striped leaves sending out offsets on long runners."
        }

        val details = when (name) {
            "Fiddle Leaf Fig" -> "• Light: Bright indirect sunlight. Turn regularly.\n• Watering: Water thoroughly only when top 2 inches feel dry.\n• Soil: Well-draining peat-based mix.\n• Remedy/Tip: Wipe leaves with a damp cloth to clean dust."
            "Monstera Deliciosa" -> "• Light: Medium to bright indirect. Avoid harsh rays.\n• Watering: Every 7-10 days. Allow to dry out slightly.\n• Soil: Chunky well-aerated soil mix with orchid bark.\n• Remedy/Tip: Provide a moss pole to support upright climbing."
            "Aloe Vera" -> "• Light: Full, direct bright light. Ideal for sunny sills.\n• Watering: Sparingly. Every 3 weeks. Avoid standing water.\n• Soil: Sandy cactus or succulent potting soil.\n• Remedy/Tip: Soft leaves mean overwatering. Stop water immediately."
            "English Lavender" -> "• Light: Full, raw direct sun. Minimum 6 hours.\n• Watering: Low water needs. Let soil dry completely between drinks.\n• Soil: Lean, rocky, alkaline, highly porous drainage soil.\n• Remedy/Tip: Prune back after flowering to keep it compact."
            "Snake Plant" -> "• Light: Extremely tolerant. Thrives in low, indirect, or direct light.\n• Watering: Every 3-4 weeks. Very prone to rot if overwatered.\n• Soil: Loose cactus soil.\n• Remedy/Tip: Wrinkled leaves indicate underwatering."
            "Peace Lily" -> "• Light: Low to medium indirect shade. Avoid direct sun.\n• Watering: Keep soil evenly moist. Will droop when thirsty.\n• Soil: Rich, moisture-retentive but draining soil.\n• Remedy/Tip: Flush with distilled water to prevent brown tips."
            "Golden Pothos" -> "• Light: Adaptable to low light, but thrives in bright indirect.\n• Watering: Let dry out between waterings.\n• Soil: General multipurpose soil.\n• Remedy/Tip: Trim leggy stems to encourage bushier growth."
            "Jade Plant" -> "• Light: Bright direct or indirect light.\n• Watering: Water when leaves feel slightly soft to squeeze.\n• Soil: Fast-draining succulent substrate.\n• Remedy/Tip: Leaf drop means insufficient light or damp roots."
            else -> "• Light: Bright indirect to partial shade.\n• Watering: Keep moist but not waterlogged.\n• Soil: Well-draining loam.\n• Remedy/Tip: Pet-friendly! Perfect choice for homes with furry friends."
        }
        Triple(name, sciName, Pair(description, details))
    }

    val diseasesDatabase = listOf(
        "Root Rot (Fungal Infestation)" to Triple(
            "Waterlogged soil causing root death.",
            "Yellowing lower leaves, mushy stems, musty wet soil smell, dropping foliage.",
            "Stop watering immediately. Remove plant from pot, trim rotten black mushy roots, spray roots with copper fungicide medicine, and repot in fresh aerated soil."
        ),
        "Powdery Mildew (Fungal Spores)" to Triple(
            "High humidity coupled with stagnant air circulation.",
            "White chalky or powdery coating on leaves, stunted development, leaves curling.",
            "Wipe leaves clean. Spray with organic remedy (1 tbsp baking soda + 1 tsp dish soap in 1 gallon water) or neem oil medicine. Increase room aeration."
        ),
        "Spider Mites (Tiny Pest Attack)" to Triple(
            "Dry, hot indoor climates.",
            "Fine silky webbing under leaves, speckled tiny yellow dots, dull leaf color.",
            "Isolate plant. Rinse foliage with high-pressure water. Spray thoroughly with neem oil medicine or insecticidal soap, targeting leaf backings."
        ),
        "Leaf Spot (Fungal/Bacterial)" to Triple(
            "Excess water sitting on leaf surfaces.",
            "Brown or black spots on foliage, often bordered by yellow halos.",
            "Cut off affected leaves. Avoid overhead watering. Spray plant with copper fungicide or organic liquid chamomile tea remedy."
        ),
        "Aphids (Sap-Sucking Insects)" to Triple(
            "Soft succulent new shoots and stems.",
            "Clustering tiny green/black bugs on tips, sticky shiny residue (honeydew), distorted leaves.",
            "Blast bugs off with water. Spray with organic neem oil solution or insecticidal soap every 3 days until fully cleared."
        ),
        "Nutrient Burn (Mineral Excess)" to Triple(
            "Too much chemical fertilizer application.",
            "Crispy brown leaf tips, crusty white mineral salt buildup on soil surface.",
            "Flush the soil thoroughly with generous plain water to wash out salts. Halt all fertilizer feeding for 2 months."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // DB Sub-Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Species Catalog", "Diseases & Remedies", "Home Remedies").forEach { section ->
                val selected = dbSection == section
                Button(
                    onClick = { dbSection = section },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF386B4F) else Color(0xFFE2EDE4),
                        contentColor = if (selected) Color.White else Color(0xFF386B4F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(section, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dbQuery,
            onValueChange = { dbQuery = it },
            placeholder = { Text("Search catalog... E.g. Aloe, Rot, Neem, Coffee") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF386B4F)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (dbSection == "Species Catalog") {
            val filteredSpecies = speciesDatabase.filter {
                it.first.contains(dbQuery, ignoreCase = true) || it.second.contains(dbQuery, ignoreCase = true)
            }
            if (filteredSpecies.isEmpty()) {
                Text("No matching plant species found.", fontStyle = FontStyle.Italic, color = Color.Gray)
            } else {
                filteredSpecies.forEach { (name, sciName, data) ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF112611))
                                    Text(sciName, fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color(0xFF5C6258))
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = Color(0xFF386B4F)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(data.first, fontSize = 13.sp, color = Color(0xFF2B2F2A))

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFF1F6F2))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Care Instructions & Remedies:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF386B4F))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(data.second, fontSize = 13.sp, color = Color(0xFF414941), lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        } else if (dbSection == "Diseases & Remedies") {
            val filteredDiseases = diseasesDatabase.filter {
                it.first.contains(dbQuery, ignoreCase = true) || it.second.first.contains(dbQuery, ignoreCase = true)
            }
            if (filteredDiseases.isEmpty()) {
                Text("No matching diseases or remedies found.", fontStyle = FontStyle.Italic, color = Color.Gray)
            } else {
                filteredDiseases.forEach { (title, content) ->
                    var isExpanded by remember { mutableStateOf(false) }
                    val (cause, symptoms, treatment) = content
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.BugReport, null, tint = Color(0xFF855428), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF112611))
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = Color(0xFF386B4F)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                Text("Cause: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF855428))
                                Text(cause, fontSize = 13.sp, color = Color(0xFF414941))
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFF1F6F2))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Symptoms:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF386B4F))
                                Text(symptoms, fontSize = 13.sp, color = Color(0xFF414941), modifier = Modifier.padding(start = 4.dp))

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Actionable Remedies & Medicine Plan:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF386B4F))
                                Text(treatment, fontSize = 13.sp, color = Color(0xFF1B3827), lineHeight = 18.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                            }
                        }
                    }
                }
            }
        } else {
            val filteredRemedies = homeRemediesDatabase.filter {
                it.title.contains(dbQuery, ignoreCase = true) || 
                it.category.contains(dbQuery, ignoreCase = true) || 
                it.summary.contains(dbQuery, ignoreCase = true) || 
                it.instructions.contains(dbQuery, ignoreCase = true)
            }
            if (filteredRemedies.isEmpty()) {
                Text("No matching home remedies found.", fontStyle = FontStyle.Italic, color = Color.Gray)
            } else {
                filteredRemedies.forEach { remedy ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = when {
                                            remedy.category.contains("Pesticide", ignoreCase = true) || remedy.category.contains("Insect", ignoreCase = true) || remedy.category.contains("Pest", ignoreCase = true) -> Icons.Default.BugReport
                                            remedy.category.contains("Fertilizer", ignoreCase = true) || remedy.category.contains("Nutrient", ignoreCase = true) || remedy.category.contains("Nitrogen", ignoreCase = true) -> Icons.Default.LocalFlorist
                                            remedy.category.contains("Water", ignoreCase = true) || remedy.category.contains("Tonic", ignoreCase = true) -> Icons.Default.WaterDrop
                                            else -> Icons.Default.Spa
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF386B4F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(remedy.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF112611))
                                        Text(remedy.category, fontSize = 11.sp, color = Color(0xFF386B4F), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = Color(0xFF386B4F)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(remedy.summary, fontSize = 13.sp, color = Color(0xFF414941))

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFF1F6F2))
                                Spacer(modifier = Modifier.height(10.dp))

                                Text("How to Prepare & Apply:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF386B4F))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = remedy.instructions,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1B3827),
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val context = LocalContext.current
                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                            data = android.provider.CalendarContract.Events.CONTENT_URI
                                            putExtra(android.provider.CalendarContract.Events.TITLE, "Apply Remedy: ${remedy.title}")
                                            putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Category: ${remedy.category}\n\n${remedy.summary}\n\nInstructions: ${remedy.instructions}")
                                            putExtra(android.provider.CalendarContract.Events.RRULE, "FREQ=WEEKLY")
                                            putExtra(android.provider.CalendarContract.Events.AVAILABILITY, android.provider.CalendarContract.Events.AVAILABILITY_FREE)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3EB), contentColor = Color(0xFF386B4F)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Schedule to Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-TAB 3: LACKING CHECKER (DEFICIENCY DIAGNOSER) ---
@Composable
fun LackingCheckerTab(viewModel: GardenViewModel, loadingMessage: String? = null) {
    val deficiencyResult by viewModel.deficiencyResult.collectAsStateWithLifecycle()
    val isCheckingDeficiency by viewModel.isCheckingDeficiency.collectAsStateWithLifecycle()

    var selectedSymptoms = remember { mutableStateListOf<String>() }
    var envNotesInput by remember { mutableStateOf("") }
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
                Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_LONG).show()
            }
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
                selectedBitmap = uriToBitmap(context, uri)
            }
        }
    )

    val symptomsList = listOf(
        "Pale / washed out yellowing leaves (Nitrogen Mineral lack)",
        "Crispy brown leaf tips or crispy margins (Water lack)",
        "Stretched, leggy growth & pale stems (Sunlight lack)",
        "Stunted slow growth with purple hues (Phosphorus lack)",
        "Drooping wilted stems with wet damp soil (Water excess/rot)",
        "Dry crispy curling leaves (Humidity/Water lack)",
        "White powdery mildew or grey mold spots (Fungicide need)",
        "Webs, tiny bugs or sticky fluid on stems (Pesticide/Medicine need)",
        "Deformed curling new leaves (Calcium Vitamin lack)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Deficiency & Lacking Analyzer",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF112611)
        )
        Text(
            text = "Select what symptoms you observe to diagnose lack of minerals, sunlight, water, vitamins, or medicine.",
            fontSize = 13.sp,
            color = Color(0xFF5C6258)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Symptoms Multi-Select Checklist
        Text("Observed Symptoms:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF386B4F))
        Spacer(modifier = Modifier.height(6.dp))

        symptomsList.forEach { symptom ->
            val isChecked = selectedSymptoms.contains(symptom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isChecked) selectedSymptoms.remove(symptom) else selectedSymptoms.add(symptom)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        if (isChecked) selectedSymptoms.remove(symptom) else selectedSymptoms.add(symptom)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF386B4F))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(symptom, fontSize = 13.sp, color = Color(0xFF112611))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Attach Image option
        Text("Attach Plant Photo (Optional - helpful for AI checking):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF386B4F))
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasCameraPermission) {
                        cameraLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2EDE4), contentColor = Color(0xFF386B4F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2EDE4), contentColor = Color(0xFF386B4F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Photo", fontSize = 12.sp)
            }
        }

        selectedBitmap?.let { bitmap ->
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected plant preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = {
                        selectedBitmap = null
                        selectedUri = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = envNotesInput,
            onValueChange = { envNotesInput = it },
            label = { Text("Environment notes (Soil dampness, fertilizer frequency, light spot...)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.checkDeficiency(selectedSymptoms, envNotesInput, selectedBitmap)
            },
            enabled = !isCheckingDeficiency && (selectedSymptoms.isNotEmpty() || envNotesInput.isNotBlank()),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isCheckingDeficiency) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = loadingMessage ?: "Diagnosing Nutrient Deficiencies...",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Icon(Icons.Default.QueryStats, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Deficiency Check")
            }
        }

        // Display Results
        AnimatedVisibility(
            visible = deficiencyResult != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            deficiencyResult?.let { result ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FactCheck, null, tint = Color(0xFF386B4F), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Results", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF112611))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F6F2), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Diagnosis / Condition:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF386B4F))
                                Text(result.plantCondition, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B3827))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Lacking Category: ${result.lackingCategory}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF855428))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Comprehensive Category Check", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF112611))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Category list breakdown
                        DeficiencyItem(title = "1. Minerals (NPK & Minerals)", icon = Icons.Default.Terrain, text = result.mineralsAnalysis)
                        DeficiencyItem(title = "2. Sunlight (Light intensity)", icon = Icons.Default.LightMode, text = result.sunlightAnalysis)
                        DeficiencyItem(title = "3. Water (Watering loop)", icon = Icons.Default.WaterDrop, text = result.waterAnalysis)
                        DeficiencyItem(title = "4. Vitamins (Growth boosters)", icon = Icons.Default.Spa, text = result.vitaminsAnalysis)
                        DeficiencyItem(title = "5. Medicine (Pest & disease remedies)", icon = Icons.Default.Healing, text = result.medicineAnalysis)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Remedial Action Steps Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF112611))
                        Spacer(modifier = Modifier.height(8.dp))

                        result.remedialPlan.forEachIndexed { i, step ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Text("${i + 1}.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF386B4F))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(step, fontSize = 13.sp, color = Color(0xFF414941))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeficiencyItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = Color(0xFF386B4F), modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF112611))
            Text(text, fontSize = 12.sp, color = Color(0xFF5C6258), lineHeight = 16.sp)
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

@Composable
fun WeatherAdvisorTab(viewModel: GardenViewModel, loadingMessage: String? = null) {
    val weatherAdvice by viewModel.weatherAdvice.collectAsStateWithLifecycle()
    val isFetchingWeatherAdvice by viewModel.isFetchingWeatherAdvice.collectAsStateWithLifecycle()

    var citySearchInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                detectLocationForWeather(
                    context = context,
                    onSuccess = { lat, lon, resolvedAddress ->
                        viewModel.fetchWeatherAndGardeningAdviceByCoords(lat.toFloat(), lon.toFloat(), resolvedAddress)
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(context, "Location permission is required to detect coordinates.", Toast.LENGTH_LONG).show()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Localized Climate Advisor",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF112611)
        )
        Text(
            text = "Enter a city or use GPS to fetch live climate conditions and personalized Gemini gardening tips.",
            fontSize = 13.sp,
            color = Color(0xFF5C6258)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search panel card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = citySearchInput,
                    onValueChange = { citySearchInput = it },
                    label = { Text("Search by City Name") },
                    placeholder = { Text("e.g. London, New York, Tokyo") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF386B4F)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weather_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF386B4F),
                        unfocusedBorderColor = Color(0xFFC2D1C7)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Use GPS button
                    Button(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                detectLocationForWeather(
                                    context = context,
                                    onSuccess = { lat, lon, resolvedAddress ->
                                        viewModel.fetchWeatherAndGardeningAdviceByCoords(lat.toFloat(), lon.toFloat(), resolvedAddress)
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE2EDE4),
                            contentColor = Color(0xFF386B4F)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weather_detect_location_button")
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use GPS", fontSize = 12.sp)
                    }

                    // Fetch advisor button
                    Button(
                        onClick = {
                            if (citySearchInput.isNotBlank()) {
                                viewModel.fetchWeatherAndGardeningAdvice(citySearchInput.trim())
                            } else {
                                Toast.makeText(context, "Please enter a city name first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF386B4F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("weather_submit_button"),
                        enabled = !isFetchingWeatherAdvice && citySearchInput.isNotBlank()
                    ) {
                        if (isFetchingWeatherAdvice) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Advice", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isFetchingWeatherAdvice) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF386B4F))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = loadingMessage ?: "Retrieving climate guidelines...",
                        fontSize = 13.sp,
                        color = Color(0xFF5C6258)
                    )
                }
            }
        }

        // Display advisory results
        AnimatedVisibility(
            visible = weatherAdvice != null && !isFetchingWeatherAdvice,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            weatherAdvice?.let { advice ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Weather stats row
                    Text(
                        text = "Current Weather in ${advice.locationName}:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF112611),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherMetricCard(
                            label = "Temp",
                            value = "${advice.temperature}°C",
                            icon = Icons.Default.Thermostat,
                            color = Color(0xFFFDF2E9),
                            textColor = Color(0xFFB05C1E),
                            modifier = Modifier.weight(1f)
                        )
                        WeatherMetricCard(
                            label = "Humidity",
                            value = "${advice.humidity}%",
                            icon = Icons.Default.WaterDrop,
                            color = Color(0xFFEBF5FB),
                            textColor = Color(0xFF2874A6),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherMetricCard(
                            label = "Conditions",
                            value = advice.weatherDesc,
                            icon = Icons.Default.CloudQueue,
                            color = Color(0xFFF2F4F4),
                            textColor = Color(0xFF5D6D7E),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Advice Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE2EDE4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Spa, null, tint = Color(0xFF386B4F), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Gemini Gardening Advice",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1B3827)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFE2EDE4))
                            Spacer(modifier = Modifier.height(12.dp))

                            GroundedTextRenderer(text = advice.adviceText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 11.sp, color = textColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                Text(value, fontSize = 14.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// --- SCREEN SUB-COMPONENTS: ADD PLANT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantManualDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, nickname: String, spec: String, interval: Int, sun: String, diff: String, instructions: String, location: String, addedDate: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var intervalInput by remember { mutableStateOf("7") }
    var sunlightInput by remember { mutableStateOf("Bright Indirect Light") }
    var difficultyInput by remember { mutableStateOf("Easy") }
    var instructionsInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("Indoor") }
    var addedDate by remember { mutableStateOf(System.currentTimeMillis()) }

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

                Text(
                    text = "Date Added",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5C6258).copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                val context = LocalContext.current
                val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = addedDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                addedDate = selectedCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF386B4F)),
                    border = BorderStroke(1.dp, Color(0xFFE0E3DA))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateFormatter.format(Date(addedDate)),
                            color = Color(0xFF112611),
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Choose Date Added",
                            tint = Color(0xFF386B4F)
                        )
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
                                    locationInput,
                                    addedDate
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

// --- LOCATION HELPERS ---
fun detectLocation(
    context: Context,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onError("Location service not available")
        return
    }

    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!fineGranted && !coarseGranted) {
        onError("Location permissions not granted")
        return
    }

    try {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            onError("Please enable GPS or Network location in system settings")
            return
        }

        val lastKnownLocation = locationManager.getLastKnownLocation(provider)
        if (lastKnownLocation != null) {
            resolveAddress(context, lastKnownLocation.latitude, lastKnownLocation.longitude, onSuccess, onError)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(
                    provider,
                    null,
                    context.mainExecutor
                ) { location ->
                    if (location != null) {
                        resolveAddress(context, location.latitude, location.longitude, onSuccess, onError)
                    } else {
                        onError("Could not determine current coordinates")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(
                    provider,
                    object : android.location.LocationListener {
                        override fun onLocationChanged(location: Location) {
                            resolveAddress(context, location.latitude, location.longitude, onSuccess, onError)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    context.mainLooper
                )
            }
        }
    } catch (e: SecurityException) {
        onError("Permission denied: ${e.message}")
    } catch (e: Exception) {
        onError("Error detecting location: ${e.message}")
    }
}

fun detectLocationForWeather(
    context: Context,
    onSuccess: (Double, Double, String) -> Unit,
    onError: (String) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onError("Location service not available")
        return
    }

    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!fineGranted && !coarseGranted) {
        onError("Location permissions not granted")
        return
    }

    try {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            onError("Please enable GPS or Network location in system settings")
            return
        }

        val lastKnownLocation = locationManager.getLastKnownLocation(provider)
        if (lastKnownLocation != null) {
            resolveAddressForWeather(context, lastKnownLocation.latitude, lastKnownLocation.longitude, onSuccess, onError)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(
                    provider,
                    null,
                    context.mainExecutor
                ) { location ->
                    if (location != null) {
                        resolveAddressForWeather(context, location.latitude, location.longitude, onSuccess, onError)
                    } else {
                        onError("Could not determine current coordinates")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(
                    provider,
                    object : android.location.LocationListener {
                        override fun onLocationChanged(location: Location) {
                            resolveAddressForWeather(context, location.latitude, location.longitude, onSuccess, onError)
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    context.mainLooper
                )
            }
        }
    } catch (e: SecurityException) {
        onError("Permission denied: ${e.message}")
    } catch (e: Exception) {
        onError("Error detecting location: ${e.message}")
    }
}

fun resolveAddressForWeather(
    context: Context,
    latitude: Double,
    longitude: Double,
    onSuccess: (Double, Double, String) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val state = address.adminArea ?: ""
                val country = address.countryName ?: ""
                
                val locationString = when {
                    city.isNotBlank() && country.isNotBlank() -> {
                        if (state.isNotBlank() && state != city) "$city, $state, $country" else "$city, $country"
                    }
                    city.isNotBlank() -> city
                    country.isNotBlank() -> country
                    else -> "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
                }
                
                withContext(Dispatchers.Main) {
                    onSuccess(latitude, longitude, locationString)
                }
            } else {
                val fallbackString = "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
                withContext(Dispatchers.Main) {
                    onSuccess(latitude, longitude, fallbackString)
                }
            }
        } catch (e: Exception) {
            val fallbackString = "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
            withContext(Dispatchers.Main) {
                onSuccess(latitude, longitude, fallbackString)
            }
        }
    }
}

fun resolveAddress(
    context: Context,
    latitude: Double,
    longitude: Double,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val state = address.adminArea ?: ""
                val country = address.countryName ?: ""
                
                val locationString = when {
                    city.isNotBlank() && country.isNotBlank() -> {
                        if (state.isNotBlank() && state != city) "$city, $state, $country" else "$city, $country"
                    }
                    city.isNotBlank() -> city
                    country.isNotBlank() -> country
                    else -> "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}"
                }
                
                withContext(Dispatchers.Main) {
                    onSuccess(locationString)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onSuccess("Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onSuccess("Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}")
            }
        }
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

fun getReferenceImagesForPlant(plantName: String): List<String> {
    val nameLower = plantName.lowercase()
    return when {
        nameLower.contains("monstera") -> listOf(
            "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=600",
            "https://images.unsplash.com/photo-1545241047-6083a3684587?q=80&w=600"
        )
        nameLower.contains("fiddle") || nameLower.contains("lyrata") -> listOf(
            "https://images.unsplash.com/photo-1597055181300-e3633a207518?q=80&w=600",
            "https://images.unsplash.com/photo-1598880940080-ff9a29891b85?q=80&w=600"
        )
        nameLower.contains("aloe") -> listOf(
            "https://images.unsplash.com/photo-1596547609652-9cf5d8d76921?q=80&w=600",
            "https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?q=80&w=600"
        )
        nameLower.contains("snake") || nameLower.contains("sansevieria") -> listOf(
            "https://images.unsplash.com/photo-1593487568522-746db8894941?q=80&w=600",
            "https://images.unsplash.com/photo-1620131102715-db14856f6c91?q=80&w=600"
        )
        nameLower.contains("peace lily") || nameLower.contains("spathiphyllum") -> listOf(
            "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?q=80&w=600",
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=600"
        )
        nameLower.contains("pothos") || nameLower.contains("epipremnum") -> listOf(
            "https://images.unsplash.com/photo-1620131102715-db14856f6c91?q=80&w=600",
            "https://images.unsplash.com/photo-1592150621744-aca64f48394a?q=80&w=600"
        )
        nameLower.contains("jade") || nameLower.contains("crassula") -> listOf(
            "https://images.unsplash.com/photo-1598512752271-33f913a5af13?q=80&w=600",
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=600"
        )
        nameLower.contains("lavender") || nameLower.contains("lavandula") -> listOf(
            "https://images.unsplash.com/photo-1528183429752-a97d0bf99b5a?q=80&w=600",
            "https://images.unsplash.com/photo-1565538810844-1e1194826c91?q=80&w=600"
        )
        nameLower.contains("spider") || nameLower.contains("chlorophytum") -> listOf(
            "https://images.unsplash.com/photo-1572097665309-2591eeb573be?q=80&w=600",
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=600"
        )
        else -> listOf(
            "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?q=80&w=600",
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=600"
        )
    }
}

@Composable
fun GrowthLineChart(
    metrics: List<com.example.data.model.GrowthMetric>,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFF2F5EB), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No growth metrics logged yet. Add your first metrics log above!",
                fontSize = 12.sp,
                color = Color(0xFF5C6258),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val sortedMetrics = remember(metrics) { metrics.sortedBy { it.timestamp } }
    var selectedMetricType by remember { mutableStateOf("Height") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedMetricType Over Time",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF386B4F)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Height", "Leaf Count").forEach { type ->
                    val isSelected = selectedMetricType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedMetricType = type }
                            .background(if (isSelected) Color(0xFF386B4F) else Color(0xFFE2EDE4))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = type,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF386B4F)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val maxVal = remember(sortedMetrics, selectedMetricType) {
            val maxRaw = if (selectedMetricType == "Height") {
                sortedMetrics.maxOfOrNull { it.heightCm } ?: 10f
            } else {
                sortedMetrics.maxOfOrNull { it.leafCount }?.toFloat() ?: 5f
            }
            if (maxRaw == 0f) 10f else maxRaw * 1.2f
        }

        val minVal = remember(sortedMetrics, selectedMetricType) {
            val minRaw = if (selectedMetricType == "Height") {
                sortedMetrics.minOfOrNull { it.heightCm } ?: 0f
            } else {
                sortedMetrics.minOfOrNull { it.leafCount }?.toFloat() ?: 0f
            }
            (minRaw * 0.8f).coerceAtLeast(0f)
        }

        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFF9FBF7), RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0xFFE2EDE4)), RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            val width = size.width
            val height = size.height
            val pointsCount = sortedMetrics.size

            val gridColor = Color(0xFFE2EDE4)
            for (i in 0..2) {
                val y = height * (i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            if (pointsCount == 1) {
                val metric = sortedMetrics[0]
                val value = if (selectedMetricType == "Height") metric.heightCm else metric.leafCount.toFloat()
                val yRatio = if (maxVal == minVal) 0.5f else (value - minVal) / (maxVal - minVal)
                val x = width / 2f
                val y = height - (yRatio * height)

                drawCircle(
                    color = Color(0xFF386B4F),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#386B4F")
                    textSize = 24f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val displayLabel = if (selectedMetricType == "Height") "${metric.heightCm} cm" else "${metric.leafCount}"
                drawContext.canvas.nativeCanvas.drawText(
                    displayLabel,
                    x,
                    y - 8.dp.toPx(),
                    textPaint
                )
                return@Canvas
            }

            val path = androidx.compose.ui.graphics.Path()
            val fillPath = androidx.compose.ui.graphics.Path()

            val stepX = width / (pointsCount - 1)

            for (i in 0 until pointsCount) {
                val metric = sortedMetrics[i]
                val value = if (selectedMetricType == "Height") metric.heightCm else metric.leafCount.toFloat()
                val yRatio = if (maxVal == minVal) 0.5f else (value - minVal) / (maxVal - minVal)
                val x = i * stepX
                val y = height - (yRatio * height)

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (i == pointsCount - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF386B4F).copy(alpha = 0.25f), Color.Transparent)
                )
            )

            drawPath(
                path = path,
                color = Color(0xFF386B4F),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx()
                )
            )

            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#386B4F")
                textSize = 24f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            for (i in 0 until pointsCount) {
                val metric = sortedMetrics[i]
                val value = if (selectedMetricType == "Height") metric.heightCm else metric.leafCount.toFloat()
                val yRatio = if (maxVal == minVal) 0.5f else (value - minVal) / (maxVal - minVal)
                val x = i * stepX
                val y = height - (yRatio * height)

                drawCircle(
                    color = Color(0xFF386B4F),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )

                val displayLabel = if (selectedMetricType == "Height") "${metric.heightCm} cm" else "${metric.leafCount}"
                drawContext.canvas.nativeCanvas.drawText(
                    displayLabel,
                    x,
                    y - 8.dp.toPx(),
                    textPaint
                )
            }
        }
    }
}

@Composable
fun EasyHouseholdFertilizersTab() {
    var searchQuery by remember { mutableStateOf("") }
    
    val remedies = remember {
        listOf(
            FertilizerRemedy(
                name = "Aquarium Water",
                description = "Water your plants with the aquarium water taken right out of the tank when cleaning it. Use only fresh water and not Saltwater. The fish waste makes a great plant fertiliser.",
                bestFor = "Foliage plants, leafy greens, & all indoor houseplants",
                tips = "Use only freshwater aquarium water. Avoid saltwater entirely.",
                colorTheme = Color(0xFFE3F2FD),
                iconTint = Color(0xFF1E88E5)
            ),
            FertilizerRemedy(
                name = "Bananas",
                description = "Bananas are not only tasty and healthy for humans, but they also benefit many different plants. When planting roses, bury a banana (or just the peel) in the hole alongside the rose. As the rose grows, bury bananas or banana peels into the top layer of the soil. Both of these methods will provide the much-needed potassium that plants need for proper growth.",
                bestFor = "Roses, flowering shrubs, & potassium-hungry plants",
                tips = "Bury peels deep enough to avoid attracting gnats or rodents.",
                colorTheme = Color(0xFFFFFDE7),
                iconTint = Color(0xFFFBC02D)
            ),
            FertilizerRemedy(
                name = "Coffee Grounds",
                description = "Coffee grounds are particularly useful for plants such as blueberries, evergreens, roses, camellias, avocados, and many fruit trees. Use dry coffee grounds and scatter them on the soil. Wet coffee grounds may lead to the growth of algae.",
                bestFor = "Acid-loving plants, blueberries, roses, & fruit trees",
                tips = "Use fully dried grounds to prevent mold and algae growth.",
                colorTheme = Color(0xFFEFEBE9),
                iconTint = Color(0xFF6D4C41)
            ),
            FertilizerRemedy(
                name = "Cooking Water",
                description = "Several nutrients are released into the water in which the food is cooked. Water that is used to boil potatoes, vegetables, eggs, and even pasta can be utilized as a fertiliser. Just cool the water before applying it to soil.",
                bestFor = "Container houseplants, garden beds, & foliage plants",
                tips = "Ensure water has cooled fully and contains no added salt or oils.",
                colorTheme = Color(0xFFE0F2F1),
                iconTint = Color(0xFF00897B)
            ),
            FertilizerRemedy(
                name = "Egg Shells",
                description = "Egg shells contain nitrogen, trace elements, and calcium. Calcium is an essential plant nutrient which plays a fundamental part in cell manufacture and growth. Plant growth removes large quantities of calcium from the soil, and calcium must be replenished, so the use of egg shells can help the plant. Crush the egg shells and sprinkle on the ground.",
                bestFor = "Tomatoes (prevents end rot), peppers, & eggplants",
                tips = "Bake and crush eggshells as finely as possible for faster root uptake.",
                colorTheme = Color(0xFFF5F5F5),
                iconTint = Color(0xFF616161)
            ),
            FertilizerRemedy(
                name = "Epsom Salts",
                description = "It contains magnesium and sulphur. Add salt to the soil combined with water to provide soil with magnesium and sulphur. A dose of an Epsom salts solution increases fruit and flower production in roses, tomatoes, potatoes, peppers, and houseplants.",
                bestFor = "Roses, tomatoes, potatoes, peppers, & houseplants",
                tips = "Dissolve 1 tablespoon in 4L water to spray directly onto leaves.",
                colorTheme = Color(0xFFF3E5F5),
                iconTint = Color(0xFF8E24AA)
            ),
            FertilizerRemedy(
                name = "Wood Ash (From Your Fireplace or Fire Pit)",
                description = "Ashes can be sprinkled onto your soil to supply potassium and calcium carbonate. Hardwood is best. Ashes are alkaline and can increase alkalinity in the ground.",
                bestFor = "Alkaline-loving plants, root vegetables, lawns, & shrubs",
                tips = "Apply sparingly and avoid using on acid-loving plants.",
                colorTheme = Color(0xFFECEFF1),
                iconTint = Color(0xFF546E7A)
            ),
            FertilizerRemedy(
                name = "Gelatin",
                description = "Gelatin is a great nitrogen source. Dissolve in hot water, then add cold water and pour on soil once a month.",
                bestFor = "Foliage house plants, monsteras, & ferns",
                tips = "Use only plain, unflavored, and sugar-free gelatin.",
                colorTheme = Color(0xFFFBE9E7),
                iconTint = Color(0xFFF4511E)
            ),
            FertilizerRemedy(
                name = "Green Tea",
                description = "Green tea is especially beneficial for raspberry plants as raspberry plants require a great amount of iron.",
                bestFor = "Raspberries, ferns, & acid-preferring plants",
                tips = "Dilute cold green tea 1:1 with clean water before application.",
                colorTheme = Color(0xFFE8F5E9),
                iconTint = Color(0xFF43A047)
            ),
            FertilizerRemedy(
                name = "Hair",
                description = "Hair is a good source of nitrogen. Add human or animal hair to soil for good results.",
                bestFor = "Outdoor garden beds & slow-release organic compost",
                tips = "Cut hair finely and mix deeply into soil near the root zone.",
                colorTheme = Color(0xFFFBEBE9),
                iconTint = Color(0xFF8D6E63)
            ),
            FertilizerRemedy(
                name = "Matchsticks",
                description = "The old fashioned matches are an excellent source of magnesium. To use this as a fertiliser, just place the whole in the hole with the plant, or soak them in water. The magnesium will dissolve into the water and make application easier.",
                bestFor = "Peppers, tomatoes, roses, & magnesium-hungry plants",
                tips = "Old-fashioned paper matches work best. Soak in water overnight.",
                colorTheme = Color(0xFFFFEBEE),
                iconTint = Color(0xFFE53935)
            ),
            FertilizerRemedy(
                name = "Powdered Milk",
                description = "Powdered milk is not only good for human consumption but also for plants. This source of calcium needs to be mixed into the soil before planting. Since the milk is in powder form, it is ready for use by your plants. Diluted milk can also act as an excellent fertiliser. To keep fungus from affecting your tomato plants, try watering it with diluted cold milk.",
                bestFor = "Tomatoes, peppers, squashes, & calcium-needing plants",
                tips = "Spray diluted milk onto foliage to help prevent leaf spot fungus.",
                colorTheme = Color(0xFFFFF8E1),
                iconTint = Color(0xFFFFB300)
            )
        )
    }

    val filteredRemedies = remember(searchQuery) {
        if (searchQuery.isBlank()) remedies
        else remedies.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.bestFor.contains(searchQuery, ignoreCase = true)
        }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search fertilizers... e.g. Banana, Milk", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF386B4F)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, tint = Color(0xFF386B4F))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fertilizers_search_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF386B4F),
                unfocusedBorderColor = Color(0xFFC2D1C7)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable content
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Intro Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF386B4F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "There are quite a few common items found in your house, that can be used as plant fertilizer.",
                            fontSize = 13.sp,
                            color = Color(0xFF1B3827),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            items(filteredRemedies, key = { it.name }) { remedy ->
                var isExpanded by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Beautiful Graphic
                            FertilizerRemedyGraphic(
                                remedyName = remedy.name,
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = remedy.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF112611)
                                )
                                Text(
                                    text = if (isExpanded) "Tap to hide details" else "Tap to view instructions",
                                    fontSize = 11.sp,
                                    color = Color(0xFF386B4F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand/Collapse",
                                tint = Color(0xFF386B4F),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                HorizontalDivider(color = Color(0xFFF1F6F2))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = remedy.description,
                                    fontSize = 13.sp,
                                    color = Color(0xFF414941),
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(remedy.colorTheme, RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text("BEST FOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = remedy.iconTint)
                                            Text(remedy.bestFor, fontSize = 11.sp, color = Color(0xFF112611), fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Color(0xFFF1F6F2), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text("PRO TIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF386B4F))
                                            Text(remedy.tips, fontSize = 11.sp, color = Color(0xFF112611), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                            data = android.provider.CalendarContract.Events.CONTENT_URI
                                            putExtra(android.provider.CalendarContract.Events.TITLE, "Fertilize with ${remedy.name}")
                                            putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "${remedy.description}\n\nBest for: ${remedy.bestFor}\n\nPro Tip: ${remedy.tips}")
                                            putExtra(android.provider.CalendarContract.Events.RRULE, "FREQ=MONTHLY")
                                            putExtra(android.provider.CalendarContract.Events.AVAILABILITY, android.provider.CalendarContract.Events.AVAILABILITY_FREE)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3EB), contentColor = Color(0xFF386B4F)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Monthly Reminder to Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD7E8DE)),
                    border = BorderStroke(1.dp, Color(0xFFC2D1C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = Color(0xFF386B4F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Naturally Beautiful & Affordable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF112611)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Consider using this household natural garden fertilisers and feel good about the fact that you are doing things naturally and saving money in the process. Also making it aptly right that beautiful gardens are not expensive.",
                            fontSize = 12.sp,
                            color = Color(0xFF414941),
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HomeRemediesTab() {
    var searchQuery by remember { mutableStateOf("") }

    val filteredRemedies = remember(searchQuery) {
        if (searchQuery.isBlank()) homeRemediesDatabase
        else homeRemediesDatabase.filter {
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.category.contains(searchQuery, ignoreCase = true) || 
            it.summary.contains(searchQuery, ignoreCase = true) || 
            it.instructions.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search remedies... E.g. Coffee, Eggshell, Neem", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF386B4F)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, tint = Color(0xFF386B4F))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_remedies_search_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF386B4F),
                unfocusedBorderColor = Color(0xFFC2D1C7)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable content
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Info Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EBE6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Healing,
                            contentDescription = null,
                            tint = Color(0xFF855428),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "DIY home remedies, tonics, and natural pest solutions crafted from basic pantry items.",
                            fontSize = 13.sp,
                            color = Color(0xFF4E3629),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (filteredRemedies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching home remedies found.",
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredRemedies, key = { it.title }) { remedy ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2EDE4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFF1F6F2), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                remedy.category.contains("Pesticide", ignoreCase = true) || remedy.category.contains("Insect", ignoreCase = true) || remedy.category.contains("Pest", ignoreCase = true) -> Icons.Default.BugReport
                                                remedy.category.contains("Fertilizer", ignoreCase = true) || remedy.category.contains("Nutrient", ignoreCase = true) || remedy.category.contains("Nitrogen", ignoreCase = true) -> Icons.Default.LocalFlorist
                                                else -> Icons.Default.Spa
                                            },
                                            contentDescription = null,
                                            tint = Color(0xFF386B4F),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(remedy.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF112611))
                                        Text(remedy.category, fontSize = 11.sp, color = Color(0xFF386B4F), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = Color(0xFF386B4F)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(remedy.summary, fontSize = 13.sp, color = Color(0xFF414941))

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(color = Color(0xFFF1F6F2))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text("How to Prepare & Apply:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF386B4F))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = remedy.instructions,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1B3827),
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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

@Composable
fun FeedbackModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }
    var isSubmitted by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("feedback_modal_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFE2EDE4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Color(0xFF386B4F),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Connect & Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF112611)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Have questions, feedback, or ideas? Reach out directly or send feedback below!",
                    fontSize = 13.sp,
                    color = Color(0xFF5C6258),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!isSubmitted) {
                    // Feedback Form Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SEND ANONYMOUS FEEDBACK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF386B4F),
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Star Rating Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { starIndex ->
                                    IconButton(
                                        onClick = { rating = starIndex },
                                        modifier = Modifier.size(36.dp).testTag("rating_star_$starIndex")
                                    ) {
                                        Icon(
                                            imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Rate $starIndex stars",
                                            tint = if (starIndex <= rating) Color(0xFFFBC02D) else Color(0xFF9E9E9E),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { feedbackText = it },
                                placeholder = { Text("What can we improve? Or tell us what you love!", fontSize = 13.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("feedback_text_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF386B4F),
                                    unfocusedBorderColor = Color(0xFFC2D1C7)
                                ),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (feedbackText.isBlank()) {
                                        Toast.makeText(context, "Please write a message first!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isSubmitted = true
                                        Toast.makeText(context, "Feedback sent! Thank you!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("submit_feedback_button")
                            ) {
                                Text("Submit Feedback", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EDE4)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF386B4F),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Feedback Received!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1B3827)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Thank you for helping us make this app better.",
                                fontSize = 12.sp,
                                color = Color(0xFF414941),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Developer & Contact Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F4F0)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE8E3DD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DEVELOPER CONTACT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8D6E63),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFEFEBE9), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF8D6E63)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Kanaka Mahesh",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF3E2723)
                                )
                                Text(
                                    text = "kanakamahesh4@gmail.com",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6D4C41)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Intent Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:kanakamahesh4@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Potted Garden App - Feedback/Inquiry")
                                    }
                                    context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not launch email app", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8D6E63)),
                            border = BorderStroke(1.dp, Color(0xFFD7CCC8)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("email_developer_button")
                        ) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email Me Directly", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Social Media Section
                Text(
                    text = "FOLLOW & CONNECT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386B4F),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val socials = listOf(
                        Triple("GitHub", "https://github.com/kanakamahesh4", Icons.Default.Code),
                        Triple("LinkedIn", "https://linkedin.com/in/kanakamahesh4", Icons.Default.Link),
                        Triple("Twitter", "https://x.com/kanakamahesh4", Icons.Default.Public)
                    )

                    socials.forEach { (name, url, icon) ->
                        Button(
                            onClick = {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE2EDE4),
                                contentColor = Color(0xFF386B4F)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("social_button_${name.lowercase()}")
                        ) {
                            Icon(icon, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_feedback_modal_button")
                ) {
                    Text("Close Panel", color = Color(0xFF386B4F), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SyncingOverlay(statusText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_loader")
    
    // Rotation of the outer ring / leaves
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing scale for the core icon
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(
        onDismissRequest = {}, // Cannot be dismissed manually
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .width(280.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom Canvas animated background ring
                        Canvas(modifier = Modifier.size(90.dp)) {
                            // Draw spinning segmented track
                            drawArc(
                                color = Color(0x22386B4F),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            
                            drawArc(
                                color = Color(0xFF386B4F),
                                startAngle = rotation,
                                sweepAngle = 90f,
                                useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        // Pulsing leaf/spa icon at the center
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "Syncing",
                            tint = Color(0xFF386B4F),
                            modifier = Modifier
                                .size(48.dp)
                                .scale(scale)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = statusText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Keeping your plant collections safe in the cloud",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

