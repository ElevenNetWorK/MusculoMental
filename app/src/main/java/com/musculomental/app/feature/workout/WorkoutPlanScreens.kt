package com.musculomental.app.feature.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.WorkoutPlan
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.feature.profile.Page

@Composable
fun WorkoutPlanListScreen(model: WorkoutPlanViewModel, onBack: () -> Unit, onCreate: () -> Unit, onPlan: (Long) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val plans = state.plans.filter { it.status.name == if (state.showingArchived) "ARCHIVED" else "ACTIVE" }
    Page {
        TextButton(onClick = onBack) { Text("Voltar") }
        Text("Meus treinos", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text("Fichas pessoais salvas somente neste aparelho.")
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Criar treino") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!state.showingArchived, { model.showArchived(false) }, { Text("Ativos") })
            FilterChip(state.showingArchived, { model.showArchived(true) }, { Text("Arquivados") })
        }
        when {
            state.loading -> { CircularProgressIndicator(); Text("Carregando treinos…") }
            state.error -> { Text("Não foi possível carregar seus treinos.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }); Button(onClick = model::reload) { Text("Tentar novamente") } }
            plans.isEmpty() -> Text(if (state.showingArchived) "Nenhum treino arquivado." else "Você ainda não criou um treino.")
            else -> plans.forEach { plan ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(plan.name, style = MaterialTheme.typography.titleLarge)
                        Text("Criado por você · ${plan.exercises.size} exercício(s)")
                        Button(onClick = { onPlan(plan.id) }, modifier = Modifier.fillMaxWidth()) { Text("Ver ficha") }
                    }
                }
            }
        }
        if (state.operationError) {
            Text("Não foi possível concluir a alteração. Tente novamente.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            TextButton(onClick = model::consumeOperationError) { Text("Fechar aviso") }
        }
    }
}

@Composable
fun WorkoutPlanDetailScreen(
    plan: WorkoutPlan?, activeSession: WorkoutSession?, onBack: () -> Unit, onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit, onArchive: (Long, Boolean) -> Unit, onStart: (WorkoutPlan) -> Unit, onResume: (Long) -> Unit,
) = Page {
    TextButton(onClick = onBack) { Text("Voltar aos treinos") }
    if (plan == null) { Text("Treino não encontrado", style = MaterialTheme.typography.headlineSmall); return@Page }
    Text(plan.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
    Text("Criado por você · ${plan.status.label}", color = MaterialTheme.colorScheme.primary)
    plan.exercises.sortedBy { it.position }.forEachIndexed { index, item ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${index + 1}. ${item.exerciseName}", style = MaterialTheme.typography.titleLarge)
                Text("Planejado: ${item.sets} séries de ${item.repetitions} repetições")
                Text("Carga sugerida: ${item.suggestedLoadKg?.let { "${formatLoad(it)} kg" } ?: "não informada"}")
                Text("Intervalo planejado: ${item.restSeconds} s")
            }
        }
    }
    if (plan.status.name == "ACTIVE") {
        if (activeSession == null) {
            Button(onClick = { onStart(plan) }, modifier = Modifier.fillMaxWidth()) { Text("Iniciar treino") }
        } else {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Existe uma sessão em andamento: ${activeSession.planName}")
                Button(onClick = { onResume(activeSession.id) }, modifier = Modifier.fillMaxWidth()) { Text("Retomar sessão") }
            } }
        }
        Button(onClick = { onEdit(plan.id) }, modifier = Modifier.fillMaxWidth()) { Text("Editar treino") }
        OutlinedButton(onClick = { onDuplicate(plan.id) }, modifier = Modifier.fillMaxWidth()) { Text("Duplicar treino") }
        TextButton(onClick = { onArchive(plan.id, true) }, modifier = Modifier.fillMaxWidth()) { Text("Arquivar treino") }
    } else {
        Button(onClick = { onArchive(plan.id, false) }, modifier = Modifier.fillMaxWidth()) { Text("Restaurar treino") }
    }
    Text("Esta ficha organiza valores planejados. Cada sessão registra os valores executados separadamente e preserva esta prescrição no histórico.")
}

@Composable
fun WorkoutPlanEditorScreen(
    model: WorkoutPlanViewModel, library: List<Exercise>, onBack: () -> Unit, onSaved: (Long) -> Unit,
) {
    val editor by model.editor.collectAsStateWithLifecycle()
    BackHandler(enabled = editor.saving) { }
    LaunchedEffect(editor.savedId) { editor.savedId?.let { onSaved(it); model.consumeSaved() } }
    Page {
        TextButton(onClick = onBack, enabled = !editor.saving) { Text("Voltar sem salvar") }
        Text(if (editor.id == null) "Criar treino" else "Editar treino", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text("Defina os valores planejados. Eles não são uma recomendação automática do aplicativo.")
        OutlinedTextField(
            editor.name, model::changeName, Modifier.fillMaxWidth(), label = { Text("Nome do treino") }, singleLine = true,
            enabled = !editor.saving, isError = editor.errors.name != null, supportingText = { Text(editor.errors.name ?: "Ex.: Treino A · até 80 caracteres") },
        )
        Text("Exercícios do treino", style = MaterialTheme.typography.titleLarge)
        editor.errors.exercises?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        editor.exercises.forEachIndexed { index, item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${index + 1}. ${item.exerciseName}", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Séries", item.sets, { model.changeExercise(index, "sets", it) }, Modifier.weight(1f), editor.saving)
                        NumberField("Repetições", item.repetitions, { model.changeExercise(index, "repetitions", it) }, Modifier.weight(1f), editor.saving)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Carga (kg)", item.suggestedLoadKg, { model.changeExercise(index, "load", it) }, Modifier.weight(1f), editor.saving, decimal = true)
                        NumberField("Intervalo (s)", item.restSeconds, { model.changeExercise(index, "rest", it) }, Modifier.weight(1f), editor.saving)
                    }
                    editor.errors.exerciseErrors[index]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { model.moveExercise(index, -1) }, enabled = index > 0 && !editor.saving) { Text("Subir") }
                        OutlinedButton(onClick = { model.moveExercise(index, 1) }, enabled = index < editor.exercises.lastIndex && !editor.saving) { Text("Descer") }
                        TextButton(onClick = { model.removeExercise(index) }, enabled = !editor.saving) { Text("Remover") }
                    }
                }
            }
        }
        Text("Adicionar exercício", style = MaterialTheme.typography.titleLarge)
        if (library.isEmpty()) Text("Carregando biblioteca…")
        library.forEach { exercise -> OutlinedButton(onClick = { model.addExercise(exercise) }, enabled = !editor.saving, modifier = Modifier.fillMaxWidth()) { Text("Adicionar ${exercise.name}") } }
        if (editor.saveError) Text("Não foi possível salvar. O formulário foi mantido.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        Button(onClick = model::save, enabled = !editor.saving, modifier = Modifier.fillMaxWidth()) { Text(if (editor.saving) "Salvando…" else "Salvar treino") }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier, disabled: Boolean, decimal: Boolean = false) {
    OutlinedTextField(
        value, onChange, modifier, label = { Text(label) }, singleLine = true, enabled = !disabled,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}

private fun formatLoad(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
