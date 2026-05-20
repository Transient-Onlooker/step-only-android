package com.example.walkers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.walkers.data.DailyStepEntity
import com.example.walkers.data.WalkingSessionEntity
import com.example.walkers.domain.StepSummary
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainScreen(
    uiState: WalkersUiState,
    hasStepCounter: Boolean,
    hasActivityRecognitionPermission: Boolean,
    onRequestActivityRecognitionPermission: () -> Unit,
    onStartSession: () -> Unit,
    onFinishSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (!hasStepCounter) {
            BlockingMessage(
                title = "걸음 수 센서 없음",
                body = "이 기기에서는 걸음 수 센서를 지원하지 않습니다.",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        if (!hasActivityRecognitionPermission) {
            PermissionMessage(
                onRequestActivityRecognitionPermission = onRequestActivityRecognitionPermission,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("오늘", "날짜별", "세션")

        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> TodayScreen(
                    summary = uiState.summary,
                    onStartSession = onStartSession,
                    onFinishSession = onFinishSession
                )

                1 -> DailyHistoryScreen(dailyHistory = uiState.dailyHistory)
                2 -> SessionHistoryScreen(sessions = uiState.sessions)
            }
        }
    }
}

@Composable
private fun TodayScreen(
    summary: StepSummary,
    onStartSession: () -> Unit,
    onFinishSession: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "오늘",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatSteps(summary.todaySteps),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp
            )
            Text(
                text = formatDistance(summary.todayDistanceKm),
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            StatCard(
                title = "전체 누적",
                steps = summary.totalSteps,
                distanceKm = summary.totalDistanceKm
            )
        }

        item {
            val activeSession = summary.activeSession
            if (activeSession == null) {
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("기록 시작")
                }
            } else {
                ActiveSessionCard(
                    session = activeSession,
                    onFinishSession = onFinishSession
                )
            }
        }

        item {
            Text(
                text = "최근 기록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (summary.recentSessions.isEmpty()) {
            item {
                Text(
                    text = "저장된 세션이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(summary.recentSessions, key = { it.id }) { session ->
                SessionRow(session = session)
            }
        }

        item {
            Text(
                text = "정확한 백그라운드 기록을 위해 앱을 강제 종료하지 않는 것이 좋습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DailyHistoryScreen(dailyHistory: List<DailyStepEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (dailyHistory.isEmpty()) {
            item {
                Text(
                    text = "저장된 날짜별 기록이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(dailyHistory, key = { it.date }) { dailyStep ->
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dailyStep.date,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatSteps(dailyStep.steps)} / ${formatDistance(dailyStep.distanceKm)}"
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SessionHistoryScreen(sessions: List<WalkingSessionEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sessions.isEmpty()) {
            item {
                Text(
                    text = "저장된 세션이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                SessionRow(session = session)
            }
        }
    }
}

@Composable
private fun ActiveSessionCard(
    session: WalkingSessionEntity,
    onFinishSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "기록 중",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatSteps(session.steps),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = formatDistance(session.distanceKm))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "시작 시간 ${formatTime(session.startedAt)}")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onFinishSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("기록 끝")
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    steps: Long,
    distanceKm: Double
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatSteps(steps),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = formatDistance(distanceKm))
        }
    }
}

@Composable
private fun SessionRow(session: WalkingSessionEntity) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row {
                Text(
                    text = formatDate(session.startedAt),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${formatTime(session.startedAt)} ~ ${session.endedAt?.let(::formatTime) ?: "진행 중"}"
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatSteps(session.steps)} / ${formatDistance(session.distanceKm)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionMessage(
    onRequestActivityRecognitionPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "신체 활동 권한 필요",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "걸음 수 센서를 사용하려면 권한이 필요합니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestActivityRecognitionPermission) {
            Text("권한 허용")
        }
    }
}

@Composable
private fun BlockingMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatSteps(steps: Long): String {
    return "${NumberFormat.getNumberInstance(Locale.KOREA).format(steps)} 걸음"
}

private fun formatDistance(distanceKm: Double): String {
    return String.format(Locale.KOREA, "%.2f km", distanceKm)
}

private fun formatDate(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
}

private fun formatTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}
