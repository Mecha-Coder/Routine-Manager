package com.jsk.routinemanager.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Everything the owner enters lives only in this one file (SPEC §3.5, D14: no backup). */
class AppDataJsonTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val everyFieldSet = AppData(
        settings = Settings(theme = Theme.DARK, tone = Tone.STEEL_DRUM, volume = 35),
        todo = listOf(
            OutlineItem("t1", "Parent", depth = 0, done = false),
            OutlineItem("t2", "Child – “quoted” ×", depth = 1, done = true),
        ),
        grocery = listOf(
            GroceryCategory("c1", "Food", collapsed = true, items = listOf(GroceryItem("i1", "Egg", on = true))),
            GroceryCategory("c2", "", items = emptyList()),
        ),
        self = listOf(OutlineItem("s1", "Cut Hair")),
        room = listOf(OutlineItem("r1", "Vacuum", depth = 1)),
        blocks = listOf(Block("b1", "g1", day = 6, start = 1380, end = 1440, title = "Late", subs = listOf("a", "b"))),
    )

    private fun encode(data: AppData): ByteArray =
        ByteArrayOutputStream().also { runBlocking { AppDataSerializer.writeTo(data, it) } }.toByteArray()

    private fun decode(bytes: ByteArray): AppData =
        runBlocking { AppDataSerializer.readFrom(ByteArrayInputStream(bytes)) }

    @Test
    fun roundTripKeepsEveryField() {
        assertEquals(everyFieldSet, decode(encode(everyFieldSet)))
        val sample = SampleData.create()
        assertEquals(sample, decode(encode(sample)))
    }

    @Test
    fun fileUsesTheReadmeDataModelNames() {
        // The README's names (and SPEC's groupId), so the file can be read and fixed by hand.
        val json = encode(everyFieldSet).decodeToString()
        for (part in listOf("\"schemaVersion\":1", "\"theme\":\"dark\"", "\"tone\":\"steel_drum\"", "\"groupId\":\"g1\"", "\"collapsed\":true", "\"on\":true")) {
            assertTrue(part, part in json)
        }
    }

    @Test
    fun everyToneIsSavedUnderItsOwnKeyAndReadBack() {
        // The tone has a hand-written serializer, so a typo or a shared key would silently swap the
        // owner's choice on the next launch.
        for (tone in Tone.entries) {
            val data = AppData(settings = Settings(tone = tone))
            assertTrue(tone.name, "\"tone\":\"${tone.key}\"" in encode(data).decodeToString())
            assertEquals(tone, decode(encode(data)).settings.tone)
        }
        assertEquals(Tone.entries.size, Tone.entries.map { it.key }.toSet().size)
    }

    @Test
    fun aSavedBowlOrPadLoadsAsItsReplacementInsteadOfResettingAllData() {
        // M8b replaced Singing bowl and Warm pad (SPEC D25). A file saved before that still says
        // "bowl" or "pad". If it couldn't be read, the corruption handler would replace the owner's
        // lists with sample data; instead it loads, with the new tone from the same Settings row.
        val before = """{"settings":{"theme":"dark","tone":"%s","volume":35},"todo":[{"id":"a","text":"x"}]}"""
        for ((oldKey, replacement) in listOf("bowl" to Tone.MARIMBA, "pad" to Tone.STEEL_DRUM)) {
            val loaded = decode(before.format(oldKey).encodeToByteArray())
            assertEquals(Settings(Theme.DARK, replacement, 35), loaded.settings)
            assertEquals(listOf(OutlineItem("a", "x")), loaded.todo)
            // The next save writes the new key.
            assertTrue("\"tone\":\"${replacement.key}\"" in encode(loaded).decodeToString())
        }
    }

    @Test
    fun unknownFieldsAreIgnoredSoAFileFromANewerVersionStillLoads() {
        val json = """{"schemaVersion":1,"somethingNew":[1,2],"todo":[{"id":"a","text":"x","extra":true}]}"""
        assertEquals(listOf(OutlineItem("a", "x")), decode(json.encodeToByteArray()).todo)
    }

    @Test(expected = CorruptionException::class)
    fun unreadableBytesAreReportedAsCorruption() {
        decode("{\"todo\":[{\"id\":".encodeToByteArray())
    }

    @Test
    fun aCorruptFileIsKeptBesideTheNewOneAndTheAppStartsFromSampleData() {
        val dir = tmp.newFolder("datastore")
        val file = dir.resolve("app_data.json").apply { writeText("not json {") }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = DataStoreFactory.create(
                serializer = AppDataSerializer,
                corruptionHandler = keepCorruptCopyAndStartOver(file) { _, _ -> },
                scope = scope,
                produceFile = { file },
            )
            val loaded = runBlocking { store.data.first() }

            assertEquals(SampleData.create().blocks.size, loaded.blocks.size)
            // The owner's broken data is not lost: it is copied before the file is replaced.
            val copies = dir.listFiles { f -> f.name.startsWith("app_data.corrupt-") && f.name.endsWith(".json") }!!
            assertEquals(1, copies.size)
            assertEquals("not json {", copies[0].readText())
            // And the main file now holds the sample data, so the next launch reads it normally.
            assertEquals(loaded, decode(file.readBytes()))
        } finally {
            scope.cancel()
        }
    }
}
