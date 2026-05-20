package com.example.walkers.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.walkers.MainActivity
import com.example.walkers.R
import com.example.walkers.domain.StepRepository
import com.example.walkers.domain.StepSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class StepTrackingService : Service(), SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: StepRepository
    private lateinit var sensorManager: SensorManager
    private lateinit var notificationManager: NotificationManager
    private var registered = false
    private var foregroundStarted = false
    private var observingSummary = false
    private var currentSummary = StepSummary()

    override fun onCreate() {
        super.onCreate()
        repository = StepRepository.get(this)
        sensorManager = getSystemService(SensorManager::class.java)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasActivityRecognitionPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundTracking()
        observeSummaryUpdates()
        handleAction(intent?.action)
        registerStepCounter()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val rawSteps = event.values.firstOrNull()?.toLong() ?: return
        scope.launch {
            repository.recordRawSteps(rawSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        if (registered) {
            sensorManager.unregisterListener(this)
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun handleAction(action: String?) {
        when (action) {
            ACTION_START_SESSION -> scope.launch { repository.startManualSession() }
            ACTION_PAUSE_SESSION -> scope.launch { repository.pauseManualSession() }
            ACTION_RESUME_SESSION -> scope.launch { repository.resumeManualSession() }
            ACTION_STOP_SESSION -> scope.launch { repository.finishManualSession() }
        }
    }

    private fun observeSummaryUpdates() {
        if (observingSummary) return
        observingSummary = true

        scope.launch {
            repository.observeSummary().collectLatest { summary ->
                currentSummary = summary
                updateNotification(summary)
            }
        }
    }

    private fun registerStepCounter() {
        if (registered) return

        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounter == null) {
            stopSelf()
            return
        }

        registered = sensorManager.registerListener(
            this,
            stepCounter,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun startForegroundTracking() {
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(currentSummary),
            foregroundType
        )
        foregroundStarted = true
    }

    private fun updateNotification(summary: StepSummary) {
        if (!foregroundStarted) return
        if (!canPostNotification()) return

        notificationManager.notify(NOTIFICATION_ID, buildNotification(summary))
    }

    private fun buildNotification(summary: StepSummary): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activeSession = summary.activeSession
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("오늘의 걸음 수")
            .setContentText("${formatSteps(summary.todaySteps)} / ${formatDistance(summary.todayDistanceKm)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildExpandedText(summary)))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)

        if (activeSession == null) {
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                "기록 시작",
                servicePendingIntent(ACTION_START_SESSION, REQUEST_START_SESSION)
            )
        } else {
            val pauseOrResumeAction = if (activeSession.isPaused) {
                ACTION_RESUME_SESSION
            } else {
                ACTION_PAUSE_SESSION
            }
            val pauseOrResumeTitle = if (activeSession.isPaused) "기록 재개" else "기록 일시중지"

            builder.addAction(
                R.drawable.ic_launcher_foreground,
                pauseOrResumeTitle,
                servicePendingIntent(pauseOrResumeAction, REQUEST_PAUSE_RESUME_SESSION)
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                "기록 중지",
                servicePendingIntent(ACTION_STOP_SESSION, REQUEST_STOP_SESSION)
            )
        }

        return builder.build()
    }

    private fun buildExpandedText(summary: StepSummary): String {
        val activeSession = summary.activeSession
        if (activeSession == null) {
            return "오늘 ${formatSteps(summary.todaySteps)} / ${formatDistance(summary.todayDistanceKm)}"
        }

        val sessionState = if (activeSession.isPaused) "세션 일시중지" else "세션 기록 중"
        return "$sessionState\n${formatSteps(activeSession.steps)} / ${formatDistance(activeSession.distanceKm)}"
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, StepTrackingService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "걸음 수 기록",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotification(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun formatSteps(steps: Long): String {
        return "${NumberFormat.getNumberInstance(Locale.KOREA).format(steps)} 걸음"
    }

    private fun formatDistance(distanceKm: Double): String {
        return String.format(Locale.KOREA, "%.2f km", distanceKm)
    }

    companion object {
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START_SESSION = "com.example.walkers.action.START_SESSION"
        private const val ACTION_PAUSE_SESSION = "com.example.walkers.action.PAUSE_SESSION"
        private const val ACTION_RESUME_SESSION = "com.example.walkers.action.RESUME_SESSION"
        private const val ACTION_STOP_SESSION = "com.example.walkers.action.STOP_SESSION"
        private const val REQUEST_START_SESSION = 2001
        private const val REQUEST_PAUSE_RESUME_SESSION = 2002
        private const val REQUEST_STOP_SESSION = 2003

        fun start(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
