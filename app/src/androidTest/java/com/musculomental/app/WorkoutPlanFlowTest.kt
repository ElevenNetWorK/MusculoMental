package com.musculomental.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.LocalExerciseLibraryRepository
import com.musculomental.app.data.LocalProfileRepository
import com.musculomental.app.data.LocalWorkoutPlanRepository
import com.musculomental.app.data.LocalWorkoutSessionRepository
import com.musculomental.app.data.LocalRestTimerRepository
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.feature.library.LibraryViewModel
import com.musculomental.app.feature.profile.MentalApp
import com.musculomental.app.feature.profile.ProfileViewModel
import com.musculomental.app.feature.workout.WorkoutPlanViewModel
import com.musculomental.app.feature.session.WorkoutSessionViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutPlanFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: ProfileDatabase
    private lateinit var workoutModel: WorkoutPlanViewModel
    private lateinit var libraryModel: LibraryViewModel
    private lateinit var sessionModel: WorkoutSessionViewModel

    @Before fun prepare() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, ProfileDatabase::class.java).build()
        lateinit var profileModel: ProfileViewModel
        compose.runOnUiThread {
            profileModel = ProfileViewModel(LocalProfileRepository(database.profileDao()), SavedStateHandle())
            libraryModel = LibraryViewModel(LocalExerciseLibraryRepository(database.exerciseDao()))
            workoutModel = WorkoutPlanViewModel(LocalWorkoutPlanRepository(database.workoutPlanDao()))
            sessionModel = WorkoutSessionViewModel(LocalWorkoutSessionRepository(database.workoutSessionDao()), LocalRestTimerRepository(database.restTimerDao()))
        }
        compose.setContent { MentalApp(profileModel, libraryModel, workoutModel, sessionModel) }
        compose.waitUntil(10_000) { !profileModel.state.value.loading && !libraryModel.state.value.loading && !workoutModel.state.value.loading }
    }

    @After fun finish() { database.close() }

    @Test fun createAWorkoutFromTheLocalLibrary() {
        compose.onNodeWithText("Meus treinos").performScrollTo().performClick()
        compose.onNodeWithText("Você ainda não criou um treino.").assertIsDisplayed()
        compose.onNodeWithText("Criar treino").performClick()
        compose.onNodeWithText("Nome do treino").performTextReplacement("Treino superior")
        compose.onNodeWithText("Adicionar Supino reto").performScrollTo().performClick()
        compose.onNodeWithText("Salvar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { workoutModel.state.value.plans.size == 1 && !workoutModel.editor.value.saving }
        compose.onNodeWithText("Treino superior").assertIsDisplayed()
        compose.onNodeWithText("Planejado: 3 séries de 10 repetições").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Intervalo planejado: 60 s").assertIsDisplayed()
    }
}
