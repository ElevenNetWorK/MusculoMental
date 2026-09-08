package com.musculomental.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.LocalExerciseLibraryRepository
import com.musculomental.app.data.LocalWorkoutPlanRepository
import com.musculomental.app.data.LocalWorkoutSessionRepository
import com.musculomental.app.data.LocalRestTimerRepository
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.RestTimerPreferences
import com.musculomental.app.domain.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionPersistenceTest {
    @Test fun snapshotPauseResumeCompleteAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "workout-session-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, ProfileDatabase::class.java, name)
            .addMigrations(ProfileDatabase.MIGRATION_1_2, ProfileDatabase.MIGRATION_2_3, ProfileDatabase.MIGRATION_3_4, ProfileDatabase.MIGRATION_4_5).build()
        var database = open()
        try {
            LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first()
            val plans = LocalWorkoutPlanRepository(database.workoutPlanDao())
            val planId = plans.save(null, "Treino original", listOf(PlannedExerciseDraft("supino-reto", "Supino reto", "2", "10", "30", "60")))
            val plan = plans.observePlans().first().single()
            var sessions = LocalWorkoutSessionRepository(database.workoutSessionDao())
            val sessionId = sessions.start(plan)
            plans.save(planId, "Treino alterado depois", listOf(PlannedExerciseDraft("agachamento", "Agachamento", "1", "5", "", "30")))
            var session = sessions.observeSessions().first().single()
            assertEquals("Treino original", session.planName)
            assertEquals("Supino reto", session.exercises.single().exerciseName)
            assertEquals(2, session.totalSets)
            val first = session.exercises.single().sets.first()
            val second = session.exercises.single().sets.last()
            sessions.saveSet(first.id, 9, 32.5, PerformedSetStatus.COMPLETED)
            sessions.saveSet(second.id, null, null, PerformedSetStatus.SKIPPED)
            sessions.saveFeedback(sessionId, "Boa sessão", 8, 2)
            sessions.setPaused(sessionId, true)
            database.close()
            database = open()
            sessions = LocalWorkoutSessionRepository(database.workoutSessionDao())
            session = sessions.observeSessions().first().single()
            assertEquals(SessionStatus.PAUSED, session.status)
            assertEquals(9, session.exercises.single().sets.first().repetitions)
            assertEquals("Boa sessão", session.note)
            sessions.setPaused(sessionId, false)
            sessions.complete(sessionId)
            session = sessions.observeSessions().first().single()
            assertEquals(SessionStatus.COMPLETED, session.status)
            assertTrue(session.endedAt != null)
            assertEquals(292.5, session.estimatedVolumeKg, 0.0)
            val originalEndedAt = session.endedAt
            val originalCompletedAt = session.exercises.single().sets.first().completedAt
            sessions.correctSet(first.id, 8, 35.0, PerformedSetStatus.COMPLETED)
            sessions.correctSet(second.id, 10, 20.0, PerformedSetStatus.COMPLETED)
            sessions.saveFeedback(sessionId, "Avaliação corrigida", 7, 1)
            session = sessions.observeSessions().first().single()
            assertEquals(originalEndedAt, session.endedAt)
            assertEquals(originalCompletedAt, session.exercises.single().sets.first().completedAt)
            assertEquals("Treino original", session.planName)
            assertEquals(480.0, session.estimatedVolumeKg, 0.0)
            assertEquals("Avaliação corrigida", session.note)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun deletingCompletedSessionCascadesSetsAndIntervalsButPreservesPlan() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, ProfileDatabase::class.java).build()
        try {
            LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first()
            val plans = LocalWorkoutPlanRepository(database.workoutPlanDao())
            plans.save(null, "Treino preservado", listOf(PlannedExerciseDraft("supino-reto", "Supino reto", "1", "10", "20", "60")))
            val plan = plans.observePlans().first().single()
            val sessions = LocalWorkoutSessionRepository(database.workoutSessionDao())
            val rests = LocalRestTimerRepository(database.restTimerDao())
            val sessionId = sessions.start(plan)
            val setId = sessions.observeSessions().first().single().exercises.single().sets.single().id

            assertTrue(runCatching { sessions.deleteCompleted(sessionId) }.isFailure)
            sessions.saveSet(setId, 10, 20.0, PerformedSetStatus.COMPLETED)
            rests.start(setId, 60, RestTimerPreferences())
            rests.finish(setId, 25)
            sessions.complete(sessionId)
            assertEquals(1, rests.observeTimers().first().size)

            sessions.deleteCompleted(sessionId)

            assertTrue(sessions.observeSessions().first().isEmpty())
            assertTrue(rests.observeTimers().first().isEmpty())
            assertEquals("Treino preservado", plans.observePlans().first().single().name)
        } finally {
            database.close()
        }
    }
}
