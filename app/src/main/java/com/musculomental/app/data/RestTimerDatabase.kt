package com.musculomental.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.musculomental.app.domain.RestInterval
import com.musculomental.app.domain.RestTimerPreferences
import com.musculomental.app.domain.RestTimerRepository
import com.musculomental.app.domain.TimerAlertMode
import com.musculomental.app.domain.TimerCountMode
import com.musculomental.app.domain.TimerStartMode
import com.musculomental.app.domain.TimerState
import com.musculomental.app.domain.validateTimerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(
    tableName = "rest_interval",
    foreignKeys = [ForeignKey(entity = PerformedSetEntity::class, parentColumns = ["id"], childColumns = ["performedSetId"], onDelete = ForeignKey.CASCADE)],
)
data class RestIntervalEntity(
    @PrimaryKey val performedSetId: Long,
    val plannedSeconds: Int,
    val targetSeconds: Int,
    val countMode: String,
    val state: String,
    val accumulatedSeconds: Int,
    val updatedAt: Long,
    val finishedAt: Long?,
    val actualSeconds: Int?,
    val adjusted: Boolean,
    val approximate: Boolean,
    val alerted: Boolean,
)

@Entity(tableName = "timer_preferences")
data class TimerPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean,
    val startMode: String,
    val countMode: String,
    val alertMode: String,
    val recordActual: Boolean,
    val finishOnNextSet: Boolean,
    val defaultSeconds: Int,
)

@Dao
interface RestTimerDao {
    @Query("SELECT * FROM rest_interval") fun observeAll(): Flow<List<RestIntervalEntity>>
    @Query("SELECT * FROM timer_preferences WHERE id = 1") fun observePreferences(): Flow<TimerPreferencesEntity?>
    @Query("SELECT * FROM timer_preferences WHERE id = 1") suspend fun preferences(): TimerPreferencesEntity?
    @Query("SELECT * FROM rest_interval WHERE performedSetId = :setId") suspend fun timer(setId: Long): RestIntervalEntity?
    @Query("SELECT * FROM rest_interval WHERE state IN ('RUNNING', 'PAUSED')") suspend fun activeTimers(): List<RestIntervalEntity>
    @Upsert suspend fun upsertTimer(value: RestIntervalEntity)
    @Upsert suspend fun upsertPreferences(value: TimerPreferencesEntity)
    @Query("DELETE FROM rest_interval WHERE performedSetId = :setId") suspend fun delete(setId: Long)
}

class LocalRestTimerRepository(private val dao: RestTimerDao) : RestTimerRepository {
    override fun observeTimers(): Flow<List<RestInterval>> = dao.observeAll().map { values -> values.map { it.toDomain() } }
    override fun observePreferences(): Flow<RestTimerPreferences> = dao.observePreferences().map { it?.toDomain() ?: RestTimerPreferences() }

    override suspend fun start(setId: Long, plannedSeconds: Int, preferences: RestTimerPreferences) {
        require(validateTimerPreferences(preferences) == null)
        finishActive()
        val now = System.currentTimeMillis()
        dao.upsertTimer(
            RestIntervalEntity(
                performedSetId = setId, plannedSeconds = plannedSeconds, targetSeconds = plannedSeconds.takeIf { it > 0 } ?: preferences.defaultSeconds,
                countMode = preferences.countMode.name, state = TimerState.RUNNING.name, accumulatedSeconds = 0,
                updatedAt = now, finishedAt = null, actualSeconds = null, adjusted = false, approximate = false, alerted = false,
            )
        )
    }

    override suspend fun pause(setId: Long) {
        val timer = requireNotNull(dao.timer(setId))
        if (timer.state != TimerState.RUNNING.name) return
        val now = System.currentTimeMillis()
        dao.upsertTimer(timer.copy(state = TimerState.PAUSED.name, accumulatedSeconds = timer.elapsedAt(now), updatedAt = now))
    }

    override suspend fun resume(setId: Long) {
        val timer = requireNotNull(dao.timer(setId))
        if (timer.state != TimerState.PAUSED.name) return
        dao.upsertTimer(timer.copy(state = TimerState.RUNNING.name, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun adjust(setId: Long, deltaSeconds: Int) {
        val timer = requireNotNull(dao.timer(setId))
        val target = (timer.targetSeconds + deltaSeconds).coerceIn(5, 3600)
        dao.upsertTimer(timer.copy(targetSeconds = target, adjusted = true, alerted = if (target > timer.elapsedAt(System.currentTimeMillis())) false else timer.alerted))
    }

    override suspend fun finish(setId: Long, correctedSeconds: Int?) {
        val timer = requireNotNull(dao.timer(setId))
        val preferences = dao.preferences()?.toDomain() ?: RestTimerPreferences()
        val now = System.currentTimeMillis()
        val elapsed = correctedSeconds ?: timer.elapsedAt(now)
        require(elapsed in 0..7200)
        dao.upsertTimer(
            timer.copy(
                state = TimerState.FINISHED.name, accumulatedSeconds = elapsed, updatedAt = now, finishedAt = now,
                actualSeconds = if (preferences.recordActual) elapsed else null,
                approximate = timer.approximate || correctedSeconds != null,
            )
        )
    }

    override suspend fun finishActive() {
        dao.activeTimers().forEach { finish(it.performedSetId) }
    }
    override suspend fun markAlerted(setId: Long) {
        dao.timer(setId)?.let { dao.upsertTimer(it.copy(alerted = true)) }
    }
    override suspend fun delete(setId: Long) = dao.delete(setId)
    override suspend fun savePreferences(value: RestTimerPreferences) {
        require(validateTimerPreferences(value) == null)
        dao.upsertPreferences(value.toEntity())
    }

    private fun RestIntervalEntity.elapsedAt(now: Long) = accumulatedSeconds + if (state == TimerState.RUNNING.name) ((now - updatedAt) / 1000).toInt().coerceAtLeast(0) else 0
    private fun RestIntervalEntity.toDomain() = RestInterval(performedSetId, plannedSeconds, targetSeconds, TimerCountMode.valueOf(countMode), TimerState.valueOf(state), accumulatedSeconds, updatedAt, finishedAt, actualSeconds, adjusted, approximate, alerted)
    private fun TimerPreferencesEntity.toDomain() = RestTimerPreferences(enabled, TimerStartMode.valueOf(startMode), TimerCountMode.valueOf(countMode), TimerAlertMode.valueOf(alertMode), recordActual, finishOnNextSet, defaultSeconds)
    private fun RestTimerPreferences.toEntity() = TimerPreferencesEntity(enabled = enabled, startMode = startMode.name, countMode = countMode.name, alertMode = alertMode.name, recordActual = recordActual, finishOnNextSet = finishOnNextSet, defaultSeconds = defaultSeconds)
}
