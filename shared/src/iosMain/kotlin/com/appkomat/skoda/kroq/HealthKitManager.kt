package com.appkomat.skoda.kroq

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKObserverQuery
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuantityTypeIdentifierStepCount
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKUnit
import platform.HealthKit.*

object HealthKitManager {
    private val healthStore = HKHealthStore()
    private val stepType = HKObjectType.quantityTypeForIdentifier(HKQuantityTypeIdentifierStepCount)

    fun startObservingSteps() {
        if (!HKHealthStore.isHealthDataAvailable()) return
        if (stepType == null) return

        healthStore.requestAuthorizationToShareTypes(
            null,
            setOf(stepType)
        ) { success, error ->
            if (success) {
                setupBackgroundDelivery()
                executeObserverQuery()
            } else {
                println("HealthKit authorization failed: $error")
            }
        }
    }

    private fun setupBackgroundDelivery() {
        if (stepType == null) return
        healthStore.enableBackgroundDeliveryForType(
            stepType,
            platform.HealthKit.HKUpdateFrequencyHourly,
            withCompletion = { success: Boolean, error: NSError? ->
                if (!success) {
                    println("Failed to enable background delivery: $error")
                }
            }
        )
    }

    private fun executeObserverQuery() {
        if (stepType == null) return
        val query = HKObserverQuery(
            sampleType = stepType,
            predicate = null,
            updateHandler = { _, completionHandler, error ->
                if (error == null) {
                    fetchLatestStepsAndSync(completionHandler)
                } else {
                    println("Observer query failed: $error")
                    completionHandler?.invoke()
                }
            }
        )
        healthStore.executeQuery(query)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchLatestStepsAndSync(completionHandler: (() -> Unit)?) {
        if (stepType == null) return
        
        // Fetch steps for today (from midnight to now)
        val calendar = NSCalendar.currentCalendar
        val endDate = NSDate()
        val startDate = calendar.startOfDayForDate(endDate)
        val predicate = platform.HealthKit.HKQuery.predicateForSamplesWithStartDate(
            startDate,
            endDate,
            platform.HealthKit.HKQueryOptionNone
        )

        val query = HKSampleQuery(
            sampleType = stepType,
            predicate = predicate,
            limit = 0u, // No limit
            sortDescriptors = null,
            resultsHandler = { _, results, error ->
                if (error == null && results != null) {
                    var totalSteps = 0
                    for (sample in results) {
                        val quantitySample = sample as? HKQuantitySample
                        if (quantitySample != null) {
                            val count = quantitySample.quantity.doubleValueForUnit(HKUnit.countUnit())
                            totalSteps += count.toInt()
                        }
                    }
                    
                    GlobalScope.launch {
                        try {
                            NetworkClient.sendStepsToBackend(totalSteps)
                        } finally {
                            completionHandler?.invoke()
                        }
                    }
                } else {
                    completionHandler?.invoke()
                }
            }
        )
        healthStore.executeQuery(query)
    }
}
