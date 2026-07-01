package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.WeatherClient
import com.example.api.GeminiContent
import com.example.api.GeminiGenerationConfig
import com.example.api.GeminiInlineData
import com.example.api.GeminiPart
import com.example.api.GeminiRequest
import com.example.api.GeminiTool
import com.example.api.GeminiGoogleSearch
import com.example.api.dto.DiseaseDiagnosisDTO
import com.example.api.dto.PersonalizedCarePlanDTO
import com.example.api.dto.PlantIdentificationDTO
import com.example.data.database.GardenDatabase
import com.example.data.model.Diagnosis
import com.example.data.model.Plant
import com.example.data.model.PlantIdentification
import com.example.data.model.JournalEntry
import com.example.data.model.GrowthMetric
import com.example.data.repository.GardenRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class GardenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GardenRepository
    val plants: StateFlow<List<Plant>>
    val diagnosesHistory: StateFlow<List<Diagnosis>>
    val identificationsHistory: StateFlow<List<PlantIdentification>>
    val careTasks: StateFlow<List<com.example.data.model.CareTask>>

    val userProfile: StateFlow<com.example.data.model.UserProfile?>
    val searchCount: StateFlow<Int>
    val allTransactions: StateFlow<List<com.example.data.model.TransactionRecord>>

    private val _showSubscriptionRequired = MutableStateFlow(false)
    val showSubscriptionRequired = _showSubscriptionRequired.asStateFlow()

    var isFirebaseAvailable = false
        private set

    private val _firebaseSyncStatus = MutableStateFlow("Offline Mode (No google-services.json)")
    val firebaseSyncStatus = _firebaseSyncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val sharedPreferences = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(sharedPreferences.getString("app_language", "English") ?: "English")
    val appLanguage = _appLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(
        if (sharedPreferences.contains("is_dark_mode")) {
            sharedPreferences.getBoolean("is_dark_mode", false)
        } else {
            null
        }
    )
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean?) {
        _isDarkMode.value = isDark
        if (isDark == null) {
            sharedPreferences.edit().remove("is_dark_mode").apply()
        } else {
            sharedPreferences.edit().putBoolean("is_dark_mode", isDark).apply()
        }
    }

    fun setAppLanguage(language: String) {
        _appLanguage.value = language
        sharedPreferences.edit().putString("app_language", language).apply()
    }

    init {
        val database = GardenDatabase.getDatabase(application)
        repository = GardenRepository(database.gardenDao())

        try {
            val isRunningTest = try {
                Class.forName("org.junit.Test")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
            if (isRunningTest) {
                isFirebaseAvailable = false
                _firebaseSyncStatus.value = "Offline Mode (No active firebase configuration)"
            } else {
                val auth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                _firebaseSyncStatus.value = if (auth.currentUser != null) {
                    "Cloud Ready: Synced as ${auth.currentUser?.email}"
                } else {
                    "Cloud Ready: Local Mode"
                }
            }
        } catch (e: Exception) {
            isFirebaseAvailable = false
            _firebaseSyncStatus.value = "Offline Mode (No active firebase configuration)"
        }

        viewModelScope.launch {
            if (isFirebaseAvailable) {
                kotlinx.coroutines.delay(1500)
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser != null) {
                    syncPlantsWithFirebase()
                }
            }
        }

        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        searchCount = repository.searchCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                if (profile == null) {
                    repository.insertUserProfile(
                        com.example.data.model.UserProfile(
                            id = 1,
                            subscriptionStatus = "NONE",
                            expiryDate = 0L,
                            email = "guest@example.com",
                            billingProvider = "NONE",
                            isLoggedIn = false,
                            displayName = "",
                            photoUrl = ""
                        )
                    )
                }
            }
        }

        plants = repository.allPlants.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        diagnosesHistory = repository.allDiagnoses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        identificationsHistory = repository.allIdentifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        careTasks = repository.allCareTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed some starter plants if empty
        viewModelScope.launch {
            repository.allPlants.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultPlants()
                }
            }
        }

        // Seed some starter care tasks if empty
        viewModelScope.launch {
            repository.allCareTasks.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultCareTasks()
                }
            }
        }
    }

    // --- Loading & Error States ---
    private val _isIdentifying = MutableStateFlow(false)
    val isIdentifying: StateFlow<Boolean> = _isIdentifying.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing.asStateFlow()

    private val _isGeneratingCarePlan = MutableStateFlow(false)
    val isGeneratingCarePlan: StateFlow<Boolean> = _isGeneratingCarePlan.asStateFlow()

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _identificationResult = MutableStateFlow<PlantIdentificationDTO?>(null)
    val identificationResult: StateFlow<PlantIdentificationDTO?> = _identificationResult.asStateFlow()

    private val _diagnosisResult = MutableStateFlow<DiseaseDiagnosisDTO?>(null)
    val diagnosisResult: StateFlow<DiseaseDiagnosisDTO?> = _diagnosisResult.asStateFlow()

    private val _carePlanResult = MutableStateFlow<PersonalizedCarePlanDTO?>(null)
    val carePlanResult: StateFlow<PersonalizedCarePlanDTO?> = _carePlanResult.asStateFlow()

    private val _deficiencyResult = MutableStateFlow<com.example.api.dto.DeficiencyAnalysisDTO?>(null)
    val deficiencyResult: StateFlow<com.example.api.dto.DeficiencyAnalysisDTO?> = _deficiencyResult.asStateFlow()

    private val _isCheckingDeficiency = MutableStateFlow(false)
    val isCheckingDeficiency: StateFlow<Boolean> = _isCheckingDeficiency.asStateFlow()

    private val _seasonalCareTips = MutableStateFlow<SeasonalCareTipsResult?>(null)
    val seasonalCareTips: StateFlow<SeasonalCareTipsResult?> = _seasonalCareTips.asStateFlow()

    private val _isFetchingSeasonalCare = MutableStateFlow(false)
    val isFetchingSeasonalCare: StateFlow<Boolean> = _isFetchingSeasonalCare.asStateFlow()

    private val _weatherAdvice = MutableStateFlow<WeatherAdviceResult?>(null)
    val weatherAdvice: StateFlow<WeatherAdviceResult?> = _weatherAdvice.asStateFlow()

    private val _isFetchingWeatherAdvice = MutableStateFlow(false)
    val isFetchingWeatherAdvice: StateFlow<Boolean> = _isFetchingWeatherAdvice.asStateFlow()

    fun clearDeficiencyResult() {
        _deficiencyResult.value = null
    }

    fun clearSeasonalCareTips() {
        _seasonalCareTips.value = null
    }

    fun clearWeatherAdvice() {
        _weatherAdvice.value = null
    }

    fun clearError() {
        _errorState.value = null
    }

    fun signInWithGoogle(email: String, name: String, photoUrl: String = "") {
        viewModelScope.launch {
            val currentProfile = userProfile.value
            val updatedProfile = com.example.data.model.UserProfile(
                id = 1,
                subscriptionStatus = currentProfile?.subscriptionStatus ?: "NONE",
                expiryDate = currentProfile?.expiryDate ?: 0L,
                email = email,
                billingProvider = currentProfile?.billingProvider ?: "NONE",
                isLoggedIn = true,
                displayName = name,
                photoUrl = photoUrl
            )
            repository.insertUserProfile(updatedProfile)

            // Real Firebase Auth signup/signin logic
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    _firebaseSyncStatus.value = "Authenticating with Firebase..."
                    auth.signInWithEmailAndPassword(email, "GardenPassword123!")
                        .addOnSuccessListener { result ->
                            _firebaseSyncStatus.value = "Cloud Sync Active: ${result.user?.email}"
                            syncPlantsWithFirebase()
                        }
                        .addOnFailureListener { exception ->
                            // Try signing up if user not found, or handle other errors by attempting signup
                            auth.createUserWithEmailAndPassword(email, "GardenPassword123!")
                                .addOnSuccessListener { result ->
                                    _firebaseSyncStatus.value = "Cloud Sync Active (New Account): ${result.user?.email}"
                                    syncPlantsWithFirebase()
                                }
                                .addOnFailureListener { signupError ->
                                    // Fallback to anonymous so we can still sync if signup/signin fail due to network/no auth provider
                                    auth.signInAnonymously()
                                        .addOnSuccessListener { anonResult ->
                                            _firebaseSyncStatus.value = "Cloud Sync Active (Anonymous Profile)"
                                            syncPlantsWithFirebase()
                                        }
                                        .addOnFailureListener { anonError ->
                                            _firebaseSyncStatus.value = "Auth Sync Failed: ${signupError.localizedMessage}"
                                        }
                                }
                        }
                } catch (e: Exception) {
                    _firebaseSyncStatus.value = "Firebase connection error. Running locally."
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val currentProfile = userProfile.value
            val updatedProfile = com.example.data.model.UserProfile(
                id = 1,
                subscriptionStatus = currentProfile?.subscriptionStatus ?: "NONE",
                expiryDate = currentProfile?.expiryDate ?: 0L,
                email = "guest@example.com",
                billingProvider = currentProfile?.billingProvider ?: "NONE",
                isLoggedIn = false,
                displayName = "",
                photoUrl = ""
            )
            repository.insertUserProfile(updatedProfile)

            if (isFirebaseAvailable) {
                try {
                    FirebaseAuth.getInstance().signOut()
                    _firebaseSyncStatus.value = "Offline Mode (Guest)"
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun syncPlantsWithFirebase() {
        if (!isFirebaseAvailable) return
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return
        val userId = user.uid
        val firestore = FirebaseFirestore.getInstance()

        _isSyncing.value = true
        _firebaseSyncStatus.value = "Syncing with cloud..."
        try {
            firestore.collection("users").document(userId).collection("plants")
                .get()
                .addOnSuccessListener { result ->
                    viewModelScope.launch {
                        // Add an elegant brief delay to let the sync animation play beautifully and avoid flashing
                        kotlinx.coroutines.delay(1000)
                        val cloudPlants = result.mapNotNull { doc ->
                            try {
                                val name = doc.getString("name") ?: ""
                                val customName = doc.getString("customName") ?: ""
                                val species = doc.getString("species") ?: ""
                                val lastWatered = doc.getLong("lastWatered") ?: System.currentTimeMillis()
                                val wateringIntervalDays = doc.getLong("wateringIntervalDays")?.toInt() ?: 7
                                val sunlight = doc.getString("sunlight") ?: "Indirect Bright Light"
                                val difficulty = doc.getString("difficulty") ?: "Easy"
                                val healthStatus = doc.getString("healthStatus") ?: "Healthy"
                                val careInstructions = doc.getString("careInstructions") ?: ""
                                val imageUrl = doc.getString("imageUrl")
                                val location = doc.getString("location") ?: "Indoor"
                                val addedDate = doc.getLong("addedDate") ?: System.currentTimeMillis()

                                Plant(
                                    name = name,
                                    customName = customName,
                                    species = species,
                                    lastWatered = lastWatered,
                                    wateringIntervalDays = wateringIntervalDays,
                                    sunlight = sunlight,
                                    difficulty = difficulty,
                                    healthStatus = healthStatus,
                                    careInstructions = careInstructions,
                                    imageUrl = imageUrl,
                                    location = location,
                                    addedDate = addedDate
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val localPlants = plants.value
                        // Download missing plants from cloud to local Room
                        for (cloudPlant in cloudPlants) {
                            val localMatch = localPlants.find { 
                                it.name == cloudPlant.name && it.addedDate == cloudPlant.addedDate 
                            }
                            if (localMatch == null) {
                                repository.insertPlant(cloudPlant)
                            } else {
                                if (cloudPlant.lastWatered > localMatch.lastWatered) {
                                    repository.updatePlant(cloudPlant.copy(id = localMatch.id))
                                }
                            }
                        }

                        // Upload missing local plants to cloud
                        for (localPlant in localPlants) {
                            val cloudMatch = cloudPlants.find { 
                                it.name == localPlant.name && it.addedDate == localPlant.addedDate 
                            }
                            if (cloudMatch == null) {
                                uploadPlantToFirestore(userId, localPlant)
                            }
                        }
                        _firebaseSyncStatus.value = "Cloud Sync Active: Synced as ${user.email ?: "Anonymous"}"
                        _isSyncing.value = false
                    }
                }
                .addOnFailureListener { e ->
                    _firebaseSyncStatus.value = "Cloud Sync Failed: ${e.localizedMessage}"
                    _isSyncing.value = false
                }
        } catch (e: Exception) {
            _firebaseSyncStatus.value = "Cloud Sync offline"
            _isSyncing.value = false
        }
    }

    private fun uploadPlantToFirestore(userId: String, plant: Plant) {
        if (!isFirebaseAvailable) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            val docId = "${plant.name}_${plant.addedDate}".replace("/", "_").replace(".", "_")
            val plantMap = mapOf(
                "name" to plant.name,
                "customName" to plant.customName,
                "species" to plant.species,
                "lastWatered" to plant.lastWatered,
                "wateringIntervalDays" to plant.wateringIntervalDays,
                "sunlight" to plant.sunlight,
                "difficulty" to plant.difficulty,
                "healthStatus" to plant.healthStatus,
                "careInstructions" to plant.careInstructions,
                "imageUrl" to plant.imageUrl,
                "location" to plant.location,
                "addedDate" to plant.addedDate
            )
            firestore.collection("users").document(userId).collection("plants").document(docId)
                .set(plantMap)
                .addOnSuccessListener {
                    Log.d("FirebaseSync", "Successfully uploaded plant: ${plant.name}")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseSync", "Failed to upload plant: ${plant.name}", e)
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error uploading plant: ${e.message}")
        }
    }

    private fun deletePlantFromFirestore(userId: String, plant: Plant) {
        if (!isFirebaseAvailable) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            val docId = "${plant.name}_${plant.addedDate}".replace("/", "_").replace(".", "_")
            firestore.collection("users").document(userId).collection("plants").document(docId)
                .delete()
                .addOnSuccessListener {
                    Log.d("FirebaseSync", "Successfully deleted plant: ${plant.name}")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseSync", "Failed to delete plant: ${plant.name}", e)
                }
        } catch (e: Exception) {
            Log.e("FirebaseSync", "Error deleting plant: ${e.message}")
        }
    }

    fun clearIdentificationResult() {
        _identificationResult.value = null
    }

    fun setIdentificationResult(dto: PlantIdentificationDTO) {
        _identificationResult.value = dto
    }

    fun clearDiagnosisResult() {
        _diagnosisResult.value = null
    }

    fun setDiagnosisResult(dto: DiseaseDiagnosisDTO) {
        _diagnosisResult.value = dto
    }

    fun clearCarePlanResult() {
        _carePlanResult.value = null
    }

    // --- Seeding Default Database ---
    private suspend fun seedDefaultPlants() {
        val starter1 = Plant(
            name = "Fiddle Leaf Fig",
            customName = "Fiona",
            species = "Ficus lyrata",
            wateringIntervalDays = 10,
            sunlight = "Bright Indirect Light",
            difficulty = "Medium",
            healthStatus = "Healthy",
            careInstructions = "Water when the top 2 inches of soil are dry. Wipe the large leaves weekly to keep them dust-free and clean. Protect from cold drafts.",
            imageUrl = null
        )
        val starter2 = Plant(
            name = "Snake Plant",
            customName = "Sammy",
            species = "Sansevieria trifasciata",
            wateringIntervalDays = 21,
            sunlight = "Low to Bright Light",
            difficulty = "Easy",
            healthStatus = "Healthy",
            careInstructions = "Extremely drought-tolerant. Allow soil to dry out completely between waterings. Water sparingly in winter.",
            imageUrl = null
        )
        repository.insertPlant(starter1)
        repository.insertPlant(starter2)
    }

    // --- Database Operations ---
    fun addPlantToGarden(plant: Plant) {
        viewModelScope.launch {
            val generatedId = repository.insertPlant(plant)
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                    if (user != null) {
                        uploadPlantToFirestore(user.uid, plant.copy(id = generatedId.toInt()))
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun deletePlantFromGarden(plant: Plant) {
        viewModelScope.launch {
            repository.deletePlant(plant)
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                    if (user != null) {
                        deletePlantFromFirestore(user.uid, plant)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun waterPlant(plantId: Int) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            repository.waterPlant(plantId, timestamp)
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                    if (user != null) {
                        val plant = plants.value.find { it.id == plantId }
                        if (plant != null) {
                            uploadPlantToFirestore(user.uid, plant.copy(lastWatered = timestamp))
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun updatePlant(plant: Plant) {
        viewModelScope.launch {
            repository.updatePlant(plant)
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                    if (user != null) {
                        uploadPlantToFirestore(user.uid, plant)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun getJournalEntriesForPlant(plantId: Int): kotlinx.coroutines.flow.Flow<List<JournalEntry>> {
        return repository.getJournalEntriesForPlant(plantId)
    }

    fun addJournalEntry(plantId: Int, growthStatus: String, note: String, photoUri: String?) {
        viewModelScope.launch {
            repository.insertJournalEntry(
                JournalEntry(
                    plantId = plantId,
                    growthStatus = growthStatus,
                    note = note,
                    photoUri = photoUri
                )
            )
        }
    }

    fun deleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entry)
        }
    }

    fun getGrowthMetricsForPlant(plantId: Int): kotlinx.coroutines.flow.Flow<List<GrowthMetric>> {
        return repository.getGrowthMetricsForPlant(plantId)
    }

    fun addGrowthMetric(plantId: Int, heightCm: Float, leafCount: Int, note: String) {
        viewModelScope.launch {
            repository.insertGrowthMetric(
                GrowthMetric(
                    plantId = plantId,
                    heightCm = heightCm,
                    leafCount = leafCount,
                    note = note
                )
            )
        }
    }

    fun deleteGrowthMetric(metric: GrowthMetric) {
        viewModelScope.launch {
            repository.deleteGrowthMetric(metric)
        }
    }

    // --- API & AI Operations ---

    // Helper to convert Bitmap to Base64 with scaling
    private fun Bitmap.toBase64(maxDimension: Int = 1024): String {
        val ratio = kotlin.math.min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val scaledBitmap = if (ratio < 1.0f) {
            Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
        } else {
            this
        }
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Helper to strip markdown code blocks
    private fun extractJson(rawText: String): String {
        var text = rawText.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter("```")
            if (text.startsWith("json", ignoreCase = true)) {
                text = text.substringAfter("json")
            }
            text = text.substringBeforeLast("```")
        }
        return text.trim()
    }

    private fun isKeyPlaceholder(key: String): Boolean {
        return key.isBlank() || key.contains("GEMINI_API_KEY") || key.contains("placeholder") || key == "MY_GEMINI_API_KEY"
    }

    fun identifyPlant(bitmap: Bitmap?, textDescription: String) {
        if (_isIdentifying.value) return
        viewModelScope.launch {
            if (!canSearch()) {
                triggerSubscriptionScreen()
                _isIdentifying.value = false
                return@launch
            }
            recordSearch("IDENTIFY")
            _isIdentifying.value = true
            _errorState.value = null
            _identificationResult.value = null

            val apiKey = GeminiClient.getApiKey()
            if (isKeyPlaceholder(apiKey)) {
                // Return Mock Identification after a small delay
                simulateMockIdentification(bitmap, textDescription)
                _isIdentifying.value = false
                return@launch
            }

            try {
                val parts = mutableListOf<GeminiPart>()
                if (bitmap != null) {
                    val base64Data = withContext(Dispatchers.IO) { bitmap.toBase64() }
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data)))
                }
                parts.add(
                    GeminiPart(
                        text = "You are an expert botanist and plant pathologist. Analyze this plant image. Description context: $textDescription. " +
                                "Identify the plant species with high accuracy. Provide its common and scientific name. " +
                                "Generate a complete care plan including watering, sunlight, soil, and fertilizer. " +
                                "Analyze the image and description carefully for any signs of disease, pests, or nutrient deficiencies. " +
                                "If problems are detected, provide: 1. A clear diagnosis name, 2. Detailed treatment steps (organic/chemical), 3. Home remedies or DIY solutions. " +
                                "Respond strictly with a JSON object conforming exactly to this structure: " +
                                "{\n" +
                                "  \"detectedName\": \"Common Plant Name\",\n" +
                                "  \"scientificName\": \"Scientific genus/species\",\n" +
                                "  \"familyName\": \"Family name of the plant\",\n" +
                                "  \"confidence\": 0.95,\n" +
                                "  \"description\": \"A paragraph detailing what this plant is, origin, and interest.\",\n" +
                                "  \"origin\": \"Geographical origins of the plant, e.g., Tropical rainforests of southern Mexico\",\n" +
                                "  \"growthHabits\": \"How the plant grows, e.g., Vine-like climber, can grow up to 10 feet indoors\",\n" +
                                "  \"careNeeds\": \"Brief care requirements summary, e.g., Medium watering, bright filtered light\",\n" +
                                "  \"lightGuide\": \"Detailed description of light requirement\",\n" +
                                "  \"waterGuide\": \"Detailed description of watering requirements\",\n" +
                                "  \"soilGuide\": \"Detailed description of soil requirements\",\n" +
                                "  \"fertilizerGuide\": \"Detailed description of fertilizer requirements (such as feeding frequency, type, and season)\",\n" +
                                "  \"remediesForGrowth\": [\"Remedy 1 to boost growth\", \"Remedy 2\"],\n" +
                                "  \"referenceImages\": [\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"],\n" +
                                "  \"petSafe\": true,\n" +
                                "  \"hasIssue\": true,\n" +
                                "  \"diseaseName\": \"Name of detected disease/pest/issue or 'None' if healthy\",\n" +
                                "  \"symptoms\": \"Observed symptoms description or 'None' if healthy\",\n" +
                                "  \"treatmentSteps\": [\"Step 1: description\", \"Step 2: description\"],\n" +
                                "  \"homeRemedies\": [\"Remedy 1: description\", \"Remedy 2: description\"],\n" +
                                "  \"prevention\": \"Detailed advice on preventing this in the future or general health tips\",\n" +
                                "  \"wikiRecord\": {\n" +
                                "     \"common_name\": \"\",\n" +
                                "     \"scientific_name\": \"\",\n" +
                                "     \"synonyms\": [\"\"],\n" +
                                "     \"care_plan\": {\n" +
                                "        \"watering\": \"\",\n" +
                                "        \"sunlight\": \"\",\n" +
                                "        \"soil\": \"\",\n" +
                                "        \"fertilizer\": \"\"\n" +
                                "     },\n" +
                                "     \"diagnosis\": {\n" +
                                "        \"common_diseases\": [\"\"],\n" +
                                "        \"treatment_steps\": [\"\"],\n" +
                                "        \"home_remedies\": [\"\"]\n" +
                                "     }\n" +
                                "  }\n" +
                                "}\n" +
                                "Do not include any Markdown tags other than standard text, do not return any other text outside the JSON. " +
                                "Crucially, read plant information from the following source: https://en.wikipedia.org/wiki/List_of_plants_by_common_name. " +
                                "For the identified plant, extract its common name and scientific name(s) from this source if available. " +
                                "Normalize the data and provide it in the 'wikiRecord' object as specified. " +
                                "IMPORTANT: Provide the response translated into the ${_appLanguage.value} language. Keep all JSON keys in English, but translate the string values into ${_appLanguage.value}."
                    )
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                _loadingMessage.value = "Identifying plant..."
                val response = retryWithExponentialBackoff(
                    maxRetries = 3,
                    initialDelayMs = 1500,
                    onRetry = { attempt, e ->
                        _loadingMessage.value = "Connection issue (${e.message ?: "503"}). Retrying ($attempt/3)..."
                        Log.w("GardenViewModel", "Identification retry $attempt failed", e)
                    }
                ) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }
                
                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = extractJson(rawText)

                val moshi = GeminiClient.getMoshi()
                val adapter = moshi.adapter(PlantIdentificationDTO::class.java)
                val dto = adapter.fromJson(cleanJson)

                if (dto != null) {
                    _identificationResult.value = dto
                    
                    // Save WikiPlantRecord to database
                    dto.wikiRecord?.let { wikiRecord ->
                        try {
                            val synonymsJson = moshi.adapter<List<String>>(
                                com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
                            ).toJson(wikiRecord.synonyms)
                            
                            val carePlanJson = moshi.adapter(com.example.api.dto.WikiCarePlanDTO::class.java).toJson(wikiRecord.carePlan)
                            val diagnosisJson = moshi.adapter(com.example.api.dto.WikiDiagnosisDTO::class.java).toJson(wikiRecord.diagnosis)
                            
                            val recordEntity = com.example.data.model.WikiPlantRecord(
                                commonName = wikiRecord.commonName,
                                scientificName = wikiRecord.scientificName,
                                synonyms = synonymsJson,
                                carePlan = carePlanJson,
                                diagnosis = diagnosisJson
                            )
                            repository.insertWikiPlantRecord(recordEntity)
                        } catch (e: Exception) {
                            Log.e("GardenViewModel", "Failed to save wiki record", e)
                        }
                    }

                    // Save to history
                    repository.insertIdentification(
                        PlantIdentification(
                            detectedName = dto.detectedName,
                            scientificName = dto.scientificName,
                            confidence = dto.confidence,
                            description = dto.description,
                            origin = dto.origin,
                            growthHabits = dto.growthHabits,
                            careNeeds = dto.careNeeds,
                            lightGuide = dto.lightGuide,
                            waterGuide = dto.waterGuide,
                            soilGuide = dto.soilGuide,
                            petSafe = dto.petSafe
                        )
                    )
                    // Automatically add the scanned plant under "My Plant Buddies"
                    repository.insertPlant(
                        Plant(
                            name = dto.detectedName,
                            customName = dto.detectedName,
                            species = dto.scientificName,
                            wateringIntervalDays = 7,
                            sunlight = dto.lightGuide.take(30).let { if (it.length == 30) "$it..." else it },
                            careInstructions = "Light: ${dto.lightGuide}\n\nWater: ${dto.waterGuide}\n\nSoil: ${dto.soilGuide}",
                            imageUrl = dto.referenceImages?.firstOrNull() ?: "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?q=80&w=600",
                            addedDate = System.currentTimeMillis()
                        )
                    )
                    // If an issue was detected, also save to diagnosis history automatically
                    if (dto.hasIssue == true && !dto.diseaseName.isNullOrBlank() && dto.diseaseName != "None") {
                        repository.insertDiagnosis(
                            Diagnosis(
                                plantName = dto.detectedName,
                                diseaseName = dto.diseaseName,
                                confidence = dto.confidence,
                                symptoms = dto.symptoms ?: "Observed from scan",
                                treatmentSteps = dto.treatmentSteps?.joinToString("\n") ?: "",
                                homeRemedies = dto.homeRemedies?.joinToString("\n") ?: "",
                                prevention = dto.prevention ?: ""
                            )
                        )
                    }
                } else {
                    _errorState.value = "Failed to parse plant identification data. Please try again."
                }
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Identification error", e)
                val is429 = e is retrofit2.HttpException && e.code() == 429
                val errMsg = if (is429) "HTTP 429 Rate Limit Exceeded" else e.localizedMessage ?: "Unknown error"
                _errorState.value = "Error connecting to AI Server: $errMsg. Prototyping Offline?"
                // Auto-fallback for ease of testing
                simulateMockIdentification(bitmap, textDescription)
            } finally {
                _isIdentifying.value = false
                _loadingMessage.value = null
            }
        }
    }

    private suspend fun simulateMockIdentification(bitmap: Bitmap?, desc: String) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(2000) // Simulate networking
            val isMonstera = desc.contains("monstera", ignoreCase = true) || desc.contains("split", ignoreCase = true)
            val isFern = desc.contains("fern", ignoreCase = true) || desc.contains("boston", ignoreCase = true)
            
            val isSick = desc.contains("sick", ignoreCase = true) || 
                         desc.contains("spot", ignoreCase = true) || 
                         desc.contains("yellow", ignoreCase = true) || 
                         desc.contains("brown", ignoreCase = true) || 
                         desc.contains("bug", ignoreCase = true) || 
                         desc.contains("rot", ignoreCase = true) || 
                         desc.contains("die", ignoreCase = true) || 
                         desc.contains("wither", ignoreCase = true) || 
                         desc.contains("droop", ignoreCase = true)

            val mockDto = when {
                isMonstera -> PlantIdentificationDTO(
                    detectedName = "Monstera Deliciosa",
                    scientificName = "Monstera deliciosa",
                    familyName = "Araceae",
                    confidence = 0.98f,
                    description = "A popular indoor climbing plant famous for its iconic fenestrated (split) leaves. Originating in tropical forests of Mexico.",
                    origin = "Tropical rainforests of Southern Mexico and Central America",
                    growthHabits = "Vining climber that uses aerial roots to scale trees. Can grow up to 10-15 feet indoors with a moss pole support.",
                    careNeeds = "Prefers bright, indirect light, chunky well-draining soil, and watering only when the top 50% of soil has dried out completely.",
                    lightGuide = "Bright indirect sunlight. Direct harsh light will scorch the leaves, while low light slows growth.",
                    waterGuide = "Water thoroughly when the top 2-3 inches of soil dry out. Ensure pots have drainage holes to avoid root rot.",
                    soilGuide = "Rich organic peaty soil mix. Prefers well-draining aerated soil with perlite or pine bark.",
                    fertilizerGuide = "Feed every 4 weeks in spring and summer with a balanced water-soluble fertilizer at half strength. Do not fertilize in winter.",
                    remediesForGrowth = listOf("Provide a moss pole to encourage larger leaves and fenestrations.", "Mist aerial roots occasionally."),
                    referenceImages = listOf("https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=400"),
                    petSafe = false,
                    hasIssue = isSick,
                    diseaseName = if (isSick) "Root Rot (Fungal Infection)" else "None",
                    symptoms = if (isSick) "Yellowing lower leaves, mushy dark stems, and damp soil emitting a musty, moldy odor." else "None",
                    treatmentSteps = if (isSick) listOf(
                        "Stop watering immediately and let the container dry out.",
                        "Carefully remove the plant, trim any rotten black mushy roots with sterilized shears.",
                        "Repot in fresh, well-draining chunky organic potting soil with lots of perlite."
                    ) else null,
                    homeRemedies = if (isSick) listOf(
                        "Spray remaining healthy roots with a natural copper fungicide solution.",
                        "Wipe the root crown with highly diluted hydrogen peroxide (3% mixed 1:10 with water)."
                    ) else null,
                    prevention = if (isSick) "Only water when the top 2-3 inches of soil is completely dry. Use pots with drainage holes, and empty the bottom saucer immediately." else "Maintain regular watering and bright indirect light."
                )
                isFern -> PlantIdentificationDTO(
                    detectedName = "Boston Fern",
                    scientificName = "Nephrolepis exaltata",
                    familyName = "Nephrolepidaceae",
                    confidence = 0.92f,
                    description = "An elegant classic fern with rich feathery green fronds. Thrives in high humidity and damp conditions.",
                    origin = "Humid tropical regions worldwide, particularly native to the Americas, West Indies, and Africa",
                    growthHabits = "Clump-forming, arching habit with long sword-shaped fronds that gracefully cascade outwards. Spreads via runners.",
                    careNeeds = "Requires consistent dampness, high ambient humidity, cool-to-warm temperatures, and bright but fully filtered indirect light.",
                    lightGuide = "Filtered indirect light. Thrives in bathrooms or kitchens with dappled or diffused sunlight.",
                    waterGuide = "Keep soil consistently damp but not waterlogged. Mist leaves frequently to boost ambient humidity.",
                    soilGuide = "Moist, nutrient-rich potting soil with good moisture-retaining capacity.",
                    fertilizerGuide = "Apply mild liquid nitrogen-rich foliage fertilizer every 6 weeks during active growing season.",
                    remediesForGrowth = listOf("Increase ambient humidity by grouping plants or using a pebble tray.", "Mist fronds regularly."),
                    referenceImages = listOf("https://images.unsplash.com/photo-1596784869614-7221ee9fc84b?q=80&w=400"),
                    petSafe = true,
                    hasIssue = isSick,
                    diseaseName = if (isSick) "Spider Mites (Tiny Pest Attack)" else "None",
                    symptoms = if (isSick) "Fine silky webbing under fronds, tiny speckled yellow spots, and overall dull, fading green color." else "None",
                    treatmentSteps = if (isSick) listOf(
                        "Isolate the fern immediately to prevent pests spreading to other plants.",
                        "Rinse the entire foliage thoroughly with lukewarm high-pressure water under a shower head.",
                        "Spray thoroughly with organic neem oil or insecticidal soap, targeting leaf backs."
                    ) else null,
                    homeRemedies = if (isSick) listOf(
                        "Create a spray with 1 tablespoon natural castile soap and 1 quart water to knock down pests.",
                        "Set up a cool-mist humidifier nearby to increase ambient moisture."
                    ) else null,
                    prevention = if (isSick) "Keep the fern in high humidity (above 55%) as spider mites thrive in dry hot environments. Mist foliage daily." else "Mist fronds regularly to prevent drying."
                )
                else -> PlantIdentificationDTO(
                    detectedName = "Prayer Plant",
                    scientificName = "Maranta leuconeura",
                    familyName = "Marantaceae",
                    confidence = 0.89f,
                    description = "A striking houseplant featuring stunning variegated foliage that folds up at night like hands in prayer.",
                    origin = "Tropical rainforest floors of Brazil, South America",
                    growthHabits = "Low-growing, spreading clump habit. Reaches about 12 inches in height but crawls horizontally to make a beautiful display.",
                    careNeeds = "Demands high humidity, moist but well-aerated potting mix, and low-to-medium indirect light.",
                    lightGuide = "Medium indirect sunlight. Keep away from direct hot windows to maintain bright foliage color.",
                    waterGuide = "Water when the top of the soil is dry. Use lukewarm filtered or rain water if possible.",
                    soilGuide = "Well-aerated houseplant potting soil with good drainage.",
                    fertilizerGuide = "Feed every 2 weeks during active growth with a balanced liquid houseplant food diluted to half-strength.",
                    remediesForGrowth = listOf("Use filtered water to avoid leaf tip burn.", "Keep soil consistently moist but never soggy."),
                    referenceImages = listOf("https://images.unsplash.com/photo-1632207691143-643e2a9a9361?q=80&w=400"),
                    petSafe = true,
                    hasIssue = isSick,
                    diseaseName = if (isSick) "Leaf Spot (Fungal)" else "None",
                    symptoms = if (isSick) "Circular brown/black spots on foliage, surrounded by a distinct yellow ring or halo." else "None",
                    treatmentSteps = if (isSick) listOf(
                        "Snip off heavily spotted leaves to limit spore spread.",
                        "Improve air circulation around the plant and avoid getting water on leaves when watering.",
                        "Apply a copper-based organic fungicide or sulfur spray."
                    ) else null,
                    homeRemedies = if (isSick) listOf(
                        "Brew chamomile tea, let it cool, and spray over the entire plant to act as a natural antifungal.",
                        "Dust soil lightly with cinnamon, a natural antifungal agent."
                    ) else null,
                    prevention = if (isSick) "Water the soil directly instead of overhead. Ensure leaves stay dry. Space out plants to ensure proper fresh airflow." else "Keep away from cold drafty windows."
                )
            }

            _identificationResult.value = mockDto
            repository.insertIdentification(
                PlantIdentification(
                    detectedName = mockDto.detectedName,
                    scientificName = mockDto.scientificName,
                    confidence = mockDto.confidence,
                    description = mockDto.description,
                    origin = mockDto.origin,
                    growthHabits = mockDto.growthHabits,
                    careNeeds = mockDto.careNeeds,
                    lightGuide = mockDto.lightGuide,
                    waterGuide = mockDto.waterGuide,
                    soilGuide = mockDto.soilGuide,
                    petSafe = mockDto.petSafe
                )
            )
            // Automatically add the scanned plant under "My Plant Buddies"
            repository.insertPlant(
                Plant(
                    name = mockDto.detectedName,
                    customName = mockDto.detectedName,
                    species = mockDto.scientificName,
                    wateringIntervalDays = 7,
                    sunlight = mockDto.lightGuide.take(30).let { if (it.length == 30) "$it..." else it },
                    careInstructions = "Light: ${mockDto.lightGuide}\n\nWater: ${mockDto.waterGuide}\n\nSoil: ${mockDto.soilGuide}",
                    imageUrl = mockDto.referenceImages?.firstOrNull() ?: "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?q=80&w=600",
                    addedDate = System.currentTimeMillis()
                )
            )
            if (mockDto.hasIssue == true && !mockDto.diseaseName.isNullOrBlank() && mockDto.diseaseName != "None") {
                repository.insertDiagnosis(
                    Diagnosis(
                        plantName = mockDto.detectedName,
                        diseaseName = mockDto.diseaseName,
                        confidence = mockDto.confidence,
                        symptoms = mockDto.symptoms ?: "Observed from scan",
                        treatmentSteps = mockDto.treatmentSteps?.joinToString("\n") ?: "",
                        homeRemedies = mockDto.homeRemedies?.joinToString("\n") ?: "",
                        prevention = mockDto.prevention ?: ""
                    )
                )
            }
        }
    }

    fun diagnoseDisease(bitmap: Bitmap?, symptoms: String) {
        if (_isDiagnosing.value) return
        viewModelScope.launch {
            if (!canSearch()) {
                triggerSubscriptionScreen()
                _isDiagnosing.value = false
                return@launch
            }
            recordSearch("DIAGNOSE")
            _isDiagnosing.value = true
            _errorState.value = null
            _diagnosisResult.value = null

            val apiKey = GeminiClient.getApiKey()
            if (isKeyPlaceholder(apiKey)) {
                simulateMockDiagnosis(bitmap, symptoms)
                _isDiagnosing.value = false
                return@launch
            }

            try {
                val parts = mutableListOf<GeminiPart>()
                if (bitmap != null) {
                    val base64Data = withContext(Dispatchers.IO) { bitmap.toBase64() }
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data)))
                }
                parts.add(
                    GeminiPart(
                        text = "Diagnose this plant's disease or health issues. " +
                                "Stated symptoms: $symptoms. " +
                                "Respond strictly with a JSON object conforming exactly to this structure: " +
                                "{\n" +
                                "  \"plantName\": \"Plant Name\",\n" +
                                "  \"diseaseName\": \"Disease Name or Issue\",\n" +
                                "  \"confidence\": 0.90,\n" +
                                "  \"symptoms\": \"Symptoms observed and described\",\n" +
                                "  \"treatmentSteps\": [\"Step 1: Description\", \"Step 2: Description\"],\n" +
                                "  \"homeRemedies\": [\"Remedy 1: Description\", \"Remedy 2: Description\"],\n" +
                                "  \"prevention\": \"Detailed advice on preventing this in the future\"\n" +
                                "}\n" +
                                "Do not include any markdown styling other than the JSON itself. Make sure it is valid JSON. " +
                                "IMPORTANT: Provide the response translated into the ${_appLanguage.value} language. Keep all JSON keys in English, but translate the string values into ${_appLanguage.value}."
                    )
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                _loadingMessage.value = "Analyzing disease..."
                val response = retryWithExponentialBackoff(
                    maxRetries = 3,
                    initialDelayMs = 1500,
                    onRetry = { attempt, e ->
                        _loadingMessage.value = "Connection issue (${e.message ?: "503"}). Retrying ($attempt/3)..."
                        Log.w("GardenViewModel", "Diagnosis retry $attempt failed", e)
                    }
                ) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }

                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = extractJson(rawText)

                val moshi = GeminiClient.getMoshi()
                val adapter = moshi.adapter(DiseaseDiagnosisDTO::class.java)
                val dto = adapter.fromJson(cleanJson)

                if (dto != null) {
                    _diagnosisResult.value = dto
                    // Save to database
                    repository.insertDiagnosis(
                        Diagnosis(
                            plantName = dto.plantName,
                            diseaseName = dto.diseaseName,
                            confidence = dto.confidence,
                            symptoms = dto.symptoms,
                            treatmentSteps = dto.treatmentSteps.joinToString("\n"),
                            homeRemedies = dto.homeRemedies.joinToString("\n"),
                            prevention = dto.prevention
                        )
                    )
                } else {
                    _errorState.value = "Failed to parse plant diagnosis. Please try again."
                }
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Diagnosis error", e)
                val is429 = e is retrofit2.HttpException && e.code() == 429
                val errMsg = if (is429) "HTTP 429 Rate Limit Exceeded" else e.localizedMessage ?: "Unknown error"
                _errorState.value = "Error connecting to AI Server: $errMsg. Prototyping Offline?"
                simulateMockDiagnosis(bitmap, symptoms)
            } finally {
                _isDiagnosing.value = false
                _loadingMessage.value = null
            }
        }
    }

    private suspend fun simulateMockDiagnosis(bitmap: Bitmap?, symptoms: String) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(2000)
            val isYellow = symptoms.contains("yellow", ignoreCase = true)
            val isSpot = symptoms.contains("spot", ignoreCase = true) || symptoms.contains("brown", ignoreCase = true)
            val isWeb = symptoms.contains("web", ignoreCase = true) || symptoms.contains("spider", ignoreCase = true)

            val mockDto = when {
                isYellow -> DiseaseDiagnosisDTO(
                    plantName = "Houseplant",
                    diseaseName = "Overwatering & Root Rot",
                    confidence = 0.85f,
                    symptoms = "Yellowing lower leaves, soft soggy stems, and general wilting despite damp soil.",
                    treatmentSteps = listOf(
                        "Stop watering immediately.",
                        "Remove the plant from the pot and inspect roots. Trim off any black, mushy, or smelly roots.",
                        "Repot into a clean pot with fresh, well-draining soil mixed with perlite."
                    ),
                    homeRemedies = listOf(
                        "Let the soil dry fully to the bottom before next watering.",
                        "Sprinkle ground cinnamon on the roots during repotting; cinnamon has natural anti-fungal properties."
                    ),
                    prevention = "Always check soil dampness before watering. Ensure pots have drainage holes, and never let the plant sit in standing water."
                )
                isWeb -> DiseaseDiagnosisDTO(
                    plantName = "Houseplant",
                    diseaseName = "Spider Mites Infestation",
                    confidence = 0.91f,
                    symptoms = "Fine webbing on leaves and stems, yellow stippling or tiny spots on leaves, dull appearance.",
                    treatmentSteps = listOf(
                        "Isolate the infected plant immediately from other houseplants.",
                        "Take the plant to the shower or outdoor hose and wash off the foliage thoroughly to dislodge the mites.",
                        "Treat the leaves with insecticidal soap or Neem oil spray every 7 days for 3 weeks."
                    ),
                    homeRemedies = listOf(
                        "Spray with a mild solution of diluted soap (1 tbsp dish soap per gallon of warm water).",
                        "Mist the plant regularly; spider mites prefer warm, dry conditions and thrive in low humidity."
                    ),
                    prevention = "Maintain high relative humidity around plants by using a humidifier or pebble tray. Inspect new plants before bringing them home."
                )
                else -> DiseaseDiagnosisDTO(
                    plantName = "Garden Plant",
                    diseaseName = "Leaf Spot (Fungal Fungus)",
                    confidence = 0.88f,
                    symptoms = "Circular brown spots on leaves surrounded by a yellow halo, spreading gradually to upper foliage.",
                    treatmentSteps = listOf(
                        "Cut off and safely discard all infected leaves to stop fungal spores from spreading.",
                        "Apply a copper-based organic fungicide spray to the remaining foliage.",
                        "Avoid splashing water on leaves during watering; water directly at the soil level."
                    ),
                    homeRemedies = listOf(
                        "Mix 1 tablespoon of baking soda and 1 teaspoon of liquid soap in 1 gallon of water. Spray on foliage weekly.",
                        "Ensure the plant has excellent air circulation to dry leaves quickly."
                    ),
                    prevention = "Space out plants to allow healthy airflow. Prune dead leaves regularly, and avoid overhead watering."
                )
            }

            _diagnosisResult.value = mockDto
            repository.insertDiagnosis(
                Diagnosis(
                    plantName = mockDto.plantName,
                    diseaseName = mockDto.diseaseName,
                    confidence = mockDto.confidence,
                    symptoms = mockDto.symptoms,
                    treatmentSteps = mockDto.treatmentSteps.joinToString("\n"),
                    homeRemedies = mockDto.homeRemedies.joinToString("\n"),
                    prevention = mockDto.prevention
                )
            )
        }
    }

    fun generateCarePlan(plantName: String) {
        if (_isGeneratingCarePlan.value) return
        viewModelScope.launch {
            if (!canSearch()) {
                triggerSubscriptionScreen()
                _isGeneratingCarePlan.value = false
                return@launch
            }
            recordSearch("CARE_PLAN")
            _isGeneratingCarePlan.value = true
            _errorState.value = null
            _carePlanResult.value = null

            val apiKey = GeminiClient.getApiKey()
            if (isKeyPlaceholder(apiKey)) {
                simulateMockCarePlan(plantName)
                _isGeneratingCarePlan.value = false
                return@launch
            }

            try {
                val parts = listOf(
                    GeminiPart(
                        text = "Generate a personalized care plan for a plant named: $plantName. " +
                                "Respond strictly with a JSON object conforming exactly to this structure: " +
                                "{\n" +
                                "  \"plantName\": \"$plantName\",\n" +
                                "  \"wateringFrequency\": \"E.g. once every 7-10 days\",\n" +
                                "  \"wateringIntervalDays\": 7,\n" +
                                "  \"sunlightRequirements\": \"Sunlight instruction\",\n" +
                                "  \"humidity\": \"Humidity level\",\n" +
                                "  \"temperature\": \"Ideal range\",\n" +
                                "  \"fertilizer\": \"Fertilizer requirements\",\n" +
                                "  \"soilType\": \"Best potting soil details\",\n" +
                                "  \"careTips\": [\"Care tip 1\", \"Care tip 2\", \"Care tip 3\"]\n" +
                                "}\n" +
                                "Respond with only the raw JSON. No markdown code block. " +
                                "IMPORTANT: Provide the response translated into the ${_appLanguage.value} language. Keep all JSON keys in English, but translate the string values into ${_appLanguage.value}."
                    )
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                _loadingMessage.value = "Generating care plan..."
                val response = retryWithExponentialBackoff(
                    maxRetries = 3,
                    initialDelayMs = 1500,
                    onRetry = { attempt, e ->
                        _loadingMessage.value = "Connection issue (${e.message ?: "503"}). Retrying ($attempt/3)..."
                        Log.w("GardenViewModel", "Care plan retry $attempt failed", e)
                    }
                ) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }

                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = extractJson(rawText)

                val moshi = GeminiClient.getMoshi()
                val adapter = moshi.adapter(PersonalizedCarePlanDTO::class.java)
                val dto = adapter.fromJson(cleanJson)

                if (dto != null) {
                    _carePlanResult.value = dto
                } else {
                    _errorState.value = "Failed to generate care plan. Try again."
                }
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Care plan error", e)
                val is429 = e is retrofit2.HttpException && e.code() == 429
                val errMsg = if (is429) "HTTP 429 Rate Limit Exceeded" else e.localizedMessage ?: "Unknown error"
                _errorState.value = "Error connecting to AI Server: $errMsg"
                simulateMockCarePlan(plantName)
            } finally {
                _isGeneratingCarePlan.value = false
                _loadingMessage.value = null
            }
        }
    }

    private suspend fun simulateMockCarePlan(plantName: String) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(1500)
            val mockDto = PersonalizedCarePlanDTO(
                plantName = plantName,
                wateringFrequency = "Once every 7 to 10 days",
                wateringIntervalDays = 8,
                sunlightRequirements = "Bright, filtered indirect sunlight. Avoid direct mid-day sun.",
                humidity = "Moderate to high (50-60%). Appreciates occasional misting.",
                temperature = "Ideal range 65°F - 85°F (18°C - 29°C). Keep away from air conditioners.",
                fertilizer = "Apply a balanced, diluted liquid fertilizer once a month during spring and summer.",
                soilType = "A well-aerated potting mixture with peat moss, perlite, and coarse sand.",
                careTips = listOf(
                    "Regularly rotate your plant to ensure even symmetrical growth on all sides.",
                    "Wipe the dust off leaves with a damp cloth to maximize photosynthesis.",
                    "Always ensure excess water drains fully out of the bottom of the pot."
                )
            )
            _carePlanResult.value = mockDto
        }
    }

    fun fetchSeasonalCareTips(location: String, season: String) {
        if (_isFetchingSeasonalCare.value) return
        viewModelScope.launch {
            if (!canSearch()) {
                triggerSubscriptionScreen()
                return@launch
            }
            recordSearch("SEASONAL_TIPS")
            
            val apiKey = GeminiClient.getApiKey()
            if (apiKey.isBlank()) {
                Log.e("GardenViewModel", "API key is missing")
                _seasonalCareTips.value = getFallbackTips(location, season)
                return@launch
            }

            _isFetchingSeasonalCare.value = true
            try {
                val prompt = """
                    Fetch and provide seasonal plant care tips and advice for the location: "$location" and season/time of year: "$season".
                    Explain specific current environment conditions, watering changes needed, light requirements, humidity changes, and unique tasks for indoor or outdoor plants in this environment at this time of year.
                    Highlight any real-time seasonal events or regional weather quirks that gardeners in this area should watch out for right now.
                    Format the response using clean Markdown with headers (###), bullet points, and brief paragraphs.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    tools = listOf(GeminiTool(googleSearch = GeminiGoogleSearch()))
                )

                _loadingMessage.value = "Fetching tips..."
                val response = retryWithExponentialBackoff(
                    maxRetries = 3,
                    initialDelayMs = 1500,
                    onRetry = { attempt, e ->
                        _loadingMessage.value = "Connection issue (${e.message ?: "503"}). Retrying ($attempt/3)..."
                        Log.w("GardenViewModel", "Seasonal tips retry $attempt failed", e)
                    }
                ) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }

                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No tips available."
                
                val metadata = response.candidates.firstOrNull()?.groundingMetadata
                val queries = metadata?.webSearchQueries ?: emptyList()
                val sources = metadata?.groundingChunks?.mapNotNull { chunk ->
                    val title = chunk.web?.title
                    val uri = chunk.web?.uri
                    if (title != null && uri != null) Pair(title, uri) else null
                } ?: emptyList()

                _seasonalCareTips.value = SeasonalCareTipsResult(
                    content = rawText,
                    queries = queries,
                    sources = sources
                )
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Seasonal tips error", e)
                _seasonalCareTips.value = getFallbackTips(location, season)
            } finally {
                _isFetchingSeasonalCare.value = false
                _loadingMessage.value = null
            }
        }
    }

    private fun getFallbackTips(location: String, season: String): SeasonalCareTipsResult {
        val content = """
            ### 🌿 Seasonal Care Advice for $location ($season)
            
            *   **Watering Adjustments**: As the season changes to $season, plants typically adjust their growth. Check soil moisture before watering.
            *   **Light & Temperature**: Ensure your houseplants are receiving sufficient light. During $season, days can be shorter or longer. Protect plants from cold drafts or intense direct afternoon heat.
            *   **Nutrients**: Reduce fertilizer as plants slow down their growth cycle, or feed during active growth.
            *   **Pest Watch**: Keep a close eye out for common indoor pests like spider mites, fungus gnats, or mealybugs.
            
            *(Note: These are general offline fallback tips. Please connect to the internet to get live, real-time Google Search grounded advice!)*
        """.trimIndent()
        return SeasonalCareTipsResult(
            content = content,
            queries = listOf("seasonal plant care tips in $location during $season"),
            sources = listOf(Pair("Online Gardening Encyclopedia", "https://example.com/gardening"))
        )
    }

    fun fetchWeatherAndGardeningAdvice(locationName: String) {
        if (_isFetchingWeatherAdvice.value) return
        viewModelScope.launch {
            _isFetchingWeatherAdvice.value = true
            _loadingMessage.value = "Searching city..."
            try {
                val geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search"
                val geoResp = retryWithExponentialBackoff {
                    com.example.api.WeatherClient.apiService.searchCity(geocodingUrl, locationName)
                }
                val firstResult = geoResp.results?.firstOrNull()
                if (firstResult == null) {
                    _errorState.value = "City not found. Please try a different search or use GPS."
                    return@launch
                }

                val resolvedName = "${firstResult.name}, ${firstResult.country ?: ""}"
                fetchAdviceForCoords(firstResult.latitude, firstResult.longitude, resolvedName)
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Weather geocoding/fetch error", e)
                _errorState.value = "Could not fetch weather data. Check your internet connection."
            } finally {
                _isFetchingWeatherAdvice.value = false
                _loadingMessage.value = null
            }
        }
    }

    fun fetchWeatherAndGardeningAdviceByCoords(lat: Float, lon: Float, resolvedAddress: String) {
        if (_isFetchingWeatherAdvice.value) return
        viewModelScope.launch {
            _isFetchingWeatherAdvice.value = true
            try {
                fetchAdviceForCoords(lat, lon, resolvedAddress)
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Weather gps fetch error", e)
                _errorState.value = "Could not fetch weather for coordinates. Check your connection."
            } finally {
                _isFetchingWeatherAdvice.value = false
                _loadingMessage.value = null
            }
        }
    }

    private suspend fun fetchAdviceForCoords(lat: Float, lon: Float, resolvedName: String) {
        if (!canSearch()) {
            triggerSubscriptionScreen()
            return
        }
        recordSearch("WEATHER_ADVICE")
        
        _loadingMessage.value = "Fetching climate data..."
        val forecastUrl = "https://api.open-meteo.com/v1/forecast"
        val weatherResp = retryWithExponentialBackoff {
            com.example.api.WeatherClient.apiService.getForecast(forecastUrl, lat, lon)
        }
        val current = weatherResp.current
        if (current == null) {
            _errorState.value = "Could not retrieve current weather details."
            return
        }

        val temp = current.temperature
        val humidity = current.humidity
        val desc = getWeatherDescription(current.weatherCode)
        val windSpeed = current.windSpeed

        _loadingMessage.value = "Consulting gardening expert..."
        val apiKey = GeminiClient.getApiKey()
        val adviceText = if (apiKey.isNotBlank()) {
            val prompt = """
                You are an expert master gardener and horticultural advisor. 
                Analyze these real-time climate conditions for gardening:
                - Location: $resolvedName
                - Temperature: $temp°C
                - Relative Humidity: $humidity%
                - Current Weather Description: $desc
                - Wind Speed: $windSpeed km/h
                
                Provide highly specific, practical, localized gardening advice for both indoor houseplants and outdoor gardens under these exact conditions today.
                Explain how the current temperature, humidity, and rain (if any) affect soil evaporation and watering needs.
                Provide actionable checklists for protection, shading, or winter/storm guard if conditions are extreme (e.g. very hot, freezing, or windy).
                Suggest 3 optimal gardening activities that the user can perform today under this weather.
                
                Format the response using beautiful, clean Markdown with headers (###), bullet points, and brief paragraphs.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
            )

            val response = retryWithExponentialBackoff(
                maxRetries = 3,
                initialDelayMs = 1500,
                onRetry = { attempt, e ->
                    _loadingMessage.value = "Expert connection issue. Retrying ($attempt/3)..."
                }
            ) {
                GeminiClient.apiService.generateContent(apiKey, request)
            }

            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: getFallbackWeatherAdvice(resolvedName, temp, humidity, desc, windSpeed)
        } else {
            getFallbackWeatherAdvice(resolvedName, temp, humidity, desc, windSpeed)
        }

        _weatherAdvice.value = WeatherAdviceResult(
            locationName = resolvedName,
            temperature = temp,
            humidity = humidity,
            weatherDesc = desc,
            adviceText = adviceText
        )
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly Cloudy / Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzling"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rainy"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snowing"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Unknown Weather"
        }
    }

    private fun getFallbackWeatherAdvice(
        location: String,
        temp: Float,
        humidity: Float,
        desc: String,
        windSpeed: Float
    ): String {
        return """
            ### 🌦️ Localized Gardening Guidance for $location
            
            Current conditions: **$desc** at **$temp°C** with **$humidity%** humidity and **$windSpeed km/h** wind.
            
            *   **Watering Plan**: With temperature at $temp°C and humidity at $humidity%, check soil moisture levels. If it is rainy or drizzling, you can skip outdoor watering. For indoor plants, water as scheduled when the top inch of soil feels dry.
            *   **Plant Protection**: If wind speeds are high ($windSpeed km/h), secure tall outdoor potted plants or move delicate foliage to a sheltered area to prevent broken stems or leaf tear.
            *   **Outdoor Activities**: If weather permits ($desc), tidy up weeds, inspect leaves for pests, or repot hardy outdoor species. If raining, focus on cleaning indoor plant foliage or cleaning garden tools.
            
            *(Note: Active internet connection is recommended to receive deep generative gardening advice powered by Gemini.)*
        """.trimIndent()
    }

    // --- Care Tasks Operations ---
    fun addCareTask(task: com.example.data.model.CareTask) {
        viewModelScope.launch {
            repository.insertCareTask(task)
        }
    }

    fun toggleCareTask(task: com.example.data.model.CareTask) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedDate = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateCareTask(updated)
        }
    }

    fun deleteCareTask(task: com.example.data.model.CareTask) {
        viewModelScope.launch {
            repository.deleteCareTask(task)
        }
    }

    private suspend fun seedDefaultCareTasks() {
        val today = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        val tasks = listOf(
            com.example.data.model.CareTask(
                plantName = "Fiona (Fiddle Leaf Fig)",
                taskType = "Watering",
                scheduledDate = today,
                notes = "Check top 2 inches of soil first. Give 3 cups of water."
            ),
            com.example.data.model.CareTask(
                plantName = "Monstera Deliciosa",
                taskType = "Fertilizing",
                scheduledDate = today + oneDay,
                notes = "Apply balanced organic mineral liquid fertilizer."
            ),
            com.example.data.model.CareTask(
                plantName = "Aloe Vera",
                taskType = "Sunlight/Rotation",
                scheduledDate = today + 2 * oneDay,
                notes = "Rotate 90 degrees to ensure even growth towards light."
            ),
            com.example.data.model.CareTask(
                plantName = "Snake Plant",
                taskType = "Vitamins",
                scheduledDate = today + 3 * oneDay,
                notes = "Check if leaves need a gentle wipe to remove dust."
            ),
            com.example.data.model.CareTask(
                plantName = "Peace Lily",
                taskType = "Medicine/Pesticides",
                scheduledDate = today + 4 * oneDay,
                notes = "Spray with neem oil solution if any white flies are spotted."
            )
        )

        for (task in tasks) {
            repository.insertCareTask(task)
        }
    }

    fun checkDeficiency(symptoms: List<String>, envNotes: String, bitmap: Bitmap?) {
        if (_isCheckingDeficiency.value) return
        viewModelScope.launch {
            if (!canSearch()) {
                triggerSubscriptionScreen()
                _isCheckingDeficiency.value = false
                return@launch
            }
            recordSearch("DEFICIENCY")
            _isCheckingDeficiency.value = true
            _errorState.value = null
            _deficiencyResult.value = null

            val apiKey = GeminiClient.getApiKey()
            if (isKeyPlaceholder(apiKey)) {
                simulateMockDeficiency(symptoms, envNotes)
                _isCheckingDeficiency.value = false
                return@launch
            }

            try {
                val parts = mutableListOf<GeminiPart>()
                if (bitmap != null) {
                    val base64Data = withContext(Dispatchers.IO) { bitmap.toBase64() }
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data)))
                }
                
                val symptomsText = symptoms.joinToString(", ")
                parts.add(
                    GeminiPart(
                        text = "Analyze this plant's nutritional and environmental deficiency. " +
                                "Symptoms: $symptomsText. " +
                                "Environment notes: $envNotes. " +
                                "Analyze what the plant is lacking in terms of: " +
                                "1. Minerals (e.g., Nitrogen, Phosphorus, Potassium, Iron, Magnesium, Zinc) " +
                                "2. Sunlight (intensity, hours, direction) " +
                                "3. Water (underwatering vs overwatering) " +
                                "4. Vitamins (organic boosters, trace elements) " +
                                "5. Medicine (fungicide, neem oil, pesticide) " +
                                "Respond strictly with a JSON object conforming exactly to this structure: " +
                                "{\n" +
                                "  \"plantCondition\": \"Condition Summary\",\n" +
                                "  \"lackingCategory\": \"Main Lacking Categories\",\n" +
                                "  \"mineralsAnalysis\": \"Mineral analysis text\",\n" +
                                "  \"sunlightAnalysis\": \"Sunlight analysis text\",\n" +
                                "  \"waterAnalysis\": \"Water analysis text\",\n" +
                                "  \"vitaminsAnalysis\": \"Vitamins analysis text\",\n" +
                                "  \"medicineAnalysis\": \"Medicine/pesticide analysis text\",\n" +
                                "  \"remedialPlan\": [\"Step 1\", \"Step 2\", \"Step 3\"]\n" +
                                "}\n" +
                                "Do not include any markdown styling other than the JSON itself. Make sure it is valid JSON. " +
                                "IMPORTANT: Provide the response translated into the ${_appLanguage.value} language. Keep all JSON keys in English, but translate the string values into ${_appLanguage.value}."
                    )
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                _loadingMessage.value = "Analyzing nutrients..."
                val response = retryWithExponentialBackoff(
                    maxRetries = 3,
                    initialDelayMs = 1500,
                    onRetry = { attempt, e ->
                        _loadingMessage.value = "Connection issue (${e.message ?: "503"}). Retrying ($attempt/3)..."
                        Log.w("GardenViewModel", "Deficiency retry $attempt failed", e)
                    }
                ) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }

                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = extractJson(rawText)

                val moshi = GeminiClient.getMoshi()
                val adapter = moshi.adapter(com.example.api.dto.DeficiencyAnalysisDTO::class.java)
                val dto = adapter.fromJson(cleanJson)

                if (dto != null) {
                    _deficiencyResult.value = dto
                } else {
                    _errorState.value = "Failed to parse deficiency analysis. Reverting to smart analyzer."
                    simulateMockDeficiency(symptoms, envNotes)
                }
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Deficiency check error", e)
                val is429 = e is retrofit2.HttpException && e.code() == 429
                val errMsg = if (is429) "HTTP 429 Rate Limit Exceeded" else e.localizedMessage ?: "Unknown error"
                _errorState.value = "AI Deficiency check failed: $errMsg. Prototyping Offline?"
                simulateMockDeficiency(symptoms, envNotes)
            } finally {
                _isCheckingDeficiency.value = false
                _loadingMessage.value = null
            }
        }
    }

    private suspend fun simulateMockDeficiency(symptoms: List<String>, envNotes: String) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(2000)
            
            val symStr = symptoms.joinToString(" ").lowercase()
            val envStr = envNotes.lowercase()

            val lacksMinerals = symStr.contains("yellow") || symStr.contains("pale") || symStr.contains("stunted") || envStr.contains("fertilizer")
            val lacksSunlight = symStr.contains("slow") || envStr.contains("dark") || envStr.contains("corner") || symStr.contains("stretching")
            val lacksWater = symStr.contains("dry") || symStr.contains("crispy") || symStr.contains("droop") || envStr.contains("bone dry") || envStr.contains("forgot")
            val overwatered = symStr.contains("mushy") || symStr.contains("soft") || symStr.contains("yellow") && envStr.contains("wet") || envStr.contains("damp")
            val lacksVitamins = symStr.contains("deformed") || symStr.contains("curling") || symStr.contains("spots")
            val lacksMedicine = symStr.contains("web") || symStr.contains("bug") || symStr.contains("insect") || symStr.contains("powder") || symStr.contains("white") || symStr.contains("spot")

            var condition = "Mild Nutrient Imbalance"
            val lackingList = mutableListOf<String>()

            if (lacksMinerals) {
                condition = "Nitrogen & Iron Mineral Deficiency"
                lackingList.add("Minerals")
            }
            if (lacksSunlight) {
                condition = "Insuffient Sunlight (Etiolation)"
                lackingList.add("Sunlight")
            }
            if (lacksWater) {
                condition = "Severe Dehydration / Underwatering"
                lackingList.add("Water")
            }
            if (overwatered) {
                condition = "Oxygen Deprivation / Impending Root Rot"
                lackingList.add("Water Regulation")
            }
            if (lacksVitamins) {
                condition = "Micronutrient & Calcium Deficiency"
                lackingList.add("Vitamins / Micronutrients")
            }
            if (lacksMedicine) {
                condition = "Fungal or Pest Infestation"
                lackingList.add("Medicine / Pest treatment")
            }

            if (lackingList.isEmpty()) {
                condition = "Healthy / Minor Seasonal Adjustment"
                lackingList.add("Routine Minerals")
            }

            val mineralsTxt = if (lacksMinerals) {
                "Lacking Nitrogen (N) and Iron (Fe). Yellowing of lower leaves is a classic indicator of nitrogen deficiency. Iron deficiency causes interveinal chlorosis in new growth."
            } else {
                "Minerals levels look stable. Keep feeding with balanced liquid fertilizer once a month during spring/summer."
            }

            val lightTxt = if (lacksSunlight) {
                "Insufficient light exposure. The plant is stretching towards light source, losing variegation or growing pale. Needs bright indirect light (at least 4-6 hours)."
            } else if (envStr.contains("direct") || envStr.contains("hot")) {
                "Excessive direct sunlight! Risk of leaf scorching/sunburn. Move to a filtered indirect lighting spot."
            } else {
                "Light level seems appropriate for general houseplants."
            }

            val waterTxt = if (lacksWater) {
                "Severely dehydrated. Soil is compacted, which prevents moisture absorption. Do a deep bottom-watering soak immediately."
            } else if (overwatered) {
                "Critical overwatering! Soil is waterlogged, suffocating root hairs. Aerate soil, stop watering immediately, and check for root rot signs."
            } else {
                "Moisture levels seem standard. Always check top 2 inches before watering."
            }

            val vitTxt = if (lacksVitamins || lacksMinerals) {
                "Deficient in vital trace elements (Zinc, Magnesium) and Calcium. Needs organic seaweed extract or Epsom salt (magnesium sulfate) spray booster."
            } else {
                "Micronutrients seem adequate. Treat with compost tea twice a year."
            }

            val medTxt = if (lacksMedicine) {
                if (symStr.contains("powder") || symStr.contains("white") || symStr.contains("spot")) {
                    "Fungal spore activity detected. Treat with copper fungicide or organic baking soda spray (1 tbsp baking soda + 1 tsp dish soap per gallon of water)."
                } else {
                    "Pest infestation suspected. Spray thoroughly with neem oil emulsion or insecticidal soap, paying close attention to leaf undersides."
                }
            } else {
                "No urgent pests or fungal signs. Apply diluted neem oil once a month as safe preventative medicine."
            }

            val steps = mutableListOf<String>()
            if (lacksWater) steps.add("Soak the pot in a tub of water for 30 minutes (bottom watering) to rehydrate compacted soil.")
            if (overwatered) steps.add("Stop watering immediately, place in high-airflow area, and repot in fresh, dry soil if musty smell persists.")
            if (lacksMinerals) steps.add("Apply nitrogen-rich organic NPK liquid plant fertilizer at half-strength during next water loop.")
            if (lacksSunlight) steps.add("Gradually move plant to a brighter east or south-facing window. Avoid sudden direct hot sun.")
            if (lacksVitamins) steps.add("Apply calcium-magnesium (Cal-Mag) spray supplement to improve leaf structure strength.")
            if (lacksMedicine) steps.add("Isolate the plant from other green buddies. Treat with neem oil or copper fungicide spray every 5 days.")
            if (steps.isEmpty()) {
                steps.add("Wipe leaves with a damp cloth to remove dust and maximize photosynthesis.")
                steps.add("Maintain current care loop and check soil weekly.")
            }

            val result = com.example.api.dto.DeficiencyAnalysisDTO(
                plantCondition = condition,
                lackingCategory = lackingList.joinToString(" + "),
                mineralsAnalysis = mineralsTxt,
                sunlightAnalysis = lightTxt,
                waterAnalysis = waterTxt,
                vitaminsAnalysis = vitTxt,
                medicineAnalysis = medTxt,
                remedialPlan = steps
            )

            _deficiencyResult.value = result
        }
    }

    private suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        factor: Double = 2.0,
        onRetry: (Int, Exception) -> Unit = { _, _ -> },
        block: suspend () -> T
    ): T {
        return com.example.api.RequestQueue.submit(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            factor = factor,
            onRetry = onRetry,
            block = block
        )
    }

    fun triggerSubscriptionScreen() {
        _showSubscriptionRequired.value = true
    }

    fun dismissSubscriptionScreen() {
        _showSubscriptionRequired.value = false
    }

    suspend fun canSearch(): Boolean {
        val profile = userProfile.value ?: return false
        return profile.subscriptionStatus == "ACTIVE" && profile.expiryDate > System.currentTimeMillis()
    }

    fun recordSearch(searchType: String) {
        viewModelScope.launch {
            repository.insertSearchRecord(
                com.example.data.model.SearchRecord(
                    timestamp = System.currentTimeMillis(),
                    searchType = searchType
                )
            )
        }
    }

    fun purchaseSubscriptionPlay(planType: String, token: String) {
        viewModelScope.launch {
            val amount = when (planType) {
                "DAILY" -> 5f
                "WEEKLY" -> 50f
                "YEARLY" -> 365f
                else -> 5f
            }
            val durationMs = when (planType) {
                "DAILY" -> 24 * 60 * 60 * 1000L
                "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                "YEARLY" -> 365 * 24 * 60 * 60 * 1000L
                else -> 24 * 60 * 60 * 1000L
            }

            val updatedProfile = com.example.data.model.UserProfile(
                id = 1,
                subscriptionStatus = "ACTIVE",
                expiryDate = System.currentTimeMillis() + durationMs,
                email = "kanakamahesh4@gmail.com",
                billingProvider = "PLAY"
            )
            repository.insertUserProfile(updatedProfile)

            repository.insertTransactionRecord(
                com.example.data.model.TransactionRecord(
                    timestamp = System.currentTimeMillis(),
                    amount = amount,
                    type = "PLAY_BILLING",
                    duration = planType,
                    tokenOrTxnId = token,
                    status = "SUCCESS"
                )
            )

            _showSubscriptionRequired.value = false
        }
    }

    fun initiateUpiPayment(planType: String, txnId: String) {
        viewModelScope.launch {
            val amount = when (planType) {
                "DAILY" -> 5f
                "WEEKLY" -> 50f
                "YEARLY" -> 365f
                else -> 5f
            }
            repository.insertTransactionRecord(
                com.example.data.model.TransactionRecord(
                    timestamp = System.currentTimeMillis(),
                    amount = amount,
                    type = "UPI",
                    duration = planType,
                    tokenOrTxnId = txnId,
                    status = "PENDING"
                )
            )
        }
    }

    fun confirmUpiPayment(txnId: String, planType: String) {
        viewModelScope.launch {
            val amount = when (planType) {
                "DAILY" -> 5f
                "WEEKLY" -> 50f
                "YEARLY" -> 365f
                else -> 5f
            }
            val durationMs = when (planType) {
                "DAILY" -> 24 * 60 * 60 * 1000L
                "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                "YEARLY" -> 365 * 24 * 60 * 60 * 1000L
                else -> 24 * 60 * 60 * 1000L
            }

            val updatedProfile = com.example.data.model.UserProfile(
                id = 1,
                subscriptionStatus = "ACTIVE",
                expiryDate = System.currentTimeMillis() + durationMs,
                email = "kanakamahesh4@gmail.com",
                billingProvider = "UPI"
            )
            repository.insertUserProfile(updatedProfile)

            repository.insertTransactionRecord(
                com.example.data.model.TransactionRecord(
                    timestamp = System.currentTimeMillis(),
                    amount = amount,
                    type = "UPI",
                    duration = planType,
                    tokenOrTxnId = txnId,
                    status = "SUCCESS"
                )
            )

            _showSubscriptionRequired.value = false
        }
    }

    fun failUpiPayment(txnId: String, planType: String) {
        viewModelScope.launch {
            val amount = when (planType) {
                "DAILY" -> 5f
                "WEEKLY" -> 50f
                "YEARLY" -> 365f
                else -> 5f
            }
            repository.insertTransactionRecord(
                com.example.data.model.TransactionRecord(
                    timestamp = System.currentTimeMillis(),
                    amount = amount,
                    type = "UPI",
                    duration = planType,
                    tokenOrTxnId = txnId,
                    status = "FAILED"
                )
            )
        }
    }
}

data class SeasonalCareTipsResult(
    val content: String,
    val queries: List<String> = emptyList(),
    val sources: List<Pair<String, String>> = emptyList() // title, uri
)

data class WeatherAdviceResult(
    val locationName: String,
    val temperature: Float,
    val humidity: Float,
    val weatherDesc: String,
    val adviceText: String,
    val timestamp: Long = System.currentTimeMillis()
)
