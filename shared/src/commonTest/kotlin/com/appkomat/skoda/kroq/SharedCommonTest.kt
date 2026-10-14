package com.appkomat.skoda.kroq

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = false
    }

    @Test
    fun testStepUpdatePayloadToday() {
        val payload = StepUpdatePayload(steps = 12345)
        val jsonString = json.encodeToString(payload)
        assertEquals("""{"steps":12345}""", jsonString)
    }

    @Test
    fun testStepUpdatePayloadYesterday() {
        val payload = StepUpdatePayload(steps = 12345, date = "yesterday")
        val jsonString = json.encodeToString(payload)
        assertEquals("""{"steps":12345,"date":"yesterday"}""", jsonString)
    }

    @Test
    fun testStepUpdatePayloadSpecificDate() {
        val payload = StepUpdatePayload(steps = 12345, date = "2026-10-04")
        val jsonString = json.encodeToString(payload)
        assertEquals("""{"steps":12345,"date":"2026-10-04"}""", jsonString)
    }
}