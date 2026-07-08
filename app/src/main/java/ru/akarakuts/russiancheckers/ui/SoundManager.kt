package ru.akarakuts.russiancheckers.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import ru.akarakuts.russiancheckers.R

/** Game sound effect ids for [SoundManager.play]. */
enum class GameSound { Move, Capture, Crown, Win, Lose }

/** SoundPool wrapper for short board sounds; create in composition, release in onDispose. */
class SoundManager(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<GameSound, Int> = mapOf(
        GameSound.Move to pool.load(context, R.raw.snd_move, 1),
        GameSound.Capture to pool.load(context, R.raw.snd_capture, 1),
        GameSound.Crown to pool.load(context, R.raw.snd_crown, 1),
        GameSound.Win to pool.load(context, R.raw.snd_win, 1),
        GameSound.Lose to pool.load(context, R.raw.snd_lose, 1),
    )

    fun play(sound: GameSound) {
        val id = ids[sound] ?: return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
