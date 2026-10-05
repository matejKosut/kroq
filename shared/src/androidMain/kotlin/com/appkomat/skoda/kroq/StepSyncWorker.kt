package com.appkomat.skoda.kroq

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class StepSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (HealthConnectClient.sdkStatus(applicationContext) != HealthConnectClient.SDK_AVAILABLE) {
                return@withContext Result.failure()
            }

            val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)

            // Read steps for the last hour
            val endTime = Instant.now()
            val startTime = endTime.minus(1, ChronoUnit.HOURS)

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Calculate total steps
            var totalSteps = 0L
            for (stepRecord in response.records) {
                totalSteps += stepRecord.count
            }

            // Send to our backend
            NetworkClient.sendStepsToBackend(totalSteps.toInt())

            Result.success()
        } catch (e: Exception) {
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
    }
}
