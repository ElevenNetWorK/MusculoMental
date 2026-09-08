package com.musculomental.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.musculomental.app.domain.PlannedExercise
import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.WorkoutPlan
import com.musculomental.app.domain.WorkoutPlanRepository
import com.musculomental.app.domain.WorkoutPlanStatus
import com.musculomental.app.domain.validateWorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "workout_plan")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val status: String,
    val source: String = "PERSONAL",
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "planned_exercise",
    foreignKeys = [
        ForeignKey(entity = WorkoutPlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
    indices = [Index("planId"), Index("exerciseId"), Index(value = ["planId", "position"], unique = true)],
)
data class PlannedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseId: String,
    val position: Int,
    val sets: Int,
    val repetitions: Int,
    val suggestedLoadKg: Double?,
    val restSeconds: Int,
)

data class WorkoutPlanRow(
    val planId: Long,
    val planName: String,
    val planStatus: String,
    val planCreatedAt: Long,
    val planUpdatedAt: Long,
    val itemId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val position: Int,
    val sets: Int,
    val repetitions: Int,
    val suggestedLoadKg: Double?,
    val restSeconds: Int,
)

@Dao
interface WorkoutPlanDao {
    @Query("""
        SELECT p.id AS planId, p.name AS planName, p.status AS planStatus,
               p.createdAt AS planCreatedAt, p.updatedAt AS planUpdatedAt,
               pe.id AS itemId, pe.exerciseId AS exerciseId, e.name AS exerciseName,
               pe.position AS position, pe.sets AS sets, pe.repetitions AS repetitions,
               pe.suggestedLoadKg AS suggestedLoadKg, pe.restSeconds AS restSeconds
        FROM workout_plan p
        JOIN planned_exercise pe ON pe.planId = p.id
        JOIN exercise e ON e.id = pe.exerciseId
        ORDER BY p.updatedAt DESC, pe.position ASC
    """)
    fun observeRows(): Flow<List<WorkoutPlanRow>>

    @Query("SELECT * FROM workout_plan WHERE id = :id") suspend fun plan(id: Long): WorkoutPlanEntity?
    @Query("SELECT * FROM planned_exercise WHERE planId = :id ORDER BY position") suspend fun items(id: Long): List<PlannedExerciseEntity>
    @Insert suspend fun insertPlan(plan: WorkoutPlanEntity): Long
    @Update suspend fun updatePlan(plan: WorkoutPlanEntity)
    @Insert suspend fun insertItems(items: List<PlannedExerciseEntity>)
    @Query("DELETE FROM planned_exercise WHERE planId = :planId") suspend fun deleteItems(planId: Long)

    @Transaction
    suspend fun replace(plan: WorkoutPlanEntity, items: List<PlannedExerciseEntity>): Long {
        val id = if (plan.id == 0L) insertPlan(plan) else { updatePlan(plan); plan.id }
        deleteItems(id)
        insertItems(items.map { it.copy(planId = id) })
        return id
    }
}

class LocalWorkoutPlanRepository(private val dao: WorkoutPlanDao) : WorkoutPlanRepository {
    override fun observePlans(): Flow<List<WorkoutPlan>> = dao.observeRows().map { rows ->
        rows.groupBy { it.planId }.values.map { planRows ->
            val first = planRows.first()
            WorkoutPlan(
                id = first.planId,
                name = first.planName,
                status = WorkoutPlanStatus.valueOf(first.planStatus),
                exercises = planRows.map { row ->
                    PlannedExercise(row.itemId, row.exerciseId, row.exerciseName, row.position, row.sets, row.repetitions, row.suggestedLoadKg, row.restSeconds)
                },
                createdAt = first.planCreatedAt,
                updatedAt = first.planUpdatedAt,
            )
        }
    }

    override suspend fun save(id: Long?, name: String, exercises: List<PlannedExerciseDraft>): Long {
        require(validateWorkoutPlan(name, exercises).isValid)
        val now = System.currentTimeMillis()
        val existing = id?.let { dao.plan(it) }
        require(id == null || existing != null) { "Treino não encontrado." }
        val plan = WorkoutPlanEntity(
            id = existing?.id ?: 0,
            name = name.trim(),
            status = existing?.status ?: WorkoutPlanStatus.ACTIVE.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return dao.replace(plan, exercises.mapIndexed { index, item -> item.toEntity(plan.id, index) })
    }

    override suspend fun duplicate(id: Long): Long {
        val source = requireNotNull(dao.plan(id)) { "Treino não encontrado." }
        val items = dao.items(id)
        val now = System.currentTimeMillis()
        return dao.replace(
            WorkoutPlanEntity(name = "${source.name} (cópia)", status = WorkoutPlanStatus.ACTIVE.name, createdAt = now, updatedAt = now),
            items.map { it.copy(id = 0, planId = 0) },
        )
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        val source = requireNotNull(dao.plan(id)) { "Treino não encontrado." }
        dao.updatePlan(source.copy(status = if (archived) WorkoutPlanStatus.ARCHIVED.name else WorkoutPlanStatus.ACTIVE.name, updatedAt = System.currentTimeMillis()))
    }

    private fun PlannedExerciseDraft.toEntity(planId: Long, index: Int) = PlannedExerciseEntity(
        planId = planId,
        exerciseId = exerciseId,
        position = index,
        sets = sets.toInt(),
        repetitions = repetitions.toInt(),
        suggestedLoadKg = suggestedLoadKg.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDouble(),
        restSeconds = restSeconds.toInt(),
    )
}
