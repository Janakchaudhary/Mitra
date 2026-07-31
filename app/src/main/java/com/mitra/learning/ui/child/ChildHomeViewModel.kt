package com.mitra.learning.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitra.learning.learning.limits.LearningLimitService
import com.mitra.learning.network.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class ChildHomeUiState(
    val loading: Boolean = true,
    val canPlay: Boolean = true,
    val usedTodayMinutes: Int = 0,
    val remainingTodayMinutes: Int = 0,
    val online: Boolean = true,
    val messageGujarati: String? = null,
)

class ChildHomeViewModel(
    private val limitService: LearningLimitService,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {
    private val _state = MutableStateFlow(ChildHomeUiState())
    val state: StateFlow<ChildHomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val limits = limitService.status()
        val online = networkMonitor.isOnline()
        _state.value = ChildHomeUiState(
            loading = false,
            canPlay = limits.canStart,
            usedTodayMinutes = limits.usedTodayMinutes,
            remainingTodayMinutes = limits.remainingTodayMinutes,
            online = online,
            messageGujarati = when {
                !limits.canStart -> "આજનો શીખવાનો સમય પૂરો થયો. હવે ફોનને આરામ આપીએ. 😊"
                !online -> "Internet નથી — સ્થાનિક રમતો અને તૈયાર પુસ્તકો ચાલુ રહેશે."
                else -> null
            },
        )
    }
}
