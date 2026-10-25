package com.jsk.routinemanager.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * SPEC §3.5: a change shows at once and reaches disk soon after, typing must not write on every
 * key, and nothing pending is lost when the app goes to the background.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataRepositoryTest {

    private class FakeStore(initial: AppData) : DataStore<AppData> {
        val writes = mutableListOf<AppData>()
        override val data = MutableStateFlow(initial)
        override suspend fun updateData(transform: suspend (t: AppData) -> AppData): AppData =
            transform(data.value).also {
                writes += it
                data.value = it
            }
    }

    private val saved = AppData(todo = listOf(OutlineItem("a", "Saved item")))

    private fun TestScope.loadedRepository(store: FakeStore): DataRepository =
        DataRepository(store, backgroundScope, hasSavedFile = { true }).also { runCurrent() }

    private fun rename(text: String): (AppData) -> AppData = { d -> d.copy(todo = listOf(d.todo[0].copy(text = text))) }

    @Test
    fun firstRunSavesTheSampleDataSoTheSameDataComesBackAfterARestart() = runTest {
        val store = FakeStore(AppData())
        val repo = DataRepository(store, backgroundScope, hasSavedFile = { false })
        runCurrent()

        assertEquals(1, store.writes.size)
        assertEquals(SampleData.create().blocks.size, store.writes[0].blocks.size)
        assertEquals(store.writes[0], repo.data.value)
    }

    @Test
    fun aSavedFileIsLoadedAndNotReplaced() = runTest {
        val store = FakeStore(saved)
        val repo = loadedRepository(store)

        assertEquals(saved, repo.data.value)
        assertEquals(0, store.writes.size)
    }

    @Test
    fun aChangeShowsAtOnceAndIsWrittenHalfASecondLater() = runTest {
        val store = FakeStore(saved)
        val repo = loadedRepository(store)

        repo.update(rename("Edited"))
        assertEquals("Edited", repo.data.value!!.todo[0].text)

        advanceTimeBy(SAVE_DELAY_MS - 1)
        runCurrent()
        assertEquals(0, store.writes.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Edited"), store.writes.map { it.todo[0].text })
    }

    @Test
    fun aBurstOfChangesLikeTypingIsWrittenOnceWithTheLatestText() = runTest {
        val store = FakeStore(saved)
        val repo = loadedRepository(store)

        for (text in listOf("E", "Ed", "Edi", "Edit")) {
            repo.update(rename(text))
            advanceTimeBy(100)
        }
        advanceTimeBy(SAVE_DELAY_MS)
        runCurrent()

        assertEquals(listOf("Edit"), store.writes.map { it.todo[0].text })
    }

    @Test
    fun flushWritesAPendingChangeAtOnceSoGoingToTheBackgroundLosesNothing() = runTest {
        val store = FakeStore(saved)
        val repo = loadedRepository(store)

        repo.update(rename("Edited"))
        repo.flush()
        runCurrent()
        assertEquals(listOf("Edited"), store.writes.map { it.todo[0].text })

        // The debounced write it replaced does not write a second time.
        advanceTimeBy(SAVE_DELAY_MS * 2)
        runCurrent()
        assertEquals(1, store.writes.size)
    }

    @Test
    fun flushWithNothingPendingDoesNotWrite() = runTest {
        val store = FakeStore(saved)
        val repo = loadedRepository(store)
        assertNotNull(repo.data.value)

        repo.flush()
        runCurrent()
        assertEquals(0, store.writes.size)
    }
}
