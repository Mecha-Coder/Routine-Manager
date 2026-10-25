package com.jsk.routinemanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jsk.routinemanager.RoutineApp
import com.jsk.routinemanager.data.Block
import com.jsk.routinemanager.data.DataRepository
import com.jsk.routinemanager.data.Theme
import com.jsk.routinemanager.domain.ClockText
import com.jsk.routinemanager.domain.RoutineResolver
import com.jsk.routinemanager.domain.RoutineState
import com.jsk.routinemanager.domain.TimeFormat
import com.jsk.routinemanager.domain.dayIndex
import com.jsk.routinemanager.domain.minuteOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

/** The dashboard's left panel at one minute. */
data class DashboardUi(
    val clock: ClockText,
    /** `Mon`. */
    val day: String,
    /** `19 Oct`. */
    val date: String,
    val routine: RoutineState,
    /** Null at Bedtime, which has no progress bar. */
    val bar: RoutineBar?,
)

data class RoutineBar(
    /** 0..1. */
    val progress: Float,
    /** `4hr 30min`. */
    val left: String,
    val start: ClockText,
    val end: ClockText,
)

internal fun dashboardUi(blocks: List<Block>, now: LocalDateTime): DashboardUi {
    val minute = minuteOfDay(now)
    val routine = RoutineResolver.resolve(blocks, now)
    val bar = (routine as? RoutineState.Running)?.let {
        RoutineBar(
            progress = ((minute - it.start) / (it.end - it.start)).coerceIn(0.0, 1.0).toFloat(),
            left = TimeFormat.left(it.end, minute),
            start = TimeFormat.clock(it.start),
            end = TimeFormat.clock(it.end),
        )
    }
    return DashboardUi(
        clock = TimeFormat.clock(minute.toInt()),
        day = TimeFormat.day(dayIndex(now.toLocalDate())),
        date = TimeFormat.date(now.toLocalDate()),
        routine = routine,
        bar = bar,
    )
}

/** The screen on show. No navigation library: system Back is handled by each screen (ARCHITECTURE §1). */
sealed interface Screen {
    data object Home : Screen

    /** Debug builds only (SPEC §3.6). */
    data object Debug : Screen
}

class MainViewModel(repository: DataRepository, minuteTicks: Flow<LocalDateTime>) : ViewModel() {

    private val shown = MutableStateFlow<Screen>(Screen.Home)

    /** Kept here so it survives rotation. */
    val screen: StateFlow<Screen> = shown.asStateFlow()

    fun show(screen: Screen) {
        shown.value = screen
    }

    // List edits don't touch the blocks, so they don't recompute the dashboard.
    private val blocks = repository.data.filterNotNull().map { it.blocks }.distinctUntilChanged()

    /**
     * Null until the saved data is loaded. Changes once a minute and when the blocks change. The
     * ticker runs only while the UI collects this, i.e. while the app is visible.
     */
    val dashboard: StateFlow<DashboardUi?> = combine(blocks, minuteTicks, ::dashboardUi)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val darkTheme: StateFlow<Boolean> = repository.data
        .map { it?.settings?.theme == Theme.DARK }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RoutineApp).container
                MainViewModel(container.repository, container.minuteTicks)
            }
        }
    }
}
