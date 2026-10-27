package com.jsk.routinemanager

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.jsk.routinemanager.data.AppDataSerializer
import com.jsk.routinemanager.data.DataRepository
import com.jsk.routinemanager.data.keepCorruptCopyAndStartOver
import com.jsk.routinemanager.debug.DebugEntry
import com.jsk.routinemanager.sound.BlockChime
import com.jsk.routinemanager.sound.BlockVibrator
import com.jsk.routinemanager.sound.TonePlayer
import com.jsk.routinemanager.time.AppClock
import com.jsk.routinemanager.time.minuteTicks
import com.jsk.routinemanager.time.timeChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import java.time.LocalDateTime

class RoutineApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** The app's few singletons (ARCHITECTURE §1). No DI framework. */
class AppContainer(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** `files/datastore/app_data.json`. */
    private val dataFile = context.dataStoreFile("app_data.json")

    val repository = DataRepository(
        store = DataStoreFactory.create(
            serializer = AppDataSerializer,
            corruptionHandler = keepCorruptCopyAndStartOver(dataFile) { message, e -> Log.e(TAG, message, e) },
            produceFile = { dataFile },
        ),
        scope = scope,
        hasSavedFile = dataFile::exists,
    )

    /** The phone's clock; in debug builds the debug screen can move it (SPEC §3.6). */
    val clock: AppClock = DebugEntry.clock

    /** Cold: each collector gets its own timer and receiver, running only while it collects. */
    val minuteTicks: Flow<LocalDateTime> = minuteTicks(clock, merge(context.timeChanges(), DebugEntry.clockChanges))

    val tonePlayer = TonePlayer(context)

    val blockVibrator = BlockVibrator(context)

    /**
     * Plays the selected tone at its volume when a block starts, or vibrates in Vibrate mode
     * (SPEC §3.1, D26). Fed by MainActivity while visible.
     */
    val chime = BlockChime(MainScope()) {
        repository.data.value?.settings?.let {
            if (it.vibrate) blockVibrator.vibrate() else tonePlayer.play(it.tone, it.volume)
        }
    }

    private companion object {
        const val TAG = "RoutineApp"
    }
}
