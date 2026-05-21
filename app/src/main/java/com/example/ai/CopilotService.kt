package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CopilotService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeFlightDiagnostics(
        telemetrySummary: String,
        activeMode: String,
        activeWarnings: List<String>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "⚠️ CO-PILOT CONFIGURATION: Gemini API Key is missing. Please set your key in the AI Studio Secrets panel to activate live AI copilot advisories."
        }

        val prompt = """
            You are SkyControl GCS Co-pilot, an elite autonomous military drone and aerospace operations analyst.
            Analyze the following real-time telemetry state of our autonomous drone and generate a concise 2-3 sentence strategic advice, alerting the operator to potential risks (such as wind gusts or battery cell voltage discharge rate) and suggested flight path or mode adjustments.
            
            [Telemetry Summary]:
            $telemetrySummary
            
            [Active Drone Mode]: $activeMode
            [Active Trigger Warnings]: ${activeWarnings.joinToString(", ").ifEmpty { "None" }}
            
            Keep your advice authoritative, clinical, aerospace-focused, and under 80 words. Direct the operator clearly.
        """.trimIndent()

        // Build standard Gemini API prompt request JSON payload
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val requestBody = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "⚠️ AI Core Connection Error (HTTP ${response.code}). Failed to fetch recommendation."
                }
                
                val responseBodyStr = response.body?.string() ?: ""
                if (responseBodyStr.isEmpty()) return@withContext "⚠️ Diagnostic Briefing Unavailable: Empty payload response."

                // Parse out the candidate text
                val rootObj = JSONObject(responseBodyStr)
                val candidates = rootObj.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = contentObj.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text").trim()
                    }
                }
                "AI Diagnostic complete. No action required."
            }
        } catch (e: Exception) {
            "⚠️ MAV-AI Link Error: ${e.message}. Check network infrastructure."
        }
    }
}
