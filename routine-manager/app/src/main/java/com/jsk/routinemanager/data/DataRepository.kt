package com.jsk.routinemanager.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How long after the last change the data is written (ARCHITECTURE §4). */
const val SAVE_DELAY_MS = 500L

/**
 * The single source of truth for saved data (ARCHITECTURE §3, §4). A change shows at once; it is
 * written [SAVE_DELAY_MS] after the last change, or at once by [flush].
 *
 * Call [update] and [flush] from the main thread.
 */
class DataRepository(
    private val store: DataStore<AppData>,
    private val scope: CoroutineScope,
    private val hasSavedFile: () -> Boolean,
) {
    private val state = MutableStateFlow<AppData?>(null)

    /** Null until the saved file has been read, a few milliseconds after launch. */
    val data: StateFlow<AppData?> = state.asStateFlow()

    private var pendingSave: Job? = null

    init {
        scope.launch {
            // First run: save the sample data at once, so it is the same data after a restart (SPEC D8).
            state.value = if (hasSavedFile()) store.data.first() else store.updateData { SampleData.create() }
        }
    }

    fun update(transform: (AppData) -> AppData) {
        state.update { it?.let(transform) }
        if (state.value == null) return
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(SAVE_DELAY_MS)
            save()
        }
    }

    /** Writes a pending change now. Called when the app goes to the background (SPEC §3.5). */
    fun flush() {
        if (pendingSave?.isActive != true) return
        pendingSave?.cancel()
        pendingSave = scope.launch { save() }
    }

    private suspend fun save() {
        val latest = state.value ?: return
        store.updateData { latest }
    }
}
