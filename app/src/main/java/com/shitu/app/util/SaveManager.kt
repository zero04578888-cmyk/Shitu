package com.shitu.app.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.shitu.app.audio.AudioSettings
import com.shitu.app.game.ArchiveFactory
import com.shitu.app.game.GameSave
import com.shitu.app.game.GameState
import com.shitu.app.model.LifeArchive

// 存档读取结果。任何异常都会变成 [Corrupt]，不会让应用崩溃。
sealed class LoadResult {
    /**
     * 读取成功。
     * [recovered] 为 true 表示主存档损坏或缺失，本次是从备份恢复的。
     */
    data class Success(val save: GameSave, val recovered: Boolean = false) : LoadResult() {
        /** 便捷访问：真实游戏状态（进度始终以它为准）。 */
        val state: GameState get() = save.state
    }
    data object Empty : LoadResult()
    data object Corrupt : LoadResult()
}

/**
 * 存档管理：自动存档 + 手动存档 + 设置项。
 *
 * 第十四阶段加固：
 * - 每次覆盖写入前先把旧内容留作备份；
 * - 为每份存档写入轻量校验和；
 * - 主存档损坏时自动回退到最近一次备份，并把它恢复成主存档。
 *
 * 旧版本存档没有校验和与备份，会被当作兼容数据正常读取。
 */
class SaveManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private data class Slot(
        val state: String,
        val time: String,
        val backup: String,
        val backupChk: String,
        val chk: String
    )

    private val autoSlot = Slot(KEY_AUTO, KEY_AUTO_TIME, KEY_AUTO_BACKUP, KEY_AUTO_BACKUP_CHK, KEY_AUTO_CHK)
    private val manualSlot =
        Slot(KEY_MANUAL, KEY_MANUAL_TIME, KEY_MANUAL_BACKUP, KEY_MANUAL_BACKUP_CHK, KEY_MANUAL_CHK)

    // ===== 写入 =====

    /** 自动存档。返回 true 表示写入成功；失败时记录日志并返回 false，不抛出异常。 */
    fun saveAuto(state: GameState): Boolean = write(autoSlot, state)

    fun saveManual(state: GameState): Boolean = write(manualSlot, state)

    private fun write(slot: Slot, state: GameState): Boolean = try {
        val now = System.currentTimeMillis()
        val save = GameSave(
            version = SaveCodec.ENVELOPE_VERSION,
            startedAt = startedAtForWrite(now),
            savedAt = now,
            state = state
        )
        val text = SaveCodec.encode(save)
        val previous = prefs.getString(slot.state, null)
        val previousChk = prefs.getString(slot.chk, null)
        val editor = prefs.edit()
        // 覆盖之前先保存旧版本，作为损坏时的回退点。
        if (previous != null) {
            editor.putString(slot.backup, previous)
            editor.putString(slot.backupChk, previousChk ?: SaveIntegrity.checksum(previous))
        }
        editor.putString(slot.state, text)
        editor.putString(slot.chk, SaveIntegrity.checksum(text))
        editor.putLong(slot.time, now)
        // 使用 commit 同步落盘：进程被杀死时数据也不会丢失。
        editor.commit()
    } catch (error: Exception) {
        Log.e(TAG, "写入存档失败", error)
        false
    }

    // ===== 读取 =====

    fun loadAuto(): LoadResult = read(autoSlot)

    fun loadManual(): LoadResult = read(manualSlot)

    // 继续游戏优先使用自动存档，其次手动存档。
    fun loadLatest(): LoadResult {
        val auto = read(autoSlot)
        if (auto is LoadResult.Success) return auto
        val manual = read(manualSlot)
        if (manual is LoadResult.Success) return manual
        if (auto is LoadResult.Corrupt || manual is LoadResult.Corrupt) return LoadResult.Corrupt
        return LoadResult.Empty
    }

    private fun read(slot: Slot): LoadResult {
        val primary = readRaw(slot.state)
        val backup = readRaw(slot.backup)
        // 先按校验和决定候选顺序；校验通过但内容无法解析时，再退一步尝试另一份。
        val decision = SaveIntegrity.decide(
            primary, readRaw(slot.chk), backup, readRaw(slot.backupChk)
        )
        val ordered = when (decision) {
            SaveIntegrity.Decision.PRIMARY -> listOf(primary to false, backup to true)
            SaveIntegrity.Decision.BACKUP -> listOf(backup to true, primary to false)
            SaveIntegrity.Decision.CORRUPT -> listOf(primary to false, backup to true)
            SaveIntegrity.Decision.NONE -> emptyList()
        }
        ordered.forEach { (text, fromBackup) ->
            if (text == null) return@forEach
            SaveCodec.decode(text)?.let { save ->
                if (fromBackup) restore(slot, text)
                rememberRunStart(save.startedAt)
                return LoadResult.Success(save, recovered = fromBackup)
            }
        }
        return if (decision == SaveIntegrity.Decision.NONE) LoadResult.Empty else LoadResult.Corrupt
    }

    /** 把备份内容写回主存档位，让玩家之后能正常继续。 */
    private fun restore(slot: Slot, text: String) {
        val editor = prefs.edit()
        editor.putString(slot.state, text)
        editor.putString(slot.chk, SaveIntegrity.checksum(text))
        editor.putLong(slot.time, System.currentTimeMillis())
        editor.commit()
    }

    // ===== 本局元信息（第十五阶段） =====

    /** 本局开始时间；尚未记录时写入当前时间。 */
    private fun startedAtForWrite(now: Long): Long {
        val existing = prefs.getLong(KEY_RUN_STARTED, 0L)
        if (existing > 0L) return existing
        prefs.edit { putLong(KEY_RUN_STARTED, now) }
        return now
    }

    /** 读到带开局时间的存档时，把它同步为当前这一局的开始时间。 */
    private fun rememberRunStart(startedAt: Long) {
        if (startedAt <= 0L) return
        if (prefs.getLong(KEY_RUN_STARTED, 0L) <= 0L) {
            prefs.edit { putLong(KEY_RUN_STARTED, startedAt) }
        }
    }

    /** 开始新的一局：重置本局开始时间，下一次写入会记录新的开局时间。 */
    fun beginRun() {
        prefs.edit { putLong(KEY_RUN_STARTED, System.currentTimeMillis()) }
    }

    /** 本局开始时间（毫秒）。0 表示未知。 */
    fun runStartedAt(): Long = prefs.getLong(KEY_RUN_STARTED, 0L)

    /** 最近一次存档的完整信封（含版本、开局时间与最后保存时间）。 */
    fun loadLatestSave(): GameSave? = (loadLatest() as? LoadResult.Success)?.save

    private fun readRaw(key: String): String? = try {
        prefs.getString(key, null)
    } catch (error: Exception) {
        null
    }

    // ===== 状态查询（设置页使用） =====

    fun hasAutoSave(): Boolean = prefs.contains(KEY_AUTO) || prefs.contains(KEY_AUTO_BACKUP)

    fun hasManualSave(): Boolean = prefs.contains(KEY_MANUAL) || prefs.contains(KEY_MANUAL_BACKUP)

    fun hasAnySave(): Boolean = hasAutoSave() || hasManualSave()

    fun backupAvailable(): Boolean =
        prefs.contains(KEY_AUTO_BACKUP) || prefs.contains(KEY_MANUAL_BACKUP)

    fun savedAt(): Long =
        maxOf(prefs.getLong(KEY_AUTO_TIME, 0L), prefs.getLong(KEY_MANUAL_TIME, 0L))

    /** 当前主存档记录的格式版本；没有存档返回 0。 */
    fun storedVersion(): Int {
        val auto = readRaw(KEY_AUTO)
        if (auto != null) return SaveIntegrity.versionOf(auto)
        val manual = readRaw(KEY_MANUAL)
        return if (manual != null) SaveIntegrity.versionOf(manual) else 0
    }

    /** 存档文本大致占用（字符数），用于设置页展示，不代表磁盘精确占用。 */
    fun storedSizeChars(): Int {
        var total = 0
        listOf(KEY_AUTO, KEY_MANUAL, KEY_ARCHIVE).forEach { key ->
            total += readRaw(key)?.length ?: 0
        }
        return total
    }

    fun clearAll() {
        prefs.edit {
            remove(KEY_RUN_STARTED)
            remove(KEY_AUTO)
            remove(KEY_MANUAL)
            remove(KEY_AUTO_TIME)
            remove(KEY_MANUAL_TIME)
            remove(KEY_AUTO_CHK)
            remove(KEY_MANUAL_CHK)
            remove(KEY_AUTO_BACKUP)
            remove(KEY_MANUAL_BACKUP)
            remove(KEY_AUTO_BACKUP_CHK)
            remove(KEY_MANUAL_BACKUP_CHK)
        }
    }

    // ===== 收集档案：结局图鉴与「最近的人生」=====
    // 与存档分离，删除存档不会清空图鉴。

    fun loadArchive(): LifeArchive {
        val text = readRaw(KEY_ARCHIVE) ?: return LifeArchive.EMPTY
        if (!SaveIntegrity.verify(text, readRaw(KEY_ARCHIVE_CHK))) {
            val backup = readRaw(KEY_ARCHIVE_BACKUP)
            if (backup != null && SaveIntegrity.verify(backup, readRaw(KEY_ARCHIVE_BACKUP_CHK))) {
                return ArchiveCodec.decode(backup) ?: LifeArchive.EMPTY
            }
            return LifeArchive.EMPTY
        }
        return ArchiveCodec.decode(text) ?: LifeArchive.EMPTY
    }

    private fun writeArchive(archive: LifeArchive): Boolean = try {
        val text = ArchiveCodec.encode(archive)
        val previous = readRaw(KEY_ARCHIVE)
        val previousChk = readRaw(KEY_ARCHIVE_CHK)
        val editor = prefs.edit()
        if (previous != null) {
            editor.putString(KEY_ARCHIVE_BACKUP, previous)
            editor.putString(KEY_ARCHIVE_BACKUP_CHK, previousChk ?: SaveIntegrity.checksum(previous))
        }
        editor.putString(KEY_ARCHIVE, text)
        editor.putString(KEY_ARCHIVE_CHK, SaveIntegrity.checksum(text))
        editor.commit()
    } catch (error: Exception) {
        Log.e(TAG, "写入人生图鉴失败", error)
        false
    }

    /**
     * 记录一次已完成的人生。返回 true 表示档案确实发生了变化
     * （重复读取同一个已结束的存档不会重复计数）。
     */
    fun recordEnding(state: GameState): Boolean {
        val record = ArchiveFactory.lifeRecordOf(state) ?: return false
        val current = loadArchive()
        val updated = current
            .appended(record)
            .mergedWith(
                record,
                ArchiveFactory.discoveredRoutesOf(state),
                ArchiveFactory.discoveredNpcsOf(state)
            )
        if (updated == current) return false
        return writeArchive(updated)
    }

    fun clearArchive() {
        prefs.edit {
            remove(KEY_ARCHIVE)
            remove(KEY_ARCHIVE_CHK)
            remove(KEY_ARCHIVE_BACKUP)
            remove(KEY_ARCHIVE_BACKUP_CHK)
        }
    }

    // ===== 设置项 =====

    // 深色模式：null 表示跟随系统。
    fun darkModeOverride(): Boolean? =
        if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null

    fun setDarkModeOverride(value: Boolean?) {
        prefs.edit {
            if (value == null) remove(KEY_DARK) else putBoolean(KEY_DARK, value)
        }
    }

    // 新手引导：完成一次后不再出现。与存档分开保存。
    fun tutorialDone(): Boolean = prefs.getBoolean(KEY_TUTORIAL, false)

    fun setTutorialDone(done: Boolean) {
        prefs.edit { putBoolean(KEY_TUTORIAL, done) }
    }

    // 音效开关：默认开启，但音效非常克制。
    fun soundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND, enabled) }
    }

    // 震动反馈开关：默认开启，只在晋升、成就与重大事件时轻震一下。
    fun hapticsEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS, true)

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_HAPTICS, enabled) }
    }

    // ===== 第十七阶段：音乐与音效音量 =====
    // 旧版本没有这些键，读取时一律回落到默认值，不影响旧存档。

    /** 背景音乐开关：默认开启，音量刻意偏低，安静耐听。 */
    fun musicEnabled(): Boolean = prefs.getBoolean(KEY_MUSIC_ENABLED, true)

    fun setMusicEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MUSIC_ENABLED, enabled) }
    }

    /** 背景音乐音量，默认 30%，非法值自动回落到默认值。 */
    fun musicVolume(): Float = AudioSettings.sanitizeVolume(
        prefs.getFloat(KEY_MUSIC_VOLUME, AudioSettings.DEFAULT_MUSIC_VOLUME),
        AudioSettings.DEFAULT_MUSIC_VOLUME
    )

    fun setMusicVolume(volume: Float) {
        prefs.edit { putFloat(KEY_MUSIC_VOLUME, AudioSettings.clampVolume(volume)) }
    }

    /** 音效音量，默认 45%。 */
    fun sfxVolume(): Float = AudioSettings.sanitizeVolume(
        prefs.getFloat(KEY_SFX_VOLUME, AudioSettings.DEFAULT_SFX_VOLUME),
        AudioSettings.DEFAULT_SFX_VOLUME
    )

    fun setSfxVolume(volume: Float) {
        prefs.edit { putFloat(KEY_SFX_VOLUME, AudioSettings.clampVolume(volume)) }
    }

    companion object {
        private const val TAG = "SaveManager"
        private const val PREFS_NAME = "shitu_save_store"
        private const val KEY_RUN_STARTED = "run_started_at"
        private const val KEY_AUTO = "save_auto"
        private const val KEY_MANUAL = "save_manual"
        private const val KEY_AUTO_TIME = "save_auto_time"
        private const val KEY_MANUAL_TIME = "save_manual_time"
        private const val KEY_AUTO_CHK = "save_auto_chk"
        private const val KEY_MANUAL_CHK = "save_manual_chk"
        private const val KEY_AUTO_BACKUP = "save_auto_backup"
        private const val KEY_MANUAL_BACKUP = "save_manual_backup"
        private const val KEY_AUTO_BACKUP_CHK = "save_auto_backup_chk"
        private const val KEY_MANUAL_BACKUP_CHK = "save_manual_backup_chk"
        private const val KEY_DARK = "setting_dark_mode"
        private const val KEY_ARCHIVE = "life_archive"
        private const val KEY_ARCHIVE_CHK = "life_archive_chk"
        private const val KEY_ARCHIVE_BACKUP = "life_archive_backup"
        private const val KEY_ARCHIVE_BACKUP_CHK = "life_archive_backup_chk"
        private const val KEY_TUTORIAL = "setting_tutorial_done"
        private const val KEY_SOUND = "setting_sound_enabled"
        private const val KEY_HAPTICS = "setting_haptics_enabled"
        private const val KEY_MUSIC_ENABLED = "setting_music_enabled"
        private const val KEY_MUSIC_VOLUME = "setting_music_volume"
        private const val KEY_SFX_VOLUME = "setting_sfx_volume"
    }
}