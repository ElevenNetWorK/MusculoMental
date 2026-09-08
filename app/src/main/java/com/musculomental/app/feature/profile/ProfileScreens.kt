package com.musculomental.app.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.musculomental.app.domain.Experience
import com.musculomental.app.domain.TrainingMode
import com.musculomental.app.feature.library.ExerciseDetailScreen
import com.musculomental.app.feature.library.LibraryScreen
import com.musculomental.app.feature.library.LibraryViewModel
import com.musculomental.app.feature.library.MuscleDetailScreen
import com.musculomental.app.feature.history.WorkoutHistoryScreen
import com.musculomental.app.feature.workout.WorkoutPlanDetailScreen
import com.musculomental.app.feature.workout.WorkoutPlanEditorScreen
import com.musculomental.app.feature.workout.WorkoutPlanListScreen
import com.musculomental.app.feature.workout.WorkoutPlanViewModel
import com.musculomental.app.feature.session.WorkoutSessionScreen
import com.musculomental.app.feature.session.WorkoutSessionCorrectionScreen
import com.musculomental.app.feature.session.WorkoutSessionSummaryScreen
import com.musculomental.app.feature.session.WorkoutSessionViewModel

@Composable
fun MentalApp(model: ProfileViewModel, libraryModel: LibraryViewModel, workoutModel: WorkoutPlanViewModel, sessionModel: WorkoutSessionViewModel) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        val state by model.state.collectAsStateWithLifecycle()
        val libraryState by libraryModel.state.collectAsStateWithLifecycle()
        val workoutState by workoutModel.state.collectAsStateWithLifecycle()
        val sessionState by sessionModel.state.collectAsStateWithLifecycle()
        val navigation = rememberNavController()
        LaunchedEffect(state.saved) {
            if (state.saved) {
                navigation.popBackStack("home", inclusive = false)
                model.consumeSaved()
            }
        }
        LaunchedEffect(sessionState.startedId) {
            sessionState.startedId?.let { id ->
                sessionModel.consumeStarted()
                navigation.navigate("session/$id")
            }
        }
        LaunchedEffect(sessionState.deletedSessionId) {
            sessionState.deletedSessionId?.let {
                sessionModel.consumeDeleted()
                navigation.popBackStack("history", inclusive = false)
            }
        }
        Scaffold { padding ->
            NavHost(navigation, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") {
                    Page {
                        Text("Músculo Mental", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                        Text("Sua trajetória começa com você.", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        when {
                            state.loading -> {
                                CircularProgressIndicator()
                                Text("Carregando seu perfil…")
                            }
                            state.loadError -> {
                                Text("Não foi possível carregar seu perfil. Seus dados não foram apagados.", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                                Button(onClick = model::reload) { Text("Tentar novamente") }
                            }
                            else -> {
                                val profile = state.profile
                                if (profile == null) {
                                    Text("Vamos preparar seu perfil", style = MaterialTheme.typography.headlineSmall)
                                    Text("Escolha como deseja usar o aplicativo e registre seu objetivo. Você poderá editar essas informações depois.")
                                } else {
                                    Text("Olá, ${profile.displayName}", style = MaterialTheme.typography.headlineSmall)
                                    Card(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("Seu perfil", style = MaterialTheme.typography.titleLarge)
                                            Text("Modo: ${profile.mode.label}")
                                            Text("Objetivo: ${profile.goal}")
                                            Text("Experiência: ${profile.experience.label}")
                                            Text("Frequência desejada: ${profile.weeklyFrequency} dias por semana")
                                        }
                                    }
                                    if (profile.mode == TrainingMode.STUDENT_DEMO) {
                                        Text("Experiência demonstrativa. Ainda não há professor vinculado nem ficha atribuída.")
                                    } else {
                                        Text("Seu espaço pessoal e seu histórico de treinos permanecem privados neste aparelho.")
                                    }
                                }
                                Button(onClick = { model.beginEdit(); navigation.navigate("profile") }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (profile == null) "Criar meu perfil" else "Editar perfil")
                                }
                                Button(onClick = { navigation.navigate("library") }, modifier = Modifier.fillMaxWidth()) { Text("Explorar biblioteca") }
                                Button(onClick = { navigation.navigate("workouts") }, modifier = Modifier.fillMaxWidth()) { Text("Meus treinos") }
                                Button(onClick = { navigation.navigate("history") }, modifier = Modifier.fillMaxWidth()) { Text("Histórico") }
                                sessionModel.activeSession()?.let { active ->
                                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Sessão em andamento", style = MaterialTheme.typography.titleMedium)
                                        Text(active.planName)
                                        Button(onClick = { navigation.navigate("session/${active.id}") }, modifier = Modifier.fillMaxWidth()) { Text("Retomar treino") }
                                    } }
                                }
                                Text("Protótipo local · Etapa 9", style = MaterialTheme.typography.labelLarge)
                                Text("Seu perfil fica neste aparelho. Esta versão não envia dados nem possui conta online. Desinstalar o aplicativo ou limpar seus dados apaga o perfil.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                composable("profile") {
                    ProfileForm(model, state) { navigation.popBackStack() }
                }
                composable("library") { LibraryScreen(libraryModel, { navigation.popBackStack() }) { navigation.navigate("exercise/$it") } }
                composable("exercise/{id}") { entry ->
                    ExerciseDetailScreen(libraryModel.exercise(entry.arguments?.getString("id").orEmpty()), { navigation.popBackStack() }) { navigation.navigate("muscle/$it") }
                }
                composable("muscle/{id}") { entry ->
                    MuscleDetailScreen(entry.arguments?.getString("id").orEmpty(), libraryModel.allExercises(), { navigation.popBackStack() }) { navigation.navigate("exercise/$it") }
                }
                composable("workouts") {
                    WorkoutPlanListScreen(workoutModel, { navigation.popBackStack() }, {
                        workoutModel.beginCreate(); navigation.navigate("workout-editor")
                    }) { navigation.navigate("workout/$it") }
                }
                composable("workout/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1
                    val plan = workoutState.plans.firstOrNull { it.id == id }
                    WorkoutPlanDetailScreen(plan, sessionModel.activeSession(), { navigation.popBackStack() }, {
                        workoutModel.beginEdit(it); navigation.navigate("workout-editor")
                    }, {
                        workoutModel.duplicate(it); workoutModel.showArchived(false); navigation.popBackStack("workouts", inclusive = false)
                    }, { planId, archived ->
                        workoutModel.setArchived(planId, archived); workoutModel.showArchived(archived); navigation.popBackStack("workouts", inclusive = false)
                    }, sessionModel::start, { navigation.navigate("session/$it") })
                }
                composable("workout-editor") {
                    WorkoutPlanEditorScreen(workoutModel, libraryModel.allExercises(), { navigation.popBackStack() }) { id ->
                        navigation.navigate("workout/$id") { popUpTo("workouts") }
                    }
                }
                composable("session/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1
                    WorkoutSessionScreen(id, sessionModel, {
                        navigation.popBackStack("workouts", inclusive = false)
                    }) { completedId ->
                        navigation.navigate("session-summary/$completedId") { popUpTo("session/$id") { inclusive = true } }
                    }
                }
                composable("session-summary/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1
                    WorkoutSessionSummaryScreen(
                        sessionState.sessions.firstOrNull { it.id == id },
                        sessionState.timers,
                        onCorrect = { navigation.navigate("session-correction/$id") },
                    ) {
                        navigation.popBackStack("workouts", inclusive = false)
                    }
                }
                composable("history") {
                    WorkoutHistoryScreen(
                        sessions = sessionState.sessions,
                        loading = sessionState.loading,
                        error = sessionState.error,
                        onBack = { navigation.popBackStack() },
                        onRetry = sessionModel::reload,
                    ) { navigation.navigate("history-session/$it") }
                }
                composable("history-session/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1
                    WorkoutSessionSummaryScreen(
                        sessionState.sessions.firstOrNull { it.id == id },
                        sessionState.timers,
                        backLabel = "Voltar ao histórico",
                        onCorrect = { navigation.navigate("session-correction/$id") },
                        deleting = sessionState.busy,
                        deleteError = sessionState.operationError,
                        onClearDeleteError = sessionModel::clearError,
                        onDelete = { sessionModel.deleteCompleted(id) },
                    ) { navigation.popBackStack() }
                }
                composable("session-correction/{id}") { entry ->
                    val id = entry.arguments?.getString("id")?.toLongOrNull() ?: -1
                    WorkoutSessionCorrectionScreen(id, sessionModel) { navigation.popBackStack() }
                }
            }
        }
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun ProfileForm(model: ProfileViewModel, state: ProfileState, onBack: () -> Unit) {
    val name by model.name.collectAsStateWithLifecycle()
    val goal by model.goal.collectAsStateWithLifecycle()
    val frequency by model.frequency.collectAsStateWithLifecycle()
    val mode by model.mode.collectAsStateWithLifecycle()
    val experience by model.experience.collectAsStateWithLifecycle()
    BackHandler(enabled = state.saving) { /* Finish the local transaction before leaving. */ }
    Page {
        TextButton(onClick = onBack, enabled = !state.saving) { Text("Voltar sem salvar") }
        Text(if (state.profile == null) "Criar perfil" else "Editar perfil", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text("Conte um pouco sobre você", style = MaterialTheme.typography.titleMedium)
        Text("Use um nome de exibição; não precisa ser seu nome completo.")
        OutlinedTextField(
            value = name, onValueChange = { model.change("name", it) }, label = { Text("Nome de exibição") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.saving,
            isError = state.errors.name != null, supportingText = { Text(state.errors.name ?: "Até 60 caracteres") },
        )
        OutlinedTextField(
            value = goal, onValueChange = { model.change("goal", it) }, label = { Text("Objetivo") },
            modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !state.saving,
            isError = state.errors.goal != null, supportingText = { Text(state.errors.goal ?: "Ex.: criar uma rotina de treino · até 120 caracteres") },
        )
        Text("Como deseja usar o aplicativo?", style = MaterialTheme.typography.titleMedium)
        TrainingMode.entries.forEach { option ->
            FilterChip(selected = mode == option.name, onClick = { model.change("mode", option.name) }, label = { Text(option.label) }, enabled = !state.saving)
        }
        if (mode == TrainingMode.STUDENT_DEMO.name) Text("Este modo é uma demonstração local, sem conexão com um professor.")
        Text("Sua experiência", style = MaterialTheme.typography.titleMedium)
        Experience.entries.forEach { option ->
            FilterChip(selected = experience == option.name, onClick = { model.change("experience", option.name) }, label = { Text(option.label) }, enabled = !state.saving)
        }
        OutlinedTextField(
            value = frequency, onValueChange = { model.change("frequency", it) }, label = { Text("Dias por semana") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.saving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.errors.frequency != null, supportingText = { Text(state.errors.frequency ?: "Sua disponibilidade, de 1 a 7 dias. Não é uma recomendação de treino.") },
        )
        if (state.saveError) Text("Não foi possível salvar. Suas alterações continuam aqui; tente novamente.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        Button(onClick = model::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.saving) "Salvando…" else "Salvar perfil")
        }
    }
}
