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
import com.jsk.routinemanager.data.GroceryCategory
import com.jsk.routinemanager.data.OutlineItem
import com.jsk.routinemanager.data.Settings
import com.jsk.routinemanager.data.Theme
import com.jsk.routinemanager.data.Tone
import com.jsk.routinemanager.data.newId
import com.jsk.routinemanager.domain.ClockText
import com.jsk.routinemanager.domain.GroceryOps
import com.jsk.routinemanager.domain.OutlineOps
import com.jsk.routinemanager.domain.RoutineResolver
import com.jsk.routinemanager.domain.RoutineState
import com.jsk.routinemanager.domain.ScreenRules
import com.jsk.routinemanager.domain.TimeFormat
import com.jsk.routinemanager.domain.dayIndex
import com.jsk.routinemanager.domain.minuteOfDay
import com.jsk.routinemanager.sound.TonePlayer
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

    /** Full screen, over everything (README › Screens › 4). */
    data object Settings : Screen

    /** Opened from Settings; Back returns there (README › Screens › 5). */
    data object Timetable : Screen

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

class MainViewModel(
    private val repository: DataRepository,
    minuteTicks: Flow<LocalDateTime>,
    private val tonePlayer: TonePlayer,
) : ViewModel() {

    private val shown = MutableStateFlow<Screen>(Screen.Home)

    /** Kept here so it survives rotation. */
    val screen: StateFlow<Screen> = shown.asStateFlow()

    fun show(screen: Screen) {
        shown.value = screen
    }

    private val opened = MutableStateFlow<ListKey?>(null)

    /** The list shown in the list panel, or null for the menu. Survives rotation; not saved (SPEC §3.5). */
    val openList: StateFlow<ListKey?> = opened.asStateFlow()

    /** Opening a list shows it in view mode. */
    fun showList(list: ListKey) {
        editingState.value = false
        opened.value = list
    }

    /** Leaving the list also leaves edit mode and cleans up the edits (README › Saving edits). */
    fun showMenu() {
        finishEditing()
        opened.value = null
    }

    private val editingState = MutableStateFlow(false)

    /** Edit mode for the open list. Survives rotation; not saved (SPEC §3.5). */
    val editing: StateFlow<Boolean> = editingState.asStateFlow()

    fun startEditing() {
        editingState.value = true
    }

    /** Done: drops empty items and nameless empty categories, and fixes depths (README › Saving edits). */
    fun finishEditing() {
        if (!editingState.value) return
        editingState.value = false
        repository.update {
            it.copy(
                todo = OutlineOps.cleanup(it.todo),
                self = OutlineOps.cleanup(it.self),
                room = OutlineOps.cleanup(it.room),
                grocery = GroceryOps.cleanup(it.grocery),
            )
        }
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

    // Edit mode. The functions that add an item return its id, so the list can focus it.

    private fun updateOutline(list: ListKey, transform: (List<OutlineItem>) -> List<OutlineItem>) =
        repository.update {
            when (list) {
                ListKey.TODO -> it.copy(todo = transform(it.todo))
                ListKey.SELF -> it.copy(self = transform(it.self))
                ListKey.ROOM -> it.copy(room = transform(it.room))
                ListKey.GROCERY -> error("Grocery is not an outline list")
            }
        }

    fun editOutlineText(list: ListKey, id: String, text: String) = updateOutline(list) { OutlineOps.editText(it, id, text) }

    fun insertOutlineItemAfter(list: ListKey, id: String): String =
        newId().also { new -> updateOutline(list) { OutlineOps.insertAfter(it, id, new) } }

    fun appendOutlineItem(list: ListKey): String = newId().also { new -> updateOutline(list) { OutlineOps.append(it, new) } }

    fun deleteOutlineItem(list: ListKey, id: String) = updateOutline(list) { OutlineOps.delete(it, id) }

    /** An indent drag released at [depth]. */
    fun setOutlineDepth(list: ListKey, id: String, depth: Int) = updateOutline(list) { OutlineOps.setDepth(it, id, depth) }

    /** A reorder dropped at [index] in the list without the item and its children. */
    fun moveOutlineItem(list: ListKey, id: String, index: Int, depth: Int) =
        updateOutline(list) { OutlineOps.moveSubtree(it, id, index, depth) }

    private fun updateGrocery(transform: (List<GroceryCategory>) -> List<GroceryCategory>) =
        repository.update { it.copy(grocery = transform(it.grocery)) }

    fun renameCategory(categoryId: String, name: String) = updateGrocery { GroceryOps.rename(it, categoryId, name) }

    fun deleteCategory(categoryId: String) = updateGrocery { GroceryOps.deleteCategory(it, categoryId) }

    fun addCategory(): String = newId().also { new -> updateGrocery { GroceryOps.addCategory(it, new) } }

    fun editGroceryItem(categoryId: String, itemId: String, text: String) =
        updateGrocery { GroceryOps.editItem(it, categoryId, itemId, text) }

    fun deleteGroceryItem(categoryId: String, itemId: String) = updateGrocery { GroceryOps.deleteItem(it, categoryId, itemId) }

    /** After [afterItemId], or at the end of the category when it is null. */
    fun addGroceryItem(categoryId: String, afterItemId: String?): String =
        newId().also { new -> updateGrocery { GroceryOps.addItem(it, categoryId, afterItemId, new) } }

    /** Null until the saved data is loaded. */
    val settings: StateFlow<Settings?> = repository.data
        .map { it?.settings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /** A tone or preview is playing (the Settings Preview / Stop button). */
    val tonePlaying: StateFlow<Boolean> = tonePlayer.playing

    private fun updateSettings(transform: (Settings) -> Settings) =
        repository.update { it.copy(settings = transform(it.settings)) }

    /** Saved at once; during Bedtime the screen stays dark until Bedtime ends (SPEC §3.2). */
    fun setTheme(theme: Theme) = updateSettings { it.copy(theme = theme) }

    /** Tapping a tone row selects it and plays it as a preview (README › Screens › 4). */
    fun pickTone(tone: Tone) {
        updateSettings { it.copy(tone = tone) }
        repository.data.value?.let { tonePlayer.play(tone, it.settings.volume) }
    }

    /** A playing tone follows the slider at once (SPEC §3.1). */
    fun setVolume(volume: Int) {
        updateSettings { it.copy(volume = volume) }
        tonePlayer.setVolume(volume)
    }

    /** "Preview sound" plays the selected tone; while playing, the button stops it. */
    fun togglePreview() {
        if (tonePlayer.playing.value) {
            tonePlayer.stop()
        } else {
            repository.data.value?.settings?.let { tonePlayer.play(it.tone, it.volume) }
        }
    }

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

    /** The weekly timetable's blocks (README › Screens › 5); null until loaded. */
    val timetableBlocks: StateFlow<List<Block>?> = blocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    /**
     * The timetable's minute, for today's column, the now line and the scroll on open. Null until
     * the first tick, and null again once the timetable closes, so reopening it never scrolls to a
     * stale minute.
     */
    val now: StateFlow<LocalDateTime?> = minute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    /** The block on now, which the timetable fills with the accent; null in Free time and at Bedtime. */
    val currentBlockId: StateFlow<String?> = routine
        .map { routine -> (routine as? RoutineState.Running)?.takeUnless { it.isFreeTime }?.key }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as RoutineApp).container
                MainViewModel(container.repository, container.minuteTicks, container.tonePlayer)
            }
        }
    }
}
