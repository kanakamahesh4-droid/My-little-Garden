package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "tools") val tools: List<GeminiTool>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiTool(
    @Json(name = "googleSearch") val googleSearch: GeminiGoogleSearch? = null,
    @Json(name = "googleSearchRetrieval") val googleSearchRetrieval: GeminiGoogleSearchRetrieval? = null
)

@JsonClass(generateAdapter = true)
class GeminiGoogleSearch

@JsonClass(generateAdapter = true)
data class GeminiGoogleSearchRetrieval(
    @Json(name = "dynamicRetrievalConfig") val dynamicRetrievalConfig: GeminiDynamicRetrievalConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiDynamicRetrievalConfig(
    @Json(name = "mode") val mode: String = "MODE_DYNAMIC",
    @Json(name = "dynamicThreshold") val dynamicThreshold: Float = 0.3f
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent,
    @Json(name = "groundingMetadata") val groundingMetadata: GeminiGroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingMetadata(
    @Json(name = "webSearchQueries") val webSearchQueries: List<String>? = null,
    @Json(name = "groundingChunks") val groundingChunks: List<GeminiGroundingChunk>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGroundingChunk(
    @Json(name = "web") val web: GeminiWebSource? = null
)

@JsonClass(generateAdapter = true)
data class GeminiWebSource(
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "title") val title: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
