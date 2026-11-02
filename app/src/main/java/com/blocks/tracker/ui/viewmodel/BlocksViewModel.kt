package com.blocks.tracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blocks.tracker.data.BlocksRepository
import com.blocks.tracker.data.EntryEntity
import com.blocks.tracker.data.FocusStateStore
import com.blocks.tracker.data.LocalDataBackupManager
import com.blocks.tracker.ui.screens.PendingGapInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class BlocksViewModel(
    private val repository: BlocksRepository,
    val stateStore: FocusStateStore
) : ViewModel() {

    private val _currentDate = MutableStateFlow(LocalDate.now().toString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val entries: StateFlow<List<EntryEntity>> = _currentDate.flatMapLatest { date ->
        repository.getEntriesForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val untitledCount: StateFlow<Int> = _currentDate.flatMapLatest { date ->
        repository.getUntitledCountForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allEntries: StateFlow<List<EntryEntity>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Composer State
    val fromMin = MutableStateFlow(0)
    val toMin = MutableStateFlow(getNowMinutes())
    val isLiveClock = MutableStateFlow(stateStore.isLiveClock)
    val activityTitle = MutableStateFlow(stateStore.draftTitle)

    // Gap dialog state when previous day ended with unlogged time
    val pendingGap = MutableStateFlow<PendingGapInfo?>(null)

    // Undo stack
    private var lastUndoSnapshot: List<EntryEntity>? = null
    val undoMessage = MutableStateFlow<String?>(null)

    init {
        checkAndHandleDayTransition()

        // Ticker for live clock (only updates state when the minute actually changes)
        viewModelScope.launch {
            while (true) {
                // Check if calendar day crossed over while app is running
                val todayStr = LocalDate.now().toString()
                if (_currentDate.value != todayStr && isLiveClock.value) {
                    checkAndHandleDayTransition()
                }

                if (isLiveClock.value) {
                    val nowM = getNowMinutes()
                    if (toMin.value != nowM) {
                        toMin.value = nowM
                    }
                }
                delay(2000)
            }
        }

        // Align fromMin with last entry on date change if not manually saved
        viewModelScope.launch {
            entries.collect { list ->
                if (stateStore.fromMin < 0) {
                    if (list.isNotEmpty()) {
                        fromMin.value = list.last().endMin
                    } else {
                        fromMin.value = 0
                    }
                }
            }
        }
    }

    /**
     * Checks if a new day has arrived.
     * - Resets today's fromMin to 00:00 (or today's latest logged entry endMin)
     * - Checks if yesterday had unlogged time (e.g. 21:00 or 23:30 to 24:00)
     * - Shows yesterday gap dialog if unlogged time exists
     */
    fun checkAndHandleDayTransition() {
        val today = LocalDate.now().toString()
        _currentDate.value = today

        val lastSavedDate = stateStore.lastActiveDate
        val isNewDay = lastSavedDate.isNotBlank() && lastSavedDate != today

        viewModelScope.launch {
            if (isNewDay) {
                // Previous day had unlogged time?
                val yesterdayEntries = repository.getEntriesForDateSync(lastSavedDate).sortedBy { it.startMin }
                val lastEnd = if (yesterdayEntries.isNotEmpty()) yesterdayEntries.last().endMin else 0
                if (lastEnd < 1440) {
                    // There is an unlogged gap from yesterday's lastEnd to midnight (1440)
                    pendingGap.value = PendingGapInfo(
                        date = lastSavedDate,
                        startMin = lastEnd,
                        endMin = 1440
                    )
                }

                // Reset today's composer cleanly starting at 00:00!
                stateStore.fromMin = 0
                fromMin.value = 0
                stateStore.clearDraft()
                activityTitle.value = ""
                isLiveClock.value = true
                stateStore.isLiveClock = true
                toMin.value = getNowMinutes()
            } else {
                // Same day
                if (stateStore.fromMin >= 0) {
                    fromMin.value = stateStore.fromMin
                }
            }

            stateStore.lastActiveDate = today
        }
    }

    fun dismissPendingGap(autoFillAsSleep: Boolean = false) {
        val gap = pendingGap.value ?: return
        viewModelScope.launch {
            if (autoFillAsSleep) {
                repository.addEntry(
                    date = gap.date,
                    fromMin = gap.startMin,
                    toMin = gap.endMin,
                    title = "Sleep"
                )
            } else {
                // Fill as untitled day auto-close
                repository.runDayAutoClose(gap.date)
            }
            pendingGap.value = null
        }
    }

    fun savePendingGapWithTitle(title: String) {
        val gap = pendingGap.value ?: return
        viewModelScope.launch {
            val finalTitle = if (title.isBlank()) "Sleep" else title.trim()
            val newBlock = EntryEntity(
                id = UUID.randomUUID().toString(),
                date = gap.date,
                startMin = gap.startMin,
                endMin = gap.endMin,
                title = finalTitle,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertDirectEntry(newBlock)
            pendingGap.value = null
        }
    }

    private fun getNowMinutes(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    fun addBlock() {
        if (fromMin.value >= toMin.value) return
        val currentSnap = entries.value

        viewModelScope.launch {
            val previous = repository.addEntry(
                date = _currentDate.value,
                fromMin = fromMin.value,
                toMin = toMin.value,
                title = activityTitle.value
            )
            activityTitle.value = ""
            stateStore.clearDraft()
            fromMin.value = toMin.value
            stateStore.fromMin = toMin.value
            isLiveClock.value = true
            stateStore.isLiveClock = true
            stateStore.lastActiveDate = _currentDate.value

            // If overlap occurred
            val diff = previous.size - entries.value.size
            if (diff > 0) {
                lastUndoSnapshot = currentSnap
                undoMessage.value = "Trimmed overlapping blocks"
            }
        }
    }

    fun resumeLiveClock() {
        isLiveClock.value = true
        stateStore.isLiveClock = true
        toMin.value = getNowMinutes()
    }

    fun setDate(date: String) {
        _currentDate.value = date
    }

    fun undo() {
        val snap = lastUndoSnapshot ?: return
        viewModelScope.launch {
            repository.restoreEntries(snap, _currentDate.value)
            lastUndoSnapshot = null
            undoMessage.value = null
        }
    }

    fun clearUndoMessage() {
        undoMessage.value = null
    }

    fun deleteEntry(entry: EntryEntity) {
        val snap = entries.value
        viewModelScope.launch {
            repository.deleteEntry(entry)
            lastUndoSnapshot = snap
            undoMessage.value = "Block removed"
        }
    }

    fun updateEdges(target: EntryEntity, newStart: Int, newEnd: Int, newTitle: String) {
        viewModelScope.launch {
            repository.updateEntryEdges(target.date, target, newStart, newEnd, newTitle)
        }
    }

    fun split(entry: EntryEntity, splitMin: Int) {
        viewModelScope.launch {
            repository.splitEntry(entry, splitMin)
        }
    }

    fun updateTitle(id: String, title: String) {
        viewModelScope.launch {
            repository.updateTitle(id, title)
        }
    }

    fun triggerLocalSnapshot(context: Context) {
        viewModelScope.launch {
            LocalDataBackupManager.createInternalSnapshot(context, repository)
        }
    }
}
