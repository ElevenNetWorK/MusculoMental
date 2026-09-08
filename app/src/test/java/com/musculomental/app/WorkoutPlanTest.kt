package com.musculomental.app

import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.validateWorkoutPlan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanTest {
    private val valid = PlannedExerciseDraft("supino-reto", "Supino reto")

    @Test fun `a plan requires a name and at least one valid exercise`() {
        assertFalse(validateWorkoutPlan("", emptyList()).isValid)
        assertNotNull(validateWorkoutPlan("Treino A", emptyList()).exercises)
        assertTrue(validateWorkoutPlan("Treino A", listOf(valid)).isValid)
        assertNotNull(validateWorkoutPlan("x".repeat(81), listOf(valid)).name)
    }

    @Test fun `planned values have safe storage boundaries`() {
        val invalid = listOf(
            valid.copy(sets = "0"), valid.copy(sets = "21"),
            valid.copy(repetitions = "0"), valid.copy(repetitions = "101"),
            valid.copy(suggestedLoadKg = "-1"), valid.copy(suggestedLoadKg = "texto"),
            valid.copy(restSeconds = "-1"), valid.copy(restSeconds = "601"),
        )
        invalid.forEach { assertFalse(validateWorkoutPlan("Treino", listOf(it)).isValid) }
        assertTrue(validateWorkoutPlan("Treino", listOf(valid.copy(suggestedLoadKg = "12,5", restSeconds = "0"))).isValid)
    }
}
