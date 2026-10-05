package com.appkomat.skoda.kroq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
            StepSyncWorker.enqueue(applicationContext)
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
        if (HealthConnectClient.sdkStatus(this) == HealthConnectClient.SDK_AVAILABLE) {
            val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
            lifecycleScope.launch {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                    StepSyncWorker.enqueue(applicationContext)
                } else {
                    requestPermissions.launch(permissions)
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}