package com.jsk.routinemanager.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** DataStore reads and writes [AppData] as JSON (ARCHITECTURE §4). */
object AppDataSerializer : Serializer<AppData> {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Only seen before the first save; [DataRepository] writes sample data on first run. */
    override val defaultValue = AppData()

    override suspend fun readFrom(input: InputStream): AppData =
        try {
            json.decodeFromString(AppData.serializer(), input.readBytes().decodeToString())
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            throw CorruptionException("app data is not valid JSON for AppData", e)
        }

    override suspend fun writeTo(t: AppData, output: OutputStream) {
        output.write(json.encodeToString(AppData.serializer(), t).encodeToByteArray())
    }
}

/**
 * An unreadable [file] is copied to `<name>.corrupt-<epochMillis>.json` beside it, so the lists can
 * still be recovered by hand, and the app starts again from sample data (SPEC §3.5).
 */
fun keepCorruptCopyAndStartOver(
    file: File,
    log: (String, Throwable) -> Unit,
) = ReplaceFileCorruptionHandler { e ->
    val copy = File(file.parentFile, "${file.nameWithoutExtension}.corrupt-${System.currentTimeMillis()}.json")
    file.copyTo(copy, overwrite = true)
    log("Unreadable ${file.name}; kept as ${copy.name}, starting from sample data", e)
    SampleData.create()
}
