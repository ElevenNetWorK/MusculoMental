package com.musculomental.app

import android.accessibilityservice.AccessibilityService
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
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.SessionStatus
import com.musculomental.app.feature.library.LibraryViewModel
import com.musculomental.app.feature.profile.MentalApp
import com.musculomental.app.feature.profile.ProfileViewModel
import com.musculomental.app.feature.session.WorkoutSessionViewModel
import com.musculomental.app.feature.workout.WorkoutPlanViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutSessionFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: ProfileDatabase
    private lateinit var workoutModel: WorkoutPlanViewModel
    private lateinit var sessionModel: WorkoutSessionViewModel

    @Before fun prepare() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, ProfileDatabase::class.java).build()
        runBlocking {
            LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first()
            LocalWorkoutPlanRepository(database.workoutPlanDao()).save(
                null, "Treino de teste", listOf(PlannedExerciseDraft("supino-reto", "Supino reto", "1", "10", "20", "60")),
            )
        }
        lateinit var profileModel: ProfileViewModel
        lateinit var libraryModel: LibraryViewModel
        compose.runOnUiThread {
            profileModel = ProfileViewModel(LocalProfileRepository(database.profileDao()), SavedStateHandle())
            libraryModel = LibraryViewModel(LocalExerciseLibraryRepository(database.exerciseDao()))
            workoutModel = WorkoutPlanViewModel(LocalWorkoutPlanRepository(database.workoutPlanDao()))
            sessionModel = WorkoutSessionViewModel(LocalWorkoutSessionRepository(database.workoutSessionDao()), LocalRestTimerRepository(database.restTimerDao()))
        }
        compose.setContent { MentalApp(profileModel, libraryModel, workoutModel, sessionModel) }
        compose.waitUntil(10_000) { !profileModel.state.value.loading && workoutModel.state.value.plans.size == 1 && !sessionModel.state.value.loading }
    }

    @After fun finish() { database.close() }

    @Test fun startRegisterAndCompleteAWorkout() {
        compose.onNodeWithText("Meus treinos").performScrollTo().performClick()
        compose.onNodeWithText("Ver ficha").performClick()
        compose.onNodeWithText("Iniciar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession() != null }
        compose.onNodeWithText("Planejado: 1 séries · 10 repetições · 20 kg · 60 s").assertIsDisplayed()
        compose.onNodeWithText("Concluir série").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession()?.exercises?.single()?.sets?.single()?.status == PerformedSetStatus.COMPLETED }
        compose.onNodeWithText("Iniciar intervalo").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.state.value.timers.size == 1 }
        compose.onNodeWithText("Intervalo · Supino reto · série 1").assertIsDisplayed()
        compose.onNodeWithText("Planejado: 60 s · alvo atual: 60 s").assertIsDisplayed()
        compose.onNodeWithText("Encerrar intervalo").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.state.value.timers.single().state.name == "FINISHED" }
        compose.onNodeWithText("Finalizar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.state.value.sessions.single().status == SessionStatus.COMPLETED }
        compose.onNodeWithText("Treino concluído").assertIsDisplayed()
        compose.onNodeWithText("Séries concluídas: 1").assertIsDisplayed()
        compose.onNodeWithText("Volume estimado: 200 kg").assertIsDisplayed()
    }

    @Test fun completedWorkoutAppearsInHistoryWithPlannedAndExecutedValues() {
        compose.onNodeWithText("Meus treinos").performScrollTo().performClick()
        compose.onNodeWithText("Ver ficha").performClick()
        compose.onNodeWithText("Iniciar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession() != null }
        compose.onNodeWithText("Concluir série").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession()?.completedSets == 1 }
        compose.onNodeWithText("Finalizar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.state.value.sessions.single().status == SessionStatus.COMPLETED }
        compose.onNodeWithText("Voltar aos treinos").performClick()
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("Histórico").performScrollTo().performClick()

        compose.onNodeWithText("1 treino esta semana").assertIsDisplayed()
        compose.onNodeWithText("Ver detalhes").performScrollTo().performClick()
        compose.onNodeWithText("Voltar ao histórico").assertIsDisplayed()
        compose.onNodeWithText("Planejado: 1 séries · 10 repetições · 20 kg · 60 s").assertIsDisplayed()
        compose.onNodeWithText("Série 1 executada: 10 reps · 20 kg").assertIsDisplayed()
        compose.onNodeWithText("Excluir sessão do histórico").performScrollTo().performClick()
        compose.onNodeWithText("Excluir esta sessão permanentemente? As séries e os intervalos registrados também serão apagados. A ficha de treino será preservada.").assertIsDisplayed()
        compose.onNodeWithText("Confirmar exclusão").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.state.value.sessions.isEmpty() && !sessionModel.state.value.busy }
        compose.onNodeWithText("Nenhum treino concluído ainda").assertIsDisplayed()
    }

    @Test fun systemBackPausesAndPreservesActiveWorkout() {
        compose.onNodeWithText("Meus treinos").performScrollTo().performClick()
        compose.onNodeWithText("Ver ficha").performClick()
        compose.onNodeWithText("Iniciar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession()?.status == SessionStatus.IN_PROGRESS && !sessionModel.state.value.busy }

        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

        compose.waitUntil(10_000) { sessionModel.activeSession()?.status == SessionStatus.PAUSED }
        compose.onNodeWithText("Meus treinos").assertIsDisplayed()
        compose.onNodeWithText("Ver ficha").performClick()
        compose.onNodeWithText("Retomar sessão").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession()?.status == SessionStatus.IN_PROGRESS }
        compose.onNodeWithText("Treino de teste").assertIsDisplayed()
    }

    @Test fun correctsCompletedSetWithoutChangingThePlan() {
        compose.onNodeWithText("Meus treinos").performScrollTo().performClick()
        compose.onNodeWithText("Ver ficha").performClick()
        compose.onNodeWithText("Iniciar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession() != null }
        compose.onNodeWithText("Concluir série").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.activeSession()?.completedSets == 1 }
        compose.onNodeWithText("Finalizar treino").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.state.value.sessions.single().status == SessionStatus.COMPLETED }

        compose.onNodeWithText("Corrigir registro").performScrollTo().performClick()
        compose.onNodeWithText("Executado: reps").performTextReplacement("12")
        compose.onNodeWithText("Salvar correção da série").performScrollTo().performClick()
        compose.waitUntil(10_000) { sessionModel.state.value.sessions.single().exercises.single().sets.single().repetitions == 12 }
        compose.onNodeWithText("Correção da série salva.").assertIsDisplayed()
        compose.onNodeWithText("Voltar ao resumo").performScrollTo().performClick()

        compose.onNodeWithText("Planejado: 1 séries · 10 repetições · 20 kg · 60 s").assertIsDisplayed()
        compose.onNodeWithText("Série 1 executada: 12 reps · 20 kg").assertIsDisplayed()
    }
}
