package com.musculomental.app.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.musculomental.app.domain.PerformedSet
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.SessionExercise
import com.musculomental.app.domain.SessionStatus
import com.musculomental.app.domain.WorkoutPlan
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.domain.WorkoutSessionRepository
import com.musculomental.app.domain.validateSessionFeedback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "workout_session")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourcePlanId: Long,
    val planName: String,
    val status: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val currentExercisePosition: Int = 0,
    val note: String = "",
    val effort: Int? = null,
    val discomfort: Int? = null,
)

@Entity(
    tableName = "session_exercise",
    foreignKeys = [ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId"), Index(value = ["sessionId", "position"], unique = true)],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sourcePlannedExerciseId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val position: Int,
    val plannedSets: Int,
    val plannedRepetitions: Int,
    val plannedLoadKg: Double?,
    val plannedRestSeconds: Int,
)

@Entity(
    tableName = "performed_set",
    foreignKeys = [ForeignKey(entity = SessionExerciseEntity::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionExerciseId"), Index(value = ["sessionExerciseId", "setNumber"], unique = true)],
)
data class PerformedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val status: String,
    val repetitions: Int? = null,
    val loadKg: Double? = null,
    val completedAt: Long? = null,
)

data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId") val sets: List<PerformedSetEntity>,
)

data class WorkoutSessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>,
)

@Dao
interface WorkoutSessionDao {
    @Transaction @Query("SELECT * FROM workout_session ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutSessionWithExercises>>
    @Query("SELECT COUNT(*) FROM workout_session WHERE status IN ('IN_PROGRESS', 'PAUSED')") suspend fun activeCount(): Int
    @Insert suspend fun insertSession(value: WorkoutSessionEntity): Long
    @Insert suspend fun insertExercise(value: SessionExerciseEntity): Long
    @Insert suspend fun insertSets(values: List<PerformedSetEntity>)
    @Query("UPDATE workout_session SET currentExercisePosition = :position WHERE id = :sessionId AND status IN ('IN_PROGRESS', 'PAUSED')")
    suspend fun updatePosition(sessionId: Long, position: Int)
    @Query("UPDATE performed_set SET repetitions = :repetitions, loadKg = :loadKg, status = :status, completedAt = :completedAt WHERE id = :setId")
    suspend fun updateSet(setId: Long, repetitions: Int?, loadKg: Double?, status: String, completedAt: Long?)
    @Query("UPDATE performed_set SET repetitions = :repetitions, loadKg = :loadKg, status = :status WHERE id = :setId")
    suspend fun correctSet(setId: Long, repetitions: Int?, loadKg: Double?, status: String)
    @Query("UPDATE workout_session SET note = :note, effort = :effort, discomfort = :discomfort WHERE id = :sessionId")
    suspend fun updateFeedback(sessionId: Long, note: String, effort: Int?, discomfort: Int?)
    @Query("UPDATE workout_session SET status = :status WHERE id = :sessionId AND status IN ('IN_PROGRESS', 'PAUSED')")
    suspend fun updateActiveStatus(sessionId: Long, status: String)
    @Query("SELECT COUNT(*) FROM performed_set ps JOIN session_exercise se ON se.id = ps.sessionExerciseId WHERE se.sessionId = :sessionId AND ps.status = 'PLANNED'")
    suspend fun pendingSets(sessionId: Long): Int
    @Query("UPDATE workout_session SET status = 'COMPLETED', endedAt = :endedAt WHERE id = :sessionId AND status IN ('IN_PROGRESS', 'PAUSED')")
    suspend fun complete(sessionId: Long, endedAt: Long)
    @Query("UPDATE workout_session SET status = 'CANCELLED', endedAt = :endedAt WHERE id = :sessionId AND status IN ('IN_PROGRESS', 'PAUSED')")
    suspend fun cancel(sessionId: Long, endedAt: Long)
    @Query("DELETE FROM workout_session WHERE id = :sessionId AND status = 'COMPLETED'")
    suspend fun deleteCompleted(sessionId: Long): Int

    @Transaction
    suspend fun createFromPlan(plan: WorkoutPlan, now: Long): Long {
        check(activeCount() == 0) { "Já existe uma sessão em andamento." }
        val sessionId = insertSession(WorkoutSessionEntity(sourcePlanId = plan.id, planName = plan.name, status = SessionStatus.IN_PROGRESS.name, startedAt = now))
        plan.exercises.sortedBy { it.position }.forEach { planned ->
            val exerciseId = insertExercise(
                SessionExerciseEntity(
                    sessionId = sessionId, sourcePlannedExerciseId = planned.id, exerciseId = planned.exerciseId,
                    exerciseName = planned.exerciseName, position = planned.position, plannedSets = planned.sets,
                    plannedRepetitions = planned.repetitions, plannedLoadKg = planned.suggestedLoadKg,
                    plannedRestSeconds = planned.restSeconds,
                )
            )
            insertSets((1..planned.sets).map { number ->
                PerformedSetEntity(sessionExerciseId = exerciseId, setNumber = number, status = PerformedSetStatus.PLANNED.name)
            })
        }
        return sessionId
    }
}

class LocalWorkoutSessionRepository(private val dao: WorkoutSessionDao) : WorkoutSessionRepository {
    override fun observeSessions(): Flow<List<WorkoutSession>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    override suspend fun start(plan: WorkoutPlan): Long = dao.createFromPlan(plan, System.currentTimeMillis())
    override suspend fun setCurrentExercise(sessionId: Long, position: Int) = dao.updatePosition(sessionId, position)
    override suspend fun saveSet(setId: Long, repetitions: Int?, loadKg: Double?, status: PerformedSetStatus) {
        require(status != PerformedSetStatus.COMPLETED || (repetitions != null && repetitions in 1..1000))
        require(loadKg == null || loadKg in 0.0..1000.0)
        dao.updateSet(setId, repetitions, loadKg, status.name, if (status == PerformedSetStatus.COMPLETED) System.currentTimeMillis() else null)
    }
    override suspend fun correctSet(setId: Long, repetitions: Int?, loadKg: Double?, status: PerformedSetStatus) {
        require(status in listOf(PerformedSetStatus.COMPLETED, PerformedSetStatus.SKIPPED))
        require(status != PerformedSetStatus.COMPLETED || (repetitions != null && repetitions in 1..1000))
        require(loadKg == null || loadKg in 0.0..1000.0)
        dao.correctSet(setId, repetitions, loadKg, status.name)
    }
    override suspend fun saveFeedback(sessionId: Long, note: String, effort: Int?, discomfort: Int?) {
        require(validateSessionFeedback(note, effort, discomfort) == null)
        dao.updateFeedback(sessionId, note.trim(), effort, discomfort)
    }
    override suspend fun setPaused(sessionId: Long, paused: Boolean) = dao.updateActiveStatus(sessionId, if (paused) SessionStatus.PAUSED.name else SessionStatus.IN_PROGRESS.name)
    override suspend fun complete(sessionId: Long) {
        check(dao.pendingSets(sessionId) == 0) { "Conclua ou ignore todas as séries." }
        dao.complete(sessionId, System.currentTimeMillis())
    }
    override suspend fun cancel(sessionId: Long) = dao.cancel(sessionId, System.currentTimeMillis())
    override suspend fun deleteCompleted(sessionId: Long) {
        check(dao.deleteCompleted(sessionId) == 1) { "A sessão concluída não foi encontrada." }
    }

    private fun WorkoutSessionWithExercises.toDomain() = WorkoutSession(
        id = session.id, sourcePlanId = session.sourcePlanId, planName = session.planName,
        status = SessionStatus.valueOf(session.status), startedAt = session.startedAt, endedAt = session.endedAt,
        currentExercisePosition = session.currentExercisePosition, note = session.note, effort = session.effort,
        discomfort = session.discomfort,
        exercises = exercises.sortedBy { it.exercise.position }.map { row ->
            SessionExercise(
                id = row.exercise.id, exerciseId = row.exercise.exerciseId, exerciseName = row.exercise.exerciseName,
                position = row.exercise.position, plannedSets = row.exercise.plannedSets,
                plannedRepetitions = row.exercise.plannedRepetitions, plannedLoadKg = row.exercise.plannedLoadKg,
                plannedRestSeconds = row.exercise.plannedRestSeconds,
                sets = row.sets.sortedBy { it.setNumber }.map {
                    PerformedSet(it.id, it.setNumber, PerformedSetStatus.valueOf(it.status), it.repetitions, it.loadKg, it.completedAt)
                },
            )
        },
    )
}
