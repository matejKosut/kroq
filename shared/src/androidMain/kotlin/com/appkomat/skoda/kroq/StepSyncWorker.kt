package com.appkomat.skoda.kroq

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class StepSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            println("[StepSyncWorker] Starting Health Connect step sync...")
            if (HealthConnectClient.getSdkStatus(applicationContext) != HealthConnectClient.SDK_AVAILABLE) {
                println("[StepSyncWorker] Health Connect SDK is not available.")
                return@withContext Result.failure()
            }

            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)

            // Read steps for today (from midnight to now)
            val now = Instant.now()
            val startOfDay = ZonedDateTime.now(ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )

            // Calculate total steps for today
            var totalSteps = 0L
            for (stepRecord in response.records) {
                totalSteps += stepRecord.count
            }

            println("[StepSyncWorker] Read $totalSteps steps for today from Health Connect. Syncing to backend...")

            // Send to backend (null date = today)
            val httpResponse = NetworkClient.sendStepsToBackend(totalSteps.toInt())
            if (httpResponse != null && httpResponse.status.value in 200..299) {
                println("[StepSyncWorker] Step sync completed successfully.")
                Result.success()
            } else {
                println("[StepSyncWorker] Step sync failed with status: ${httpResponse?.status}")
                Result.retry()
            }
        } catch (e: Exception) {
            println("[StepSyncWorker] Error in StepSyncWorker: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<StepSyncWorker>(1, TimeUnit.HOURS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "StepSyncWorker",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun runOnce(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<StepSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
