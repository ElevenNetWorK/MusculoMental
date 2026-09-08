package com.musculomental.app.domain

import kotlinx.coroutines.flow.Flow

enum class SessionStatus(val label: String) {
    IN_PROGRESS("Em andamento"), PAUSED("Pausado"), COMPLETED("Concluído"), CANCELLED("Cancelado")
}

enum class PerformedSetStatus(val label: String) {
    PLANNED("Planejada"), COMPLETED("Concluída"), SKIPPED("Ignorada")
}

data class PerformedSet(
    val id: Long,
    val setNumber: Int,
    val status: PerformedSetStatus,
    val repetitions: Int?,
    val loadKg: Double?,
    val completedAt: Long?,
)

data class SessionExercise(
    val id: Long,
    val exerciseId: String,
    val exerciseName: String,
    val position: Int,
    val plannedSets: Int,
    val plannedRepetitions: Int,
    val plannedLoadKg: Double?,
    val plannedRestSeconds: Int,
    val sets: List<PerformedSet>,
)

data class WorkoutSession(
    val id: Long,
    val sourcePlanId: Long,
    val planName: String,
    val status: SessionStatus,
    val startedAt: Long,
    val endedAt: Long?,
    val currentExercisePosition: Int,
    val note: String,
    val effort: Int?,
    val discomfort: Int?,
    val exercises: List<SessionExercise>,
) {
    val completedSets get() = exercises.sumOf { exercise -> exercise.sets.count { it.status == PerformedSetStatus.COMPLETED } }
    val skippedSets get() = exercises.sumOf { exercise -> exercise.sets.count { it.status == PerformedSetStatus.SKIPPED } }
    val totalSets get() = exercises.sumOf { it.sets.size }
    val estimatedVolumeKg get() = exercises.sumOf { exercise ->
        exercise.sets.filter { it.status == PerformedSetStatus.COMPLETED }.sumOf { (it.loadKg ?: 0.0) * (it.repetitions ?: 0) }
    }
}

data class SetEntryErrors(val repetitions: String? = null, val load: String? = null) {
    val isValid get() = repetitions == null && load == null
}

fun validateSetEntry(repetitions: String, load: String): SetEntryErrors {
    val parsedRepetitions = repetitions.toIntOrNull()
    val parsedLoad = load.replace(',', '.').toDoubleOrNull()
    return SetEntryErrors(
        repetitions = if (parsedRepetitions == null || parsedRepetitions !in 1..1000) "Informe de 1 a 1000 repetições." else null,
        load = if (load.isNotBlank() && (parsedLoad == null || parsedLoad < 0 || parsedLoad > 1000)) "Informe de 0 a 1000 kg ou deixe em branco." else null,
    )
}

fun validateSessionFeedback(note: String, effort: Int?, discomfort: Int?): String? = when {
    note.length > 500 -> "Use no máximo 500 caracteres na observação."
    effort != null && effort !in 1..10 -> "O esforço deve ficar entre 1 e 10."
    discomfort != null && discomfort !in 1..10 -> "O desconforto deve ficar entre 1 e 10."
    else -> null
}

interface WorkoutSessionRepository {
    fun observeSessions(): Flow<List<WorkoutSession>>
    suspend fun start(plan: WorkoutPlan): Long
    suspend fun setCurrentExercise(sessionId: Long, position: Int)
    suspend fun saveSet(setId: Long, repetitions: Int?, loadKg: Double?, status: PerformedSetStatus)
    suspend fun correctSet(setId: Long, repetitions: Int?, loadKg: Double?, status: PerformedSetStatus) =
        saveSet(setId, repetitions, loadKg, status)
    suspend fun saveFeedback(sessionId: Long, note: String, effort: Int?, discomfort: Int?)
    suspend fun setPaused(sessionId: Long, paused: Boolean)
    suspend fun complete(sessionId: Long)
    suspend fun cancel(sessionId: Long)
    suspend fun deleteCompleted(sessionId: Long)
}
