package com.shitu.app

import com.shitu.app.audio.AudioAssets
import com.shitu.app.audio.AudioSettings
import com.shitu.app.audio.GameSound
import com.shitu.app.audio.MediaGameMusic
import com.shitu.app.audio.MusicCue
import com.shitu.app.audio.MusicScene
import com.shitu.app.audio.MusicSceneResolver
import com.shitu.app.game.GamePhase
import com.shitu.app.model.EventTier
import com.shitu.app.ui.PlayTab
import com.shitu.app.ui.RootScreen
import com.shitu.app.ui.musicCueOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File

/**
 * 第十七阶段：音频系统测试。
 *
 * 覆盖场景映射、音量边界、资源清单与占位音频文件本身，
 * 保证「代码—资源—设置」三者不会脱节。
 */
class AudioSystemTest {

    // ===== 场景映射 =====

    @Test
    fun `菜单线索映射到菜单音乐`() {
        assertEquals(MusicScene.MENU, MusicSceneResolver.sceneFor(MusicCue.MENU))
    }

    @Test
    fun `首页线索映射到首页音乐`() {
        assertEquals(MusicScene.HOME, MusicSceneResolver.sceneFor(MusicCue.HOME))
    }

    @Test
    fun `政务线索映射到政务音乐`() {
        assertEquals(MusicScene.GOVERNANCE, MusicSceneResolver.sceneFor(MusicCue.GOVERNANCE))
    }

    @Test
    fun `普通事件使用平静的事件音乐`() {
        assertEquals(MusicScene.EVENT, MusicSceneResolver.sceneFor(MusicCue.EVENT))
    }

    @Test
    fun `重大事件使用更厚重大事件音乐`() {
        assertEquals(MusicScene.MAJOR, MusicSceneResolver.sceneFor(MusicCue.EVENT_MAJOR))
    }

    @Test
    fun `危机线索映射到危机音乐`() {
        assertEquals(MusicScene.CRISIS, MusicSceneResolver.sceneFor(MusicCue.EVENT_CRISIS))
    }

    @Test
    fun `政治线索映射到政治音乐`() {
        assertEquals(MusicScene.POLITICS, MusicSceneResolver.sceneFor(MusicCue.POLITICS))
    }

    @Test
    fun `晋升线索映射到晋升音乐`() {
        assertEquals(MusicScene.PROMOTION, MusicSceneResolver.sceneFor(MusicCue.PROMOTION))
    }

    @Test
    fun `结局线索映射到结局音乐`() {
        assertEquals(MusicScene.ENDING, MusicSceneResolver.sceneFor(MusicCue.ENDING))
    }

    // ===== 界面状态 → 线索 =====

    @Test
    fun `非游戏根页面一律使用菜单音乐`() {
        for (root in listOf(RootScreen.HOME, RootScreen.CREATE, RootScreen.ARCHIVE, RootScreen.SETTINGS)) {
            assertEquals(
                MusicCue.MENU,
                musicCueOf(root, PlayTab.HOME, null, null, false, null)
            )
        }
    }

    @Test
    fun `结局优先于任何页面`() {
        assertEquals(
            MusicCue.ENDING,
            musicCueOf(RootScreen.PLAY, PlayTab.EVENT, null, GamePhase.EVENT, true, EventTier.CRISIS)
        )
    }

    @Test
    fun `危机事件切到危机线索`() {
        assertEquals(
            MusicCue.EVENT_CRISIS,
            musicCueOf(RootScreen.PLAY, PlayTab.EVENT, null, GamePhase.EVENT, false, EventTier.CRISIS)
        )
    }

    @Test
    fun `重大事件仍然克制`() {
        assertEquals(
            MusicCue.EVENT_MAJOR,
            musicCueOf(RootScreen.PLAY, PlayTab.EVENT, null, GamePhase.EVENT, false, EventTier.MAJOR)
        )
    }

    @Test
    fun `事件阶段优先于标签页`() {
        assertEquals(
            MusicCue.EVENT,
            musicCueOf(RootScreen.PLAY, PlayTab.HOME, null, GamePhase.EVENT, false, EventTier.NORMAL)
        )
    }

    @Test
    fun `政治与转任二级页面使用政治线索`() {
        for (tab in listOf(PlayTab.POLITICS, PlayTab.TRANSFER, PlayTab.RELATION)) {
            assertEquals(
                MusicCue.POLITICS,
                musicCueOf(RootScreen.PLAY, PlayTab.MINE, tab, null, false, null)
            )
        }
    }

    @Test
    fun `其它二级页面使用政务线索`() {
        for (tab in listOf(PlayTab.PROJECT, PlayTab.REGION, PlayTab.WORLD, PlayTab.SETTINGS)) {
            assertEquals(
                MusicCue.GOVERNANCE,
                musicCueOf(RootScreen.PLAY, PlayTab.MINE, tab, null, false, null)
            )
        }
    }

    @Test
    fun `年度考核使用平静的首页线索`() {
        assertEquals(
            MusicCue.HOME,
            musicCueOf(RootScreen.PLAY, PlayTab.HOME, null, GamePhase.REVIEW, false, null)
        )
    }

    @Test
    fun `我的与生涯页面保持首页线索`() {
        assertEquals(
            MusicCue.HOME,
            musicCueOf(RootScreen.PLAY, PlayTab.MINE, null, null, false, null)
        )
        assertEquals(
            MusicCue.HOME,
            musicCueOf(RootScreen.PLAY, PlayTab.CAREER, null, null, false, null)
        )
    }

    // ===== 音量与参数 =====

    @Test
    fun `音量夹取到合法区间`() {
        assertEquals(0f, AudioSettings.clampVolume(-3f), 0.0001f)
        assertEquals(0f, AudioSettings.clampVolume(Float.NaN), 0.0001f)
        assertEquals(1f, AudioSettings.clampVolume(9f), 0.0001f)
        assertEquals(0.42f, AudioSettings.clampVolume(0.42f), 0.0001f)
    }

    @Test
    fun `非法存档音量回落到默认值`() {
        assertEquals(
            AudioSettings.DEFAULT_MUSIC_VOLUME,
            AudioSettings.sanitizeVolume(Float.NaN, AudioSettings.DEFAULT_MUSIC_VOLUME),
            0.0001f
        )
        assertEquals(
            AudioSettings.DEFAULT_MUSIC_VOLUME,
            AudioSettings.sanitizeVolume(1.7f, AudioSettings.DEFAULT_MUSIC_VOLUME),
            0.0001f
        )
        assertEquals(
            AudioSettings.DEFAULT_SFX_VOLUME,
            AudioSettings.sanitizeVolume(-0.4f, AudioSettings.DEFAULT_SFX_VOLUME),
            0.0001f
        )
    }

    @Test
    fun `默认音量落在推荐听感区间`() {
        assertTrue(AudioSettings.DEFAULT_MUSIC_VOLUME in 0.25f..0.35f)
        assertTrue(AudioSettings.DEFAULT_SFX_VOLUME in 0.40f..0.50f)
    }

    @Test
    fun `交叉淡入淡出时长在建议区间`() {
        assertTrue(AudioSettings.CROSS_FADE_MS in 800..1500)
        assertTrue(AudioSettings.PAUSE_FADE_MS in 200..800)
    }

    @Test
    fun `每首曲子资源路径唯一且位于 bgm 目录`() {
        val paths = MusicScene.entries.map { it.assetBase }
        assertEquals(paths.size, paths.toSet().size)
        for (path in paths) {
            assertTrue(path, path.startsWith("audio/bgm/"))
        }
    }

    @Test
    fun `只有晋升曲是一次性的`() {
        for (scene in MusicScene.entries) {
            assertEquals(scene.name, scene != MusicScene.PROMOTION, scene.loop)
        }
    }

    // ===== 资源清单 =====

    @Test
    fun `所有音效都有资源基名`() {
        assertEquals(GameSound.entries.size, AudioAssets.SFX_BASES.size)
        for (sound in GameSound.entries) {
            val base = AudioAssets.SFX_BASES[sound]
            assertNotNull(sound.name, base)
            assertTrue(sound.name, base!!.startsWith("audio/"))
        }
    }

    @Test
    fun `扩展名优先级为 ogg 高于 wav`() {
        assertTrue(
            AudioAssets.rankOf("ogg") < AudioAssets.rankOf("wav")
        )
        assertTrue(
            AudioAssets.rankOf("mp3") < AudioAssets.rankOf("wav")
        )
        assertEquals(
            AudioAssets.AUDIO_EXTS.size,
            AudioAssets.rankOf("flac")
        )
    }

    // ===== 占位音频文件 =====

    @Test
    fun `每首背景音乐都有可用的占位文件`() {
        for (scene in MusicScene.entries) {
            val file = assetFile(scene.assetBase)
            assertNotNull("缺少音乐资源：${scene.assetBase}", file)
            assertTrue("资源为空：${scene.assetBase}", file!!.length() > 1024)
        }
    }

    @Test
    fun `每个音效都有可用的占位文件`() {
        for ((sound, base) in AudioAssets.SFX_BASES) {
            val file = assetFile(base)
            assertNotNull("缺少音效资源：$base（${sound.name}）", file)
            assertTrue("资源为空：$base", file!!.length() > 256)
        }
    }

    @Test
    fun `背景音乐是合法的单声道 16 位 WAV`() {
        for (scene in MusicScene.entries) {
            val info = wavInfo(assetFile(scene.assetBase)!!)
            assertEquals(scene.name, 1, info.channels)
            assertEquals(scene.name, 16, info.bits)
            assertEquals(scene.name, 22050, info.sampleRate)
            assertTrue(scene.name, info.durationSeconds > 8.0)
            assertTrue(scene.name, info.durationSeconds < 60.0)
        }
    }

    @Test
    fun `占位音乐电平不为零且不削波`() {
        for (scene in MusicScene.entries) {
            val samples = readSamples(assetFile(scene.assetBase)!!)
            var peak = 0
            for (sample in samples) {
                val value = kotlin.math.abs(sample.toInt())
                if (value > peak) peak = value
            }
            assertTrue("${scene.name} 峰值过低", peak > 2000)
            assertTrue("${scene.name} 削波", peak <= 32767)
        }
    }

    @Test
    fun `循环音乐首尾做了淡入淡出不存在爆音`() {
        for (scene in MusicScene.entries.filter { it.loop }) {
            val samples = readSamples(assetFile(scene.assetBase)!!)
            assertTrue(scene.name, kotlin.math.abs(samples.first().toInt()) < 400)
            assertTrue(scene.name, kotlin.math.abs(samples.last().toInt()) < 400)
        }
    }

    @Test
    fun `音效短小干净`() {
        for ((sound, base) in AudioAssets.SFX_BASES) {
            val info = wavInfo(assetFile(base)!!)
            assertTrue(sound.name, info.durationSeconds < 3.0)
            assertTrue(sound.name, info.durationSeconds > 0.03)
        }
    }

    @Test
    fun `失效的音频管理器不会崩溃`() {
        val music = com.shitu.app.audio.GameMusic.NONE
        music.playBgm(MusicScene.HOME)
        music.crossFadeTo(MusicScene.ENDING, 900)
        music.playSfx(GameSound.CLICK)
        music.setMusicEnabled(false)
        music.setMusicVolume(0.5f)
        music.setSfxVolume(0.5f)
        music.pauseMusic()
        music.resumeMusic()
        music.stopBgm()
        music.release()
    }

    @Test
    fun `引擎对外接口保持稳定`() {
        // 编译期校验：实现类仍然提供全部能力。
        val clazz = MediaGameMusic::class.java
        for (name in listOf(
            "playBgm", "stopBgm", "crossFadeTo", "playSfx",
            "setMusicEnabled", "setMusicVolume", "setSfxVolume",
            "pauseMusic", "resumeMusic", "release", "play"
        )) {
            assertNotNull(name, clazz.methods.firstOrNull { it.name == name })
        }
    }

    // ===== 工具 =====

    private class WavInfo(val channels: Int, val sampleRate: Int, val bits: Int, val durationSeconds: Double)

    private fun assetFile(base: String): File? {
        val root = assetsRoot() ?: return null
        for (ext in AudioAssets.AUDIO_EXTS) {
            val file = File(root, "$base.$ext")
            if (file.exists() && file.length() > 0) return file
        }
        return null
    }

    private fun assetsRoot(): File? {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File("../app/src/main/assets")
        )
        return candidates.firstOrNull { it.isDirectory }
    }

    private fun wavInfo(file: File): WavInfo {
        DataInputStream(file.inputStream().buffered()).use { input ->
            val header = ByteArray(44)
            input.readFully(header)
            require(String(header, 0, 4) == "RIFF") { "不是 RIFF 文件：${file.name}" }
            require(String(header, 8, 4) == "WAVE") { "不是 WAVE 文件：${file.name}" }
            val channels = readShortLe(header, 22)
            val sampleRate = readIntLe(header, 24)
            val bits = readShortLe(header, 34)
            val dataSize = readIntLe(header, 40)
            val frameSize = channels * bits / 8
            val duration = dataSize.toDouble() / (sampleRate * frameSize)
            return WavInfo(channels, sampleRate, bits, duration)
        }
    }

    private fun readSamples(file: File): ShortArray {
        DataInputStream(file.inputStream().buffered()).use { input ->
            input.skipBytes(44)
            val bytes = ByteArray(file.length().toInt() - 44)
            input.readFully(bytes)
            val samples = ShortArray(bytes.size / 2)
            for (i in samples.indices) {
                val low = bytes[i * 2].toInt() and 0xFF
                val high = bytes[i * 2 + 1].toInt()
                samples[i] = ((high shl 8) or low).toShort()
            }
            return samples
        }
    }

    private fun readShortLe(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun readIntLe(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}