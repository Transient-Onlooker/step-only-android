package com.example.walkers

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkers.service.StepTrackingService
import com.example.walkers.ui.MainScreen
import com.example.walkers.ui.WalkersViewModel
import com.example.walkers.ui.theme.WalkersTheme

class MainActivity : ComponentActivity() {
    private val hasStepCounter = mutableStateOf(false)
    private val hasActivityRecognitionPermission = mutableStateOf(false)

    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityRecognitionPermission.value = granted
        if (granted) {
            requestNotificationPermissionIfNeeded()
            startTrackingServiceIfReady()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        startTrackingServiceIfReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hasStepCounter.value = deviceHasStepCounter()
        hasActivityRecognitionPermission.value = hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)

        setContent {
            val viewModel: WalkersViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            WalkersTheme {
                MainScreen(
                    uiState = uiState,
                    hasStepCounter = hasStepCounter.value,
                    hasActivityRecognitionPermission = hasActivityRecognitionPermission.value,
                    onRequestActivityRecognitionPermission = ::requestActivityRecognitionPermission,
                    onStartSession = viewModel::startManualSession,
                    onFinishSession = viewModel::finishManualSession
                )
            }
        }

        if (hasStepCounter.value) {
            if (hasActivityRecognitionPermission.value) {
                requestNotificationPermissionIfNeeded()
                startTrackingServiceIfReady()
            } else {
                requestActivityRecognitionPermission()
            }
        }
    }

    private fun requestActivityRecognitionPermission() {
        activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasPermission(Manifest.permission.POST_NOTIFICATIONS)) return

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun startTrackingServiceIfReady() {
        if (!hasStepCounter.value) return
        if (!hasActivityRecognitionPermission.value) return

        StepTrackingService.start(this)
    }

    private fun deviceHasStepCounter(): Boolean {
        val sensorManager = getSystemService(SensorManager::class.java)
        return sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
