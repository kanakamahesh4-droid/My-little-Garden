package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
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

class GardenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GardenRepository
    val plants: StateFlow<List<Plant>>
    val diagnosesHistory: StateFlow<List<Diagnosis>>
    val identificationsHistory: StateFlow<List<PlantIdentification>>

    init {
        val database = GardenDatabase.getDatabase(application)
        repository = GardenRepository(database.gardenDao())

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

        // Seed some starter plants if empty
        viewModelScope.launch {
            repository.allPlants.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultPlants()
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

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _identificationResult = MutableStateFlow<PlantIdentificationDTO?>(null)
    val identificationResult: StateFlow<PlantIdentificationDTO?> = _identificationResult.asStateFlow()

    private val _diagnosisResult = MutableStateFlow<DiseaseDiagnosisDTO?>(null)
    val diagnosisResult: StateFlow<DiseaseDiagnosisDTO?> = _diagnosisResult.asStateFlow()

    private val _carePlanResult = MutableStateFlow<PersonalizedCarePlanDTO?>(null)
    val carePlanResult: StateFlow<PersonalizedCarePlanDTO?> = _carePlanResult.asStateFlow()

    private val _seasonalCareTips = MutableStateFlow<SeasonalCareTipsResult?>(null)
    val seasonalCareTips: StateFlow<SeasonalCareTipsResult?> = _seasonalCareTips.asStateFlow()

    private val _isFetchingSeasonalCare = MutableStateFlow(false)
    val isFetchingSeasonalCare: StateFlow<Boolean> = _isFetchingSeasonalCare.asStateFlow()

    fun clearSeasonalCareTips() {
        _seasonalCareTips.value = null
    }

    fun clearError() {
        _errorState.value = null
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
            repository.insertPlant(plant)
        }
    }

    fun deletePlantFromGarden(plant: Plant) {
        viewModelScope.launch {
            repository.deletePlant(plant)
        }
    }

    fun waterPlant(plantId: Int) {
        viewModelScope.launch {
            repository.waterPlant(plantId, System.currentTimeMillis())
        }
    }

    fun updatePlant(plant: Plant) {
        viewModelScope.launch {
            repository.updatePlant(plant)
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

    // --- API & AI Operations ---

    // Helper to convert Bitmap to Base64
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
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
        viewModelScope.launch {
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
                        text = "Identify this plant. " +
                                "Description context: $textDescription. " +
                                "Respond strictly with a JSON object conforming exactly to this structure: " +
                                "{\n" +
                                "  \"detectedName\": \"Common Plant Name\",\n" +
                                "  \"scientificName\": \"Scientific genus/species\",\n" +
                                "  \"confidence\": 0.95,\n" +
                                "  \"description\": \"A paragraph detailing what this plant is, origin, and interest.\",\n" +
                                "  \"origin\": \"Geographical origins of the plant, e.g., Tropical rainforests of southern Mexico\",\n" +
                                "  \"growthHabits\": \"How the plant grows, e.g., Vine-like climber, can grow up to 10 feet indoors\",\n" +
                                "  \"careNeeds\": \"Brief care requirements summary, e.g., Medium watering, bright filtered light\",\n" +
                                "  \"lightGuide\": \"Detailed description of light requirement\",\n" +
                                "  \"waterGuide\": \"Detailed description of watering requirements\",\n" +
                                "  \"soilGuide\": \"Detailed description of soil requirements\",\n" +
                                "  \"petSafe\": true\n" +
                                "}\n" +
                                "Do not include any Markdown tags other than standard text, do not return any other text outside the JSON."
                    )
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                val response = GeminiClient.apiService.generateContent(apiKey, request)
                val rawText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = extractJson(rawText)

                val moshi = GeminiClient.getMoshi()
                val adapter = moshi.adapter(PlantIdentificationDTO::class.java)
                val dto = adapter.fromJson(cleanJson)

                if (dto != null) {
                    _identificationResult.value = dto
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
                } else {
                    _errorState.value = "Failed to parse plant identification data. Please try again."
                }
            } catch (e: Exception) {
                Log.e("GardenViewModel", "Identification error", e)
                _errorState.value = "Error connecting to AI Server: ${e.localizedMessage ?: "Unknown error"}. Prototyping Offline?"
                // Auto-fallback for ease of testing
                simulateMockIdentification(bitmap, textDescription)
            } finally {
                _isIdentifying.value = false
            }
        }
    }

    private suspend fun simulateMockIdentification(bitmap: Bitmap?, desc: String) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(2000) // Simulate networking
            val isMonstera = desc.contains("monstera", ignoreCase = true) || desc.contains("split", ignoreCase = true)
            val isFern = desc.contains("fern", ignoreCase = true) || desc.contains("boston", ignoreCase = true)
            
            val mockDto = when {
                isMonstera -> PlantIdentificationDTO(
                    detectedName = "Monstera Deliciosa",
                    scientificName = "Monstera deliciosa",
                    confidence = 0.98f,
                    description = "A popular indoor climbing plant famous for its iconic fenestrated (split) leaves. Originating in tropical forests of Mexico.",
                    origin = "Tropical rainforests of Southern Mexico and Central America",
                    growthHabits = "Vining climber that uses aerial roots to scale trees. Can grow up to 10-15 feet indoors with a moss pole support.",
                    careNeeds = "Prefers bright, indirect light, chunky well-draining soil, and watering only when the top 50% of soil has dried out completely.",
                    lightGuide = "Bright indirect sunlight. Direct harsh light will scorch the leaves, while low light slows growth.",
                    waterGuide = "Water thoroughly when the top 2-3 inches of soil dry out. Ensure pots have drainage holes to avoid root rot.",
                    soilGuide = "Rich organic peaty soil mix. Prefers well-draining aerated soil with perlite or pine bark.",
                    petSafe = false
                )
                isFern -> PlantIdentificationDTO(
                    detectedName = "Boston Fern",
                    scientificName = "Nephrolepis exaltata",
                    confidence = 0.92f,
                    description = "An elegant classic fern with rich feathery green fronds. Thrives in high humidity and damp conditions.",
                    origin = "Humid tropical regions worldwide, particularly native to the Americas, West Indies, and Africa",
                    growthHabits = "Clump-forming, arching habit with long sword-shaped fronds that gracefully cascade outwards. Spreads via runners.",
                    careNeeds = "Requires consistent dampness, high ambient humidity, cool-to-warm temperatures, and bright but fully filtered indirect light.",
                    lightGuide = "Filtered indirect light. Thrives in bathrooms or kitchens with dappled or diffused sunlight.",
                    waterGuide = "Keep soil consistently damp but not waterlogged. Mist leaves frequently to boost ambient humidity.",
                    soilGuide = "Moist, nutrient-rich potting soil with good moisture-retaining capacity.",
                    petSafe = true
                )
                else -> PlantIdentificationDTO(
                    detectedName = "Prayer Plant",
                    scientificName = "Maranta leuconeura",
                    confidence = 0.89f,
                    description = "A striking houseplant featuring stunning variegated foliage that folds up at night like hands in prayer.",
                    origin = "Tropical rainforest floors of Brazil, South America",
                    growthHabits = "Low-growing, spreading clump habit. Reaches about 12 inches in height but crawls horizontally to make a beautiful display.",
                    careNeeds = "Demands high humidity, moist but well-aerated potting mix, and low-to-medium indirect light.",
                    lightGuide = "Medium indirect sunlight. Keep away from direct hot windows to maintain bright foliage color.",
                    waterGuide = "Water when the top of the soil is dry. Use lukewarm filtered or rain water if possible.",
                    soilGuide = "Well-aerated houseplant potting soil with good drainage.",
                    petSafe = true
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
        }
    }

    fun diagnoseDisease(bitmap: Bitmap?, symptoms: String) {
        viewModelScope.launch {
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
                                "Do not include any markdown styling other than the JSON itself. Make sure it is valid JSON."
                    )
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                val response = GeminiClient.apiService.generateContent(apiKey, request)
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
                _errorState.value = "Error connecting to AI Server: ${e.localizedMessage ?: "Unknown error"}. Prototyping Offline?"
                simulateMockDiagnosis(bitmap, symptoms)
            } finally {
                _isDiagnosing.value = false
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
        viewModelScope.launch {
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
                                "Respond with only the raw JSON. No markdown code block."
                    )
                )

                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = parts)),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                val response = GeminiClient.apiService.generateContent(apiKey, request)
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
                _errorState.value = "Error connecting to AI Server: ${e.localizedMessage ?: "Unknown error"}"
                simulateMockCarePlan(plantName)
            } finally {
                _isGeneratingCarePlan.value = false
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
        viewModelScope.launch {
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

                val response = GeminiClient.apiService.generateContent(apiKey, request)
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
}

data class SeasonalCareTipsResult(
    val content: String,
    val queries: List<String> = emptyList(),
    val sources: List<Pair<String, String>> = emptyList() // title, uri
)
