package com.musculomental.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.LocalExerciseLibraryRepository
import com.musculomental.app.data.LocalRestTimerRepository
import com.musculomental.app.data.LocalWorkoutPlanRepository
import com.musculomental.app.data.LocalWorkoutSessionRepository
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.RestTimerPreferences
import com.musculomental.app.domain.TimerAlertMode
import com.musculomental.app.domain.TimerCountMode
import com.musculomental.app.domain.TimerStartMode
import com.musculomental.app.domain.TimerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerPersistenceTest {
    @Test fun preferencesControlsCorrectionAndReopenPersist() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "rest-timer-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, ProfileDatabase::class.java, name)
            .addMigrations(ProfileDatabase.MIGRATION_1_2, ProfileDatabase.MIGRATION_2_3, ProfileDatabase.MIGRATION_3_4, ProfileDatabase.MIGRATION_4_5).build()
        var database = open()
        try {
            LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first()
            val planRepository = LocalWorkoutPlanRepository(database.workoutPlanDao())
            planRepository.save(null, "Treino", listOf(PlannedExerciseDraft("supino-reto", "Supino reto", "1", "10", "20", "90")))
            val plan = planRepository.observePlans().first().single()
            val sessionRepository = LocalWorkoutSessionRepository(database.workoutSessionDao())
            sessionRepository.start(plan)
            val set = sessionRepository.observeSessions().first().single().exercises.single().sets.single()
            sessionRepository.saveSet(set.id, 10, 20.0, PerformedSetStatus.COMPLETED)

            var timers = LocalRestTimerRepository(database.restTimerDao())
            val preferences = RestTimerPreferences(true, TimerStartMode.AUTOMATIC, TimerCountMode.COUNT_UP, TimerAlertMode.SILENT, true, true, 45)
            timers.savePreferences(preferences)
            timers.start(set.id, 90, preferences)
            timers.adjust(set.id, 15)
            timers.pause(set.id)
            timers.resume(set.id)
            timers.finish(set.id, 112)
            database.close()
            database = open()
            timers = LocalRestTimerRepository(database.restTimerDao())
            assertEquals(preferences, timers.observePreferences().first())
            val restored = timers.observeTimers().first().single()
            assertEquals(TimerState.FINISHED, restored.state)
            assertEquals(105, restored.targetSeconds)
            assertEquals(112, restored.actualSeconds)
            assertTrue(restored.adjusted)
            assertTrue(restored.approximate)
            timers.delete(set.id)
            assertTrue(timers.observeTimers().first().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
