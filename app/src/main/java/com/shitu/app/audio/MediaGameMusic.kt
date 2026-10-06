package com.shitu.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

/**
 * 第十七阶段：基于 MediaPlayer + SoundPool 的音乐 / 音效实现。
 *
 * 设计要点：
 * - 两个 MediaPlayer 槽位做交叉淡入淡出，切场景不做硬切；
 * - 资源按 ogg → mp3 → m4a → wav 的顺序查找，替换正式音乐无需改代码；
 * - 资源缺失或解码失败只记录日志，绝不抛异常影响游戏；
 * - 进入后台降音后暂停，回到前台平滑恢复。
 */
class MediaGameMusic(context: Context) : GameMusic {

    private val appContext = context.applicationContext
    private val assets = appContext.assets
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "shitu-audio").apply { isDaemon = true }
    }

    /** 资源基名 → 实际路径（含扩展名）。 */
    private val assetIndex = HashMap<String, String>()

    private val players = arrayOfNulls<MediaPlayer>(2)
    private val slotVolume = FloatArray(2)
    private val ramps = ArrayList<Ramp>()
    private var activeSlot = 0

    /** 界面希望播放的场景（可能因为静音或资源缺失没有真正播放）。 */
    private var desiredScene: MusicScene? = null

    private var musicEnabled = true
    private var musicVolume = AudioSettings.DEFAULT_MUSIC_VOLUME
    private var sfxVolume = AudioSettings.DEFAULT_SFX_VOLUME
    private var foreground = true
    private var released = false
    private var requestToken = 0

    private val pool: SoundPool? = try {
        SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(ATTRIBUTES)
            .build()
    } catch (error: Exception) {
        Log.w(TAG, "无法创建 SoundPool，将退回系统提示音", error)
        null
    }

    private val soundIds = HashMap<GameSound, Int>()
    private var tone: ToneGenerator? = null

    private class Ramp(
        val slot: Int,
        val from: Float,
        val to: Float,
        val startedAt: Long,
        val durationMs: Int,
        val releaseAtEnd: Boolean
    )

    private val ticker = object : Runnable {
        override fun run() = pump()
    }

    init {
        buildAssetIndex()
        io.execute { loadSounds() }
    }

    // ===== 背景音乐 =====

    override fun playBgm(scene: MusicScene) = crossFadeTo(scene)

    override fun crossFadeTo(scene: MusicScene, durationMs: Int) {
        if (released) return
        desiredScene = scene
        if (!musicEnabled) {
            stopBgm()
            desiredScene = scene
            return
        }
        val duration = durationMs.coerceIn(0, MAX_FADE_MS)
        val playing = players[activeSlot]
        if (scene === currentScene() && playing != null && playing.isPlaying) {
            rampTo(activeSlot, slotVolume[activeSlot], targetVolume(scene), NOTICE_FADE_MS, false)
            return
        }
        val old = activeSlot
        val next = 1 - old
        activeSlot = next
        // 旧曲淡出后释放；新曲从 0 淡入。
        if (players[old] != null) rampTo(old, slotVolume[old], 0f, duration, true)
        playSlot(next, scene, duration)
    }

    private fun currentScene(): MusicScene? = slotScene[activeSlot]

    private val slotScene = arrayOfNulls<MusicScene>(2)

    override fun stopBgm() {
        if (released) return
        desiredScene = null
        for (slot in players.indices) {
            if (players[slot] != null) rampTo(slot, slotVolume[slot], 0f, STOP_FADE_MS, true)
        }
    }

    override fun setMusicEnabled(enabled: Boolean) {
        if (released || musicEnabled == enabled) return
        musicEnabled = enabled
        if (!enabled) {
            val keep = desiredScene
            stopBgm()
            desiredScene = keep
            return
        }
        desiredScene?.let { crossFadeTo(it, STOP_FADE_MS) }
    }

    override fun setMusicVolume(volume: Float) {
        if (released) return
        val value = AudioSettings.clampVolume(volume)
        if (value == musicVolume) return
        musicVolume = value
        val scene = currentScene() ?: return
        if (!musicEnabled) return
        rampTo(activeSlot, slotVolume[activeSlot], targetVolume(scene), VOLUME_FADE_MS, false)
    }

    override fun pauseMusic() {
        if (released || !foreground) return
        foreground = false
        for (slot in players.indices) {
            if (players[slot] != null) rampTo(slot, slotVolume[slot], 0f, AudioSettings.PAUSE_FADE_MS, false)
        }
        handler.postDelayed(pauseTask, (AudioSettings.PAUSE_FADE_MS + 80).toLong())
    }

    private val pauseTask = Runnable {
        if (!foreground) {
            for (player in players) {
                try {
                    if (player?.isPlaying == true) player.pause()
                } catch (error: Exception) {
                    Log.w(TAG, "暂停音乐失败", error)
                }
            }
        }
    }

    override fun resumeMusic() {
        if (released || foreground) return
        foreground = true
        handler.removeCallbacks(pauseTask)
        for (player in players) {
            try {
                player?.start()
            } catch (error: Exception) {
                Log.w(TAG, "恢复音乐失败", error)
            }
        }
        val scene = currentScene() ?: return
        if (musicEnabled) {
            slotVolume[activeSlot] = 0f
            rampTo(activeSlot, 0f, targetVolume(scene), RESUME_FADE_MS, false)
        }
    }

    private fun targetVolume(scene: MusicScene): Float =
        (scene.gain * musicVolume).coerceIn(0f, 1f)

    private fun playSlot(slot: Int, scene: MusicScene, fadeMs: Int) {
        val token = ++requestToken
        releaseSlot(slot, dropRamps = true)
        slotScene[slot] = scene
        slotVolume[slot] = 0f
        val path = resolve(scene.assetBase)
        if (path == null) {
            Log.w(TAG, "缺少音乐资源：${scene.assetBase}（支持 ogg/mp3/m4a/wav）")
            return
        }
        val player = try {
            MediaPlayer()
        } catch (error: Exception) {
            Log.w(TAG, "创建播放器失败", error)
            return
        }
        io.execute {
            val prepared = try {
                openSource(player, path)
                player.isLooping = scene.loop
                player.setAudioAttributes(ATTRIBUTES)
                player.setVolume(0f, 0f)
                player.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "音乐播放出错：$what/$extra（${scene.assetBase}）")
                    true
                }
                player.prepare()
                true
            } catch (error: Exception) {
                Log.w(TAG, "准备音乐失败：$path", error)
                false
            }
            handler.post {
                if (released || token != requestToken) {
                    safeRelease(player)
                    return@post
                }
                if (!prepared) {
                    safeRelease(player)
                    return@post
                }
                players[slot] = player
                try {
                    player.start()
                } catch (error: Exception) {
                    Log.w(TAG, "开始播放失败", error)
                }
                if (!foreground) {
                    try {
                        player.pause()
                    } catch (error: Exception) {
                        Log.w(TAG, "后台起播后暂停失败", error)
                    }
                }
                rampTo(slot, 0f, targetVolume(scene), fadeMs, false)
            }
        }
    }

    private fun releaseSlot(slot: Int, dropRamps: Boolean) {
        if (dropRamps) ramps.removeAll { it.slot == slot }
        val player = players[slot] ?: return
        players[slot] = null
        slotScene[slot] = null
        slotVolume[slot] = 0f
        safeRelease(player)
    }

    private fun safeRelease(player: MediaPlayer) {
        try {
            player.reset()
        } catch (_: Exception) {
        }
        try {
            player.release()
        } catch (error: Exception) {
            Log.w(TAG, "释放播放器失败", error)
        }
    }

    // ===== 音量渐变 =====

    private fun rampTo(slot: Int, from: Float, to: Float, durationMs: Int, releaseAtEnd: Boolean) {
        ramps.removeAll { it.slot == slot }
        val fromValue = from.coerceIn(0f, 1f)
        val toValue = to.coerceIn(0f, 1f)
        if (durationMs <= 0 || fromValue == toValue) {
            slotVolume[slot] = toValue
            applyVolume(slot)
            if (releaseAtEnd) releaseSlot(slot, dropRamps = false)
            if (ramps.isNotEmpty()) {
                handler.removeCallbacks(ticker)
                handler.post(ticker)
            }
            return
        }
        ramps.add(Ramp(slot, fromValue, toValue, SystemClock.uptimeMillis(), durationMs, releaseAtEnd))
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun pump() {
        val now = SystemClock.uptimeMillis()
        var pending = false
        val iterator = ramps.iterator()
        while (iterator.hasNext()) {
            val ramp = iterator.next()
            val progress =
                ((now - ramp.startedAt).toFloat() / ramp.durationMs).coerceIn(0f, 1f)
            val value = ramp.from + (ramp.to - ramp.from) * progress
            slotVolume[ramp.slot] = value
            applyVolume(ramp.slot)
            if (progress >= 1f) {
                if (ramp.releaseAtEnd) releaseSlot(ramp.slot, dropRamps = false)
                iterator.remove()
            } else {
                pending = true
            }
        }
        handler.removeCallbacks(ticker)
        if (pending) handler.postDelayed(ticker, STEP_MS)
    }

    private fun applyVolume(slot: Int) {
        val player = players[slot] ?: return
        val value = if (foreground) slotVolume[slot] else 0f
        try {
            player.setVolume(value, value)
        } catch (error: Exception) {
            Log.w(TAG, "调整音量失败", error)
        }
    }

    // ===== 音效 =====

    override fun playSfx(sound: GameSound) {
        if (released || sfxVolume <= 0f || !foreground) return
        val id = soundIds[sound]
        if (pool != null && id != null && id != 0) {
            val value = (sfxVolume * SFX_GAIN).coerceIn(0f, 1f)
            try {
                pool.play(id, value, value, 1, 0, 1f)
                return
            } catch (error: Exception) {
                Log.w(TAG, "播放音效失败：${sound.name}", error)
            }
        }
        playToneFallback(sound)
    }

    override fun setSfxVolume(volume: Float) {
        sfxVolume = AudioSettings.clampVolume(volume)
    }

    private fun loadSounds() {
        val loaded = HashMap<GameSound, Int>()
        for ((sound, base) in AudioAssets.SFX_BASES) {
            val path = resolve(base) ?: continue
            val id = loadSound(path)
            if (id != 0) loaded[sound] = id
        }
        handler.post {
            if (released) {
                loaded.values.forEach { runCatching { pool?.unload(it) } }
                return@post
            }
            soundIds.putAll(loaded)
        }
    }

    private fun loadSound(path: String): Int {
        val soundPool = pool ?: return 0
        val fromAsset = try {
            assets.openFd(path).use { descriptor -> soundPool.load(descriptor, 1) }
        } catch (_: Exception) {
            0
        }
        if (fromAsset != 0) return fromAsset
        val file = cacheAsset(path) ?: return 0
        return try {
            soundPool.load(file.absolutePath, 1)
        } catch (error: Exception) {
            Log.w(TAG, "加载音效失败：$path", error)
            0
        }
    }

    private fun playToneFallback(sound: GameSound) {
        try {
            val generator = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME).also { tone = it }
            val (type, duration) = toneOf(sound)
            generator.startTone(type, duration)
        } catch (error: Exception) {
            Log.w(TAG, "系统提示音也不可用：${sound.name}", error)
        }
    }

    private fun toneOf(sound: GameSound): Pair<Int, Int> = when (sound) {
        GameSound.CLICK, GameSound.TOGGLE -> ToneGenerator.TONE_PROP_BEEP to 40
        GameSound.PAGE, GameSound.BACK, GameSound.CLOSE -> ToneGenerator.TONE_PROP_BEEP2 to 50
        GameSound.CONFIRM, GameSound.SUCCESS, GameSound.ACHIEVEMENT -> ToneGenerator.TONE_PROP_ACK to 120
        GameSound.EVENT_MAJOR -> ToneGenerator.TONE_PROP_BEEP2 to 90
        GameSound.WARNING, GameSound.FAILURE -> ToneGenerator.TONE_PROP_NACK to 150
        GameSound.PROMOTION -> ToneGenerator.TONE_PROP_ACK to 160
        GameSound.ENDING -> ToneGenerator.TONE_PROP_BEEP2 to 200
    }

    // ===== 资源 =====

    private fun buildAssetIndex() {
        for (dir in AudioAssets.ASSET_DIRS) {
            val names = try {
                assets.list(dir)
            } catch (error: Exception) {
                Log.w(TAG, "读取音频目录失败：$dir", error)
                null
            } ?: continue
            for (name in names) {
                val dot = name.lastIndexOf('.')
                if (dot <= 0) continue
                val ext = name.substring(dot + 1).lowercase()
                if (ext !in AudioAssets.AUDIO_EXTS) continue
                val base = "$dir/" + name.substring(0, dot)
                val current = assetIndex[base]
                if (current == null || AudioAssets.rankOf(ext) < AudioAssets.rankOf(current.substringAfterLast('.'))) {
                    assetIndex[base] = "$dir/$name"
                }
            }
        }
    }

    private fun resolve(base: String): String? = assetIndex[base]

    private fun openSource(player: MediaPlayer, path: String) {
        val descriptor = try {
            assets.openFd(path)
        } catch (_: Exception) {
            null
        }
        if (descriptor != null) {
            descriptor.use {
                player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            return
        }
        val file = cacheAsset(path) ?: throw IllegalStateException("资源不可用：$path")
        player.setDataSource(file.absolutePath)
    }

    /** 资源被压缩时 openFd 不可用，退回到缓存目录中的副本。 */
    private fun cacheAsset(path: String): File? = try {
        val target = File(appContext.cacheDir, "audio/$path")
        if (!target.exists() || target.length() <= 0L) {
            target.parentFile?.mkdirs()
            assets.open(path).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        target
    } catch (error: Exception) {
        Log.w(TAG, "复制音频资源失败：$path", error)
        null
    }

    override fun release() {
        if (released) return
        released = true
        handler.removeCallbacksAndMessages(null)
        ramps.clear()
        for (slot in players.indices) releaseSlot(slot, dropRamps = false)
        try {
            pool?.release()
        } catch (error: Exception) {
            Log.w(TAG, "释放 SoundPool 失败", error)
        }
        try {
            tone?.release()
        } catch (error: Exception) {
            Log.w(TAG, "释放提示音通道失败", error)
        }
        tone = null
        io.shutdownNow()
    }

    private companion object {
        const val TAG = "GameMusic"
        const val MAX_STREAMS = 4
        const val STEP_MS = 40L
        const val MAX_FADE_MS = 3000
        const val NOTICE_FADE_MS = 400
        const val STOP_FADE_MS = 800
        const val VOLUME_FADE_MS = 300
        const val RESUME_FADE_MS = 700
        const val SFX_GAIN = 0.85f
        const val TONE_VOLUME = 40

        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    }
}