package com.musculomental.app.domain

import kotlinx.coroutines.flow.Flow

enum class WorkoutPlanStatus(val label: String) { ACTIVE("Ativo"), ARCHIVED("Arquivado") }

data class PlannedExercise(
    val id: Long = 0,
    val exerciseId: String,
    val exerciseName: String,
    val position: Int,
    val sets: Int,
    val repetitions: Int,
    val suggestedLoadKg: Double?,
    val restSeconds: Int,
)

data class WorkoutPlan(
    val id: Long = 0,
    val name: String,
    val status: WorkoutPlanStatus = WorkoutPlanStatus.ACTIVE,
    val exercises: List<PlannedExercise>,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class PlannedExerciseDraft(
    val exerciseId: String,
    val exerciseName: String,
    val sets: String = "3",
    val repetitions: String = "10",
    val suggestedLoadKg: String = "",
    val restSeconds: String = "60",
)

data class WorkoutPlanErrors(
    val name: String? = null,
    val exercises: String? = null,
    val exerciseErrors: Map<Int, String> = emptyMap(),
) {
    val isValid get() = name == null && exercises == null && exerciseErrors.isEmpty()
}

fun validateWorkoutPlan(name: String, exercises: List<PlannedExerciseDraft>): WorkoutPlanErrors {
    val nameError = when {
        name.isBlank() -> "Informe o nome do treino."
        name.trim().length > 80 -> "Use no máximo 80 caracteres."
        else -> null
    }
    val itemErrors = exercises.mapIndexedNotNull { index, item ->
        val sets = item.sets.toIntOrNull()
        val repetitions = item.repetitions.toIntOrNull()
        val rest = item.restSeconds.toIntOrNull()
        val load = item.suggestedLoadKg.replace(',', '.').toDoubleOrNull()
        val error = when {
            sets == null || sets !in 1..20 -> "Séries: informe um número de 1 a 20."
            repetitions == null || repetitions !in 1..100 -> "Repetições: informe um número de 1 a 100."
            item.suggestedLoadKg.isNotBlank() && (load == null || load < 0 || load > 1000) -> "Carga: informe de 0 a 1000 kg ou deixe em branco."
            rest == null || rest !in 0..600 -> "Intervalo: informe de 0 a 600 segundos."
            else -> null
        }
        error?.let { index to it }
    }.toMap()
    return WorkoutPlanErrors(nameError, if (exercises.isEmpty()) "Adicione ao menos um exercício." else null, itemErrors)
}

interface WorkoutPlanRepository {
    fun observePlans(): Flow<List<WorkoutPlan>>
    suspend fun save(id: Long?, name: String, exercises: List<PlannedExerciseDraft>): Long
    suspend fun duplicate(id: Long): Long
    suspend fun setArchived(id: Long, archived: Boolean)
}
