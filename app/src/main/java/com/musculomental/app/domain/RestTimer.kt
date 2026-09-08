package com.musculomental.app.domain

import kotlinx.coroutines.flow.Flow
import kotlin.math.max

enum class TimerState { RUNNING, PAUSED, FINISHED }
enum class TimerStartMode(val label: String) { MANUAL("Manual"), AUTOMATIC("Automático após a série") }
enum class TimerCountMode(val label: String) { COUNTDOWN("Regressiva"), COUNT_UP("Crescente") }
enum class TimerAlertMode(val label: String) { SILENT("Silencioso"), VIBRATION("Vibração"), SOUND("Som") }

data class RestTimerPreferences(
    val enabled: Boolean = true,
    val startMode: TimerStartMode = TimerStartMode.MANUAL,
    val countMode: TimerCountMode = TimerCountMode.COUNTDOWN,
    val alertMode: TimerAlertMode = TimerAlertMode.VIBRATION,
    val recordActual: Boolean = true,
    val finishOnNextSet: Boolean = true,
    val defaultSeconds: Int = 60,
)

data class RestInterval(
    val performedSetId: Long,
    val plannedSeconds: Int,
    val targetSeconds: Int,
    val countMode: TimerCountMode,
    val state: TimerState,
    val accumulatedSeconds: Int,
    val updatedAt: Long,
    val finishedAt: Long?,
    val actualSeconds: Int?,
    val adjusted: Boolean,
    val approximate: Boolean,
    val alerted: Boolean,
) {
    fun elapsedAt(now: Long): Int = accumulatedSeconds + if (state == TimerState.RUNNING) max(0, ((now - updatedAt) / 1000).toInt()) else 0
    fun remainingAt(now: Long): Int = targetSeconds - elapsedAt(now)
}

fun validateTimerPreferences(value: RestTimerPreferences): String? =
    if (value.defaultSeconds !in 5..3600) "O tempo padrão deve ficar entre 5 e 3600 segundos." else null

interface RestTimerRepository {
    fun observeTimers(): Flow<List<RestInterval>>
    fun observePreferences(): Flow<RestTimerPreferences>
    suspend fun start(setId: Long, plannedSeconds: Int, preferences: RestTimerPreferences)
    suspend fun pause(setId: Long)
    suspend fun resume(setId: Long)
    suspend fun adjust(setId: Long, deltaSeconds: Int)
    suspend fun finish(setId: Long, correctedSeconds: Int? = null)
    suspend fun finishActive()
    suspend fun markAlerted(setId: Long)
    suspend fun delete(setId: Long)
    suspend fun savePreferences(value: RestTimerPreferences)
}
