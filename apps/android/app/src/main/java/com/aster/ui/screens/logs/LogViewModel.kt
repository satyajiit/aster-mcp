package com.aster.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aster.data.local.db.ToolCallLog
import com.aster.data.local.db.ToolCallLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val dao: ToolCallLogDao
) : ViewModel() {

    val logs: StateFlow<List<ToolCallLog>> = dao.getRecent(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = dao.getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successCount: StateFlow<Int> = dao.getSuccessCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}
