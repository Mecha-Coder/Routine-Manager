package com.jsk.routinemanager.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

const val SCHEMA_VERSION = 1

/**
 * Everything the app saves, as one JSON document (SPEC §3.5): README › Data model plus
 * [schemaVersion]. Bump the version and migrate in [AppDataSerializer] if the shape changes after
 * release.
 */
@Serializable
data class AppData(
    val schemaVersion: Int = SCHEMA_VERSION,
    val settings: Settings = Settings(),
    val todo: List<OutlineItem> = emptyList(),
    val grocery: List<GroceryCategory> = emptyList(),
    val self: List<OutlineItem> = emptyList(),
    val room: List<OutlineItem> = emptyList(),
    val blocks: List<Block> = emptyList(),
)

@Serializable
data class Settings(
    val theme: Theme = Theme.LIGHT,
    /** Soft chime, not the README's bowl: the bowl and the pad are too soft to notice (SPEC D25). */
    val tone: Tone = Tone.CHIME,
    /** 0..100. */
    val volume: Int = 60,
)

@Serializable
enum class Theme {
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}

/** In Settings row order. [key] is the name saved in the file. */
@Serializable(with = ToneSerializer::class)
enum class Tone(val key: String) {
    MARIMBA("marimba"),
    CHIME("chime"),
    STEEL_DRUM("steel_drum"),
}

/**
 * Saves a [Tone] by its key, and still reads the keys of the two tones M8b replaced (SPEC D25):
 * `bowl` is now Marimba and `pad` Steel drum, each in the replaced tone's row. A custom serializer
 * because `@JsonNames` still needs an experimental opt-in (kotlinx.serialization 1.12.0).
 */
object ToneSerializer : KSerializer<Tone> {
    private val replacedKeys = mapOf("bowl" to Tone.MARIMBA, "pad" to Tone.STEEL_DRUM)

    override val descriptor = PrimitiveSerialDescriptor("com.jsk.routinemanager.data.Tone", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Tone) = encoder.encodeString(value.key)

    override fun deserialize(decoder: Decoder): Tone {
        val key = decoder.decodeString()
        return Tone.entries.firstOrNull { it.key == key } ?: replacedKeys[key]
            ?: throw SerializationException("Unknown tone \"$key\"")
    }
}

/**
 * A To-Do, Self or Room item. Lists are stored flat in display order; a child (depth 1) always
 * follows its parent. [done] is used only by To-Do.
 */
@Serializable
data class OutlineItem(
    val id: String,
    val text: String,
    val depth: Int = 0,
    val done: Boolean = false,
)

@Serializable
data class GroceryCategory(
    val id: String,
    val name: String,
    val collapsed: Boolean = false,
    val items: List<GroceryItem> = emptyList(),
)

/** [on] = selected = "need to buy". */
@Serializable
data class GroceryItem(
    val id: String,
    val text: String,
    val on: Boolean = false,
)

/**
 * One day's copy of a routine block. Copies saved together for several days share [groupId].
 * [day] is 0..6 for Mon..Sun. [start] and [end] are minutes from midnight on 15-minute steps;
 * [end] can be 1440 (midnight, the end of the day). At most 5 [subs].
 */
@Serializable
data class Block(
    val id: String,
    val groupId: String,
    val day: Int,
    val start: Int,
    val end: Int,
    val title: String,
    val subs: List<String> = emptyList(),
)

fun newId(): String = UUID.randomUUID().toString()
