package com.musculomental.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.LocalExerciseLibraryRepository
import com.musculomental.app.data.LocalWorkoutPlanRepository
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.WorkoutPlanStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlanPersistenceTest {
    @Test fun createEditDuplicateArchiveAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "workout-plan-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, ProfileDatabase::class.java, name)
            .addMigrations(ProfileDatabase.MIGRATION_1_2, ProfileDatabase.MIGRATION_2_3, ProfileDatabase.MIGRATION_3_4, ProfileDatabase.MIGRATION_4_5).build()
        var database = open()
        try {
            LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first()
            var repository = LocalWorkoutPlanRepository(database.workoutPlanDao())
            val id = repository.save(null, "Treino A", listOf(
                PlannedExerciseDraft("supino-reto", "Supino reto", "4", "8", "40,5", "90"),
                PlannedExerciseDraft("agachamento", "Agachamento", "3", "10", "", "60"),
            ))
            var plans = repository.observePlans().first()
            assertEquals(listOf("Supino reto", "Agachamento"), plans.single().exercises.map { it.exerciseName })
            assertEquals(40.5, plans.single().exercises.first().suggestedLoadKg!!, 0.0)

            repository.save(id, "Treino A ajustado", listOf(PlannedExerciseDraft("agachamento", "Agachamento", "5", "6", "20", "120")))
            val duplicateId = repository.duplicate(id)
            repository.setArchived(id, true)
            database.close()
            database = open()
            repository = LocalWorkoutPlanRepository(database.workoutPlanDao())
            plans = repository.observePlans().first()
            assertEquals(2, plans.size)
            assertEquals(WorkoutPlanStatus.ARCHIVED, plans.first { it.id == id }.status)
            assertEquals("Treino A ajustado (cópia)", plans.first { it.id == duplicateId }.name)
            assertTrue(plans.first { it.id == duplicateId }.exercises.single().sets == 5)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
