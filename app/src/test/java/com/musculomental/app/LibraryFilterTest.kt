package com.musculomental.app

import com.musculomental.app.domain.Equipment
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.ExerciseMuscle
import com.musculomental.app.domain.Muscle
import com.musculomental.app.domain.MuscleRole
import com.musculomental.app.domain.filterExercises
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {
    private val chest = Muscle("chest", "Peitoral maior", "Pectoralis major", "Peito", "Função")
    private val arm = Muscle("arm", "Bíceps braquial", "Biceps brachii", "Braços", "Função")
    private val exercises = listOf(
        Exercise("curl", "Rosca direta", Equipment.BARBELL, listOf("Faça"), listOf("Erro"), listOf(ExerciseMuscle(arm, MuscleRole.PRIMARY))),
        Exercise("bench", "Supino reto", Equipment.BARBELL, listOf("Faça"), listOf("Erro"), listOf(ExerciseMuscle(chest, MuscleRole.PRIMARY))),
    )

    @Test fun `search matches exercise common and anatomical muscle names ignoring case`() {
        assertEquals(listOf("curl"), filterExercises(exercises, "ROSCA", null, null).map { it.id })
        assertEquals(listOf("bench"), filterExercises(exercises, "peitoral", null, null).map { it.id })
        assertEquals(listOf("curl"), filterExercises(exercises, "biceps brachii", null, null).map { it.id })
    }

    @Test fun `region and equipment filters combine`() {
        assertEquals(listOf("bench"), filterExercises(exercises, "", "Peito", Equipment.BARBELL).map { it.id })
        assertEquals(emptyList<String>(), filterExercises(exercises, "", "Peito", Equipment.DUMBBELL).map { it.id })
    }
}
