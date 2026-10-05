package com.appkomat.skoda.kroq

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StepUpdatePayload(
    val userId: String,
    val steps: Int,
    val timestamp: Long
)

object NetworkClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun sendStepsToBackend(steps: Int) {
        try {
            val payload = StepUpdatePayload(
                userId = "test_user", // This would normally come from your auth/settings
                steps = steps,
                timestamp = 0L // Placeholder
            )
            // Replace with your actual API endpoint
            client.post("https://kroq.lovable.app/api/update-steps") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
