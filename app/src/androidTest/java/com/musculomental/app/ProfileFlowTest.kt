package com.musculomental.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.LocalProfileRepository
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.feature.profile.MentalApp
import com.musculomental.app.feature.profile.ProfileViewModel
import com.musculomental.app.feature.library.LibraryViewModel
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.ExerciseLibraryRepository
import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.WorkoutPlan
import com.musculomental.app.domain.WorkoutPlanRepository
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.domain.WorkoutSessionRepository
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.RestInterval
import com.musculomental.app.domain.RestTimerPreferences
import com.musculomental.app.domain.RestTimerRepository
import com.musculomental.app.feature.workout.WorkoutPlanViewModel
import com.musculomental.app.feature.session.WorkoutSessionViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: ProfileDatabase
    private lateinit var model: ProfileViewModel
    private lateinit var libraryModel: LibraryViewModel
    private lateinit var workoutModel: WorkoutPlanViewModel
    private lateinit var sessionModel: WorkoutSessionViewModel

    @Before fun prepare() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, ProfileDatabase::class.java).build()
        compose.runOnUiThread {
            model = ProfileViewModel(LocalProfileRepository(database.profileDao()), SavedStateHandle())
            libraryModel = LibraryViewModel(EmptyLibraryRepository)
            workoutModel = WorkoutPlanViewModel(EmptyWorkoutRepository)
            sessionModel = WorkoutSessionViewModel(EmptySessionRepository, EmptyRestTimerRepository)
        }
        compose.setContent { MentalApp(model, libraryModel, workoutModel, sessionModel) }
        compose.waitUntil(10_000) { !model.state.value.loading }
    }

    @After fun finish() { database.close() }

    @Test fun createEditAndCancelProfile() {
        compose.onNodeWithText("Criar meu perfil").performScrollTo().performClick()
        compose.onNodeWithText("Nome de exibição").performTextReplacement("Pessoa teste")
        compose.onNodeWithText("Objetivo", substring = false).performTextReplacement("Criar rotina")
        compose.onNodeWithText("Salvar perfil").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.state.value.profile != null && !model.state.value.saving }
        compose.onNodeWithText("Olá, Pessoa teste").assertIsDisplayed()
        compose.onNodeWithText("Editar perfil").performScrollTo().performClick()
        compose.onNodeWithText("Objetivo", substring = false).performTextReplacement("Nova meta")
        compose.onNodeWithText("Salvar perfil").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.state.value.profile?.goal == "Nova meta" && !model.state.value.saving }
        compose.onNodeWithText("Objetivo: Nova meta").assertIsDisplayed()
        compose.onNodeWithText("Editar perfil").performScrollTo().performClick()
        compose.onNodeWithText("Nome de exibição").performTextReplacement("Não salvar")
        compose.onNodeWithText("Voltar sem salvar").performScrollTo().performClick()
        compose.onNodeWithText("Olá, Pessoa teste").assertIsDisplayed()
    }
}

private object EmptyLibraryRepository : ExerciseLibraryRepository {
    override fun observeExercises() = flowOf<List<Exercise>>(emptyList())
}

private object EmptyWorkoutRepository : WorkoutPlanRepository {
    override fun observePlans() = flowOf<List<WorkoutPlan>>(emptyList())
    override suspend fun save(id: Long?, name: String, exercises: List<PlannedExerciseDraft>) = 1L
    override suspend fun duplicate(id: Long) = 1L
    override suspend fun setArchived(id: Long, archived: Boolean) = Unit
}

private object EmptySessionRepository : WorkoutSessionRepository {
    override fun observeSessions() = flowOf<List<WorkoutSession>>(emptyList())
    override suspend fun start(plan: WorkoutPlan) = 1L
    override suspend fun setCurrentExercise(sessionId: Long, position: Int) = Unit
    override suspend fun saveSet(setId: Long, repetitions: Int?, loadKg: Double?, status: PerformedSetStatus) = Unit
    override suspend fun saveFeedback(sessionId: Long, note: String, effort: Int?, discomfort: Int?) = Unit
    override suspend fun setPaused(sessionId: Long, paused: Boolean) = Unit
    override suspend fun complete(sessionId: Long) = Unit
    override suspend fun cancel(sessionId: Long) = Unit
    override suspend fun deleteCompleted(sessionId: Long) = Unit
}

private object EmptyRestTimerRepository : RestTimerRepository {
    override fun observeTimers() = flowOf<List<RestInterval>>(emptyList())
    override fun observePreferences() = flowOf(RestTimerPreferences())
    override suspend fun start(setId: Long, plannedSeconds: Int, preferences: RestTimerPreferences) = Unit
    override suspend fun pause(setId: Long) = Unit
    override suspend fun resume(setId: Long) = Unit
    override suspend fun adjust(setId: Long, deltaSeconds: Int) = Unit
    override suspend fun finish(setId: Long, correctedSeconds: Int?) = Unit
    override suspend fun finishActive() = Unit
    override suspend fun markAlerted(setId: Long) = Unit
    override suspend fun delete(setId: Long) = Unit
    override suspend fun savePreferences(value: RestTimerPreferences) = Unit
}
