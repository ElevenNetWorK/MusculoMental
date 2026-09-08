package com.musculomental.app.domain

import kotlinx.coroutines.flow.Flow

enum class TrainingMode(val label: String) {
    AUTONOMOUS("Treino autônomo"), STUDENT_DEMO("Aluno demonstrativo")
}

enum class Experience(val label: String) {
    NEW("Nunca treinei"), BEGINNER("Iniciante"), INTERMEDIATE("Intermediário"), ADVANCED("Avançado")
}

data class UserProfile(
    val displayName: String,
    val goal: String,
    val experience: Experience,
    val weeklyFrequency: Int,
    val mode: TrainingMode,
)

data class ProfileErrors(val name: String? = null, val goal: String? = null, val frequency: String? = null) {
    val isValid: Boolean get() = name == null && goal == null && frequency == null
}

fun validateProfile(name: String, goal: String, frequency: String): ProfileErrors = ProfileErrors(
    name = when {
        name.isBlank() -> "Informe como quer ser chamado."
        name.trim().length > 60 -> "Use até 60 caracteres."
        else -> null
    },
    goal = when {
        goal.isBlank() -> "Informe seu objetivo."
        goal.trim().length > 120 -> "Use até 120 caracteres."
        else -> null
    },
    frequency = if (frequency.toIntOrNull() in 1..7) null else "Informe de 1 a 7 dias por semana.",
)

interface ProfileRepository {
    fun observe(): Flow<UserProfile?>
    suspend fun save(profile: UserProfile)
}
