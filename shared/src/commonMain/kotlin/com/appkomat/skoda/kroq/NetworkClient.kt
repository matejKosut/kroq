package com.appkomat.skoda.kroq

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StepUpdatePayload(
    val steps: Int,
    @SerialName("date")
    val date: String? = null
)

object NetworkClient {
    private const val API_URL = "https://qifibhlfgojruxbewtoa.supabase.co/functions/v1/apple-health-sync"
    private const val API_KEY = "kroq_47a1b608c1f69a0a914f4a74ed2be0c5d429be1bb06819bd"

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun sendStepsToBackend(steps: Int, date: String? = null): HttpResponse? {
        return try {
            val payload = StepUpdatePayload(
                steps = steps,
                date = date
            )
            val response = client.post(API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", API_KEY)
                setBody(payload)
            }
            println("Steps sync response status: ${response.status}")
            response
        } catch (e: Exception) {
            println("Error syncing steps: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
