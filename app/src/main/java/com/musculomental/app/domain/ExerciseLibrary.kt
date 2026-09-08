package com.musculomental.app.domain

import kotlinx.coroutines.flow.Flow

enum class Equipment(val label: String) {
    BARBELL("Barra"), DUMBBELL("Halteres"), MACHINE("Máquina"), BODY_WEIGHT("Peso corporal")
}

enum class MuscleRole { PRIMARY, ASSISTANT }

data class Muscle(val id: String, val commonName: String, val anatomicalName: String, val region: String, val function: String)
data class ExerciseMuscle(val muscle: Muscle, val role: MuscleRole)
data class Exercise(
    val id: String,
    val name: String,
    val equipment: Equipment,
    val instructions: List<String>,
    val commonErrors: List<String>,
    val muscles: List<ExerciseMuscle>,
    val contentStatus: String = "Conteúdo demonstrativo em revisão",
)

interface ExerciseLibraryRepository { fun observeExercises(): Flow<List<Exercise>> }

fun filterExercises(exercises: List<Exercise>, query: String, region: String?, equipment: Equipment?): List<Exercise> {
    val normalized = query.trim().lowercase()
    return exercises.filter { exercise ->
        val matchesText = normalized.isEmpty() || exercise.name.lowercase().contains(normalized) || exercise.muscles.any {
            it.muscle.commonName.lowercase().contains(normalized) || it.muscle.anatomicalName.lowercase().contains(normalized)
        }
        val matchesRegion = region == null || exercise.muscles.any { it.muscle.region == region }
        val matchesEquipment = equipment == null || exercise.equipment == equipment
        matchesText && matchesRegion && matchesEquipment
    }.sortedBy { it.name }
}
