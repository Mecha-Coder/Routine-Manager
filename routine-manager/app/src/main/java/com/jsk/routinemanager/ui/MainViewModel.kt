package com.jsk.routinemanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jsk.routinemanager.RoutineApp
import com.jsk.routinemanager.data.AppData
import com.jsk.routinemanager.data.Block
import com.jsk.routinemanager.data.DataRepository
import com.jsk.routinemanager.domain.ClockText
import com.jsk.routinemanager.domain.GroceryOps
import com.jsk.routinemanager.domain.OutlineOps
import com.jsk.routinemanager.domain.RoutineResolver
import com.jsk.routinemanager.domain.RoutineState
import com.jsk.routinemanager.domain.ScreenRules
import com.jsk.routinemanager.domain.TimeFormat
import com.jsk.routinemanager.domain.dayIndex
import com.jsk.routinemanager.domain.minuteOfDay
import com.jsk.routinemanager.ui.lists.ListKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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

/** The list menu's counts (README › List menu); Self and Room have none. */
data class MenuBadges(
    /** Not-done to-dos at every level. */
    val todo: Int,
    /** Selected grocery items in all categories. */
    val grocery: Int,
)

internal fun menuBadges(data: AppData) =
    MenuBadges(todo = OutlineOps.openCount(data.todo), grocery = GroceryOps.selectedCount(data.grocery))

class MainViewModel(private val repository: DataRepository, minuteTicks: Flow<LocalDateTime>) : ViewModel() {

    private val shown = MutableStateFlow<Screen>(Screen.Home)

    /** Kept here so it survives rotation. */
    val screen: StateFlow<Screen> = shown.asStateFlow()

    fun show(screen: Screen) {
        shown.value = screen
    }

    private val opened = MutableStateFlow<ListKey?>(null)

    /** The list shown in the list panel, or null for the menu. Survives rotation; not saved (SPEC §3.5). */
    val openList: StateFlow<ListKey?> = opened.asStateFlow()

    fun showList(list: ListKey) {
        opened.value = list
    }

    fun showMenu() {
        opened.value = null
    }

    /** The saved lists for the list panel; null until loaded. */
    val data: StateFlow<AppData?> = repository.data

    val badges: StateFlow<MenuBadges?> = repository.data
        .map { it?.let(::menuBadges) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun toggleDone(id: String) = repository.update { it.copy(todo = OutlineOps.toggleDone(it.todo, id)) }

    fun toggleGroceryItem(categoryId: String, itemId: String) =
        repository.update { it.copy(grocery = GroceryOps.toggle(it.grocery, categoryId, itemId)) }

    fun toggleCollapsed(categoryId: String) =
        repository.update { it.copy(grocery = GroceryOps.toggleCollapsed(it.grocery, categoryId)) }

    // List edits don't touch the blocks, so they don't recompute the dashboard.
    private val blocks = repository.data.filterNotNull().map { it.blocks }.distinctUntilChanged()

    /**
     * One ticker for every collector, so the app wakes once per minute. It runs only while something
     * collects, i.e. while the app is visible. The last minute is dropped when it stops, so a return
     * from the background never starts from a stale minute.
     */
    private val minute = minuteTicks.shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
        replay = 1,
    )

    /** Null until the saved data is loaded. Changes once a minute and when the blocks change. */
    val dashboard: StateFlow<DashboardUi?> = combine(blocks, minute, ::dashboardUi)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * The routine at every minute tick (even when it is unchanged) and whenever the blocks change,
     * on every screen and in both orientations. ScreenPolicy checks the battery and Bedtime on each.
     */
    val routine: Flow<RoutineState> = combine(blocks, minute, RoutineResolver::resolve)

    private val theme = repository.data.filterNotNull().map { it.settings.theme }.distinctUntilChanged()

    /** The chosen theme, or dark during Bedtime whatever the setting (SPEC §3.2). */
    val darkTheme: StateFlow<Boolean> = combine(theme, routine, ScreenRules::darkPalette)
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
