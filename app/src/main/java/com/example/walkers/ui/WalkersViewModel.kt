package com.example.walkers.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkers.data.DailyStepEntity
import com.example.walkers.data.WalkingSessionEntity
import com.example.walkers.domain.StepRepository
import com.example.walkers.domain.StepSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalkersUiState(
    val summary: StepSummary = StepSummary(),
    val dailyHistory: List<DailyStepEntity> = emptyList(),
    val sessions: List<WalkingSessionEntity> = emptyList()
)

class WalkersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StepRepository.get(application)

    val uiState = combine(
        repository.observeSummary(),
        repository.observeDailyHistory(),
        repository.observeSessions()
    ) { summary, dailyHistory, sessions ->
        WalkersUiState(
            summary = summary,
            dailyHistory = dailyHistory,
            sessions = sessions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalkersUiState()
    )

    fun startManualSession() {
        viewModelScope.launch {
            repository.startManualSession()
        }
    }

    fun pauseManualSession() {
        viewModelScope.launch {
            repository.pauseManualSession()
        }
    }

    fun resumeManualSession() {
        viewModelScope.launch {
            repository.resumeManualSession()
        }
    }

    fun finishManualSession() {
        viewModelScope.launch {
            repository.finishManualSession()
        }
    }
}
