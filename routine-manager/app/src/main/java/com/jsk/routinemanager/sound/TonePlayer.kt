package com.jsk.routinemanager.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.jsk.routinemanager.R
import com.jsk.routinemanager.data.Tone
import com.jsk.routinemanager.domain.toneGain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays one tone at a time on the media stream (SPEC §3.1, D10; ARCHITECTURE §7). Each play gets a
 * new MediaPlayer, released when the sound ends or is stopped, so no audio memory is held between
 * chimes. While it plays, other apps' audio is ducked.
 *
 * Main thread only.
 */
class TonePlayer(context: Context) {

    private val resources = context.applicationContext.resources
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** "May duck": the system lowers a podcast or music while the tone plays, then restores it. */
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change ->
            // A call or another app took the audio: the tone stops rather than playing over it.
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
        }
        .build()

    private var player: MediaPlayer? = null

    private val isPlaying = MutableStateFlow(false)

    /** True from [play] until the tone ends or is stopped (the Settings Preview / Stop button). */
    val playing: StateFlow<Boolean> = isPlaying.asStateFlow()

    /** Plays [tone] once, start to end, at [volume] (0..100). Stops any tone already playing first. */
    fun play(tone: Tone, volume: Int) {
        stop()
        if (audioManager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        val gain = toneGain(volume)
        // Not built in `apply { }`: there, `stop()` would be MediaPlayer.stop(), which keeps the
        // player and the audio focus.
        val next = MediaPlayer()
        next.setAudioAttributes(attributes)
        resources.openRawResourceFd(tone.file).use { next.setDataSource(it) }
        next.setVolume(gain, gain)
        next.setOnPreparedListener { it.start() }
        next.setOnCompletionListener { if (it === player) stop() }
        next.setOnErrorListener { mp, _, _ ->
            if (mp === player) stop()
            true
        }
        next.prepareAsync() // Off the main thread; it starts when ready.
        player = next
        isPlaying.value = true
    }

    /** Changes the loudness of a playing tone at once (the volume slider). */
    fun setVolume(volume: Int) {
        val gain = toneGain(volume)
        player?.setVolume(gain, gain)
    }

    fun stop() {
        val current = player ?: return
        player = null
        current.release()
        audioManager.abandonAudioFocusRequest(focus)
        isPlaying.value = false
    }
}

private val Tone.file: Int
    get() = when (this) {
        Tone.MARIMBA -> R.raw.marimba
        Tone.CHIME -> R.raw.soft_chime
        Tone.STEEL_DRUM -> R.raw.steel_drum
    }
