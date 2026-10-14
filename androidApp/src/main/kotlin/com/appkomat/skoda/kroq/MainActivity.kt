package com.appkomat.skoda.kroq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
    
    private val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
        if (granted.containsAll(setOf(HealthPermission.getReadPermission(StepsRecord::class)))) {
            onPermissionsGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        checkHealthConnectPermissions()

        setContent {
            App()
        }
    }

    private fun checkHealthConnectPermissions() {
        val status = HealthConnectClient.getSdkStatus(this)
        println("[MainActivity] Checking Health Connect SDK status: $status")
        when (status) {
            HealthConnectClient.SDK_AVAILABLE -> {
                val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
                lifecycleScope.launch {
                    val client = HealthConnectClient.getOrCreate(this@MainActivity)
                    val granted = client.permissionController.getGrantedPermissions()
                    println("[MainActivity] Health Connect granted permissions: $granted")
                    if (granted.containsAll(permissions)) {
                        println("[MainActivity] All required permissions granted. Starting step sync...")
                        onPermissionsGranted()
                    } else {
                        println("[MainActivity] Permissions missing. Launching permission prompt...")
                        requestPermissions.launch(permissions)
                    }
                }
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                println("[MainActivity] Health Connect update required.")
                val providerPackageName = "com.google.android.apps.healthdata"
                val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
                try {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriString)))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> {
                println("[MainActivity] Health Connect SDK is unavailable: status=$status")
            }
        }
    }

    private fun onPermissionsGranted() {
        StepSyncWorker.enqueue(applicationContext)
        StepSyncWorker.runOnce(applicationContext)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}