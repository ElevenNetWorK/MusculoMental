package com.musculomental.app.feature.session

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.SessionStatus
import com.musculomental.app.domain.RestInterval
import com.musculomental.app.domain.TimerAlertMode
import com.musculomental.app.domain.TimerCountMode
import com.musculomental.app.domain.TimerStartMode
import com.musculomental.app.domain.TimerState
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.feature.profile.Page
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

@Composable
fun WorkoutSessionScreen(
    sessionId: Long, model: WorkoutSessionViewModel, onClose: () -> Unit, onCompleted: (Long) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val session = state.sessions.firstOrNull { it.id == sessionId }
    BackHandler(enabled = session != null && session.status in listOf(SessionStatus.IN_PROGRESS, SessionStatus.PAUSED)) {
        if (!state.busy) model.pause(sessionId)
    }
    LaunchedEffect(session?.id) { session?.let { model.open(it.id) } }
    LaunchedEffect(state.closed) { if (state.closed) { model.consumeClosed(); onClose() } }
    LaunchedEffect(state.completedId) { state.completedId?.let { model.consumeCompleted(); onCompleted(it) } }
    LaunchedEffect(state.alertSetId) {
        if (state.alertSetId != null) {
            when (state.timerPreferences.alertMode) {
                TimerAlertMode.SILENT -> Unit
                TimerAlertMode.VIBRATION -> context.vibrateForTimer()
                TimerAlertMode.SOUND -> {
                    val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 700)
                    delay(800)
                    tone.release()
                }
            }
            model.consumeTimerAlert()
        }
    }
    Page {
        if (session == null) {
            Text(
                when {
                    state.loading -> "Carregando sessão…"
                    state.error -> "Não foi possível carregar a sessão. Seus registros não foram apagados."
                    else -> "Sessão não encontrada"
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = if (state.error) Modifier.semantics { liveRegion = LiveRegionMode.Assertive } else Modifier,
            )
            if (state.error) Button(onClick = model::reload, modifier = Modifier.fillMaxWidth()) { Text("Tentar novamente") }
            TextButton(onClick = onClose) { Text("Voltar") }
            return@Page
        }
        if (session.status == SessionStatus.COMPLETED || session.status == SessionStatus.CANCELLED) {
            Text("Esta sessão já foi encerrada.")
            Button(onClick = { onCompleted(session.id) }) { Text("Ver resumo") }
            return@Page
        }
        val position = session.currentExercisePosition.coerceIn(0, session.exercises.lastIndex)
        val exercise = session.exercises[position]
        Text(session.planName, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        val registeredSets = session.completedSets + session.skippedSets
        Text(
            "$registeredSets de ${session.totalSets} séries registradas",
            modifier = Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(registeredSets.toFloat(), 0f..session.totalSets.toFloat()) },
        )
        state.timers.firstOrNull { it.state == TimerState.RUNNING || it.state == TimerState.PAUSED }?.let { timer ->
            val owner = session.exercises.flatMap { exercise -> exercise.sets.map { Triple(it.id, exercise.exerciseName, it.setNumber) } }.firstOrNull { it.first == timer.performedSetId }
            ActiveTimerCard(timer, owner?.second ?: "Série", owner?.third, state.now, state.busy, model)
        }
        Text("Exercício ${position + 1} de ${session.exercises.size}", color = MaterialTheme.colorScheme.primary)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(exercise.exerciseName, style = MaterialTheme.typography.headlineSmall)
                Text("Planejado: ${exercise.plannedSets} séries · ${exercise.plannedRepetitions} repetições · ${exercise.plannedLoadKg?.let { "${formatLoad(it)} kg" } ?: "carga livre"} · ${exercise.plannedRestSeconds} s")
            }
        }
        exercise.sets.forEach { set ->
            val draft = state.drafts[set.id] ?: SetDraft(exercise.plannedRepetitions.toString(), exercise.plannedLoadKg?.let(::formatLoad).orEmpty())
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Série ${set.setNumber} · ${set.status.label}", style = MaterialTheme.typography.titleMedium)
                    Text("Planejado: ${exercise.plannedRepetitions} repetições${exercise.plannedLoadKg?.let { " com ${formatLoad(it)} kg" } ?: ""}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            draft.repetitions, { model.changeSet(set.id, "repetitions", it) }, Modifier.weight(1f),
                            label = { Text("Executado: reps") }, singleLine = true, enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = draft.errors.repetitions != null,
                        )
                        OutlinedTextField(
                            draft.load, { model.changeSet(set.id, "load", it) }, Modifier.weight(1f),
                            label = { Text("Executado: kg") }, singleLine = true, enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = draft.errors.load != null,
                        )
                    }
                    draft.errors.repetitions?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    draft.errors.load?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (set.setNumber > 1) {
                        TextButton(onClick = { model.copyPrevious(session.id, exercise.id, set.setNumber) }, enabled = !state.busy) { Text("Copiar série anterior") }
                    }
                    Button(onClick = { model.saveSet(set.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (set.status == PerformedSetStatus.COMPLETED) "Atualizar série" else "Concluir série")
                    }
                    TextButton(onClick = { model.skipSet(set.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (set.status == PerformedSetStatus.SKIPPED) "Manter ignorada" else "Ignorar série")
                    }
                    if (set.status == PerformedSetStatus.COMPLETED && state.timerPreferences.enabled) {
                        val timer = state.timers.firstOrNull { it.performedSetId == set.id }
                        if (timer == null || timer.state == TimerState.FINISHED) {
                            OutlinedButton(onClick = { model.startRest(set.id, exercise.plannedRestSeconds) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (timer == null) "Iniciar intervalo" else "Reiniciar intervalo") }
                        }
                        if (timer?.state == TimerState.FINISHED) FinishedTimerRecord(timer, state.correctionInputs[timer.performedSetId].orEmpty(), state.busy, model)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { model.move(session.id, position - 1) }, enabled = position > 0 && !state.busy, modifier = Modifier.weight(1f)) { Text("Anterior") }
            Button(onClick = { model.move(session.id, position + 1) }, enabled = position < session.exercises.lastIndex && !state.busy, modifier = Modifier.weight(1f)) { Text("Próximo") }
        }
        Text("Como foi o treino?", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(state.note, model::changeNote, Modifier.fillMaxWidth(), label = { Text("Observação opcional") }, minLines = 2, enabled = !state.busy, supportingText = { Text("Até 500 caracteres") })
        Rating("Esforço opcional", state.effort, model::changeEffort, state.busy)
        Rating("Desconforto opcional", state.discomfort, model::changeDiscomfort, state.busy)
        TimerPreferencesCard(state, model)
        state.feedbackError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
        state.operationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            TextButton(onClick = model::clearError) { Text("Fechar aviso") }
        }
        Button(onClick = { model.complete(session.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Finalizar treino") }
        OutlinedButton(onClick = { model.pause(session.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Pausar e sair") }
        CancelSessionButton(state.busy) { model.cancel(session.id) }
        Text("Dor forte, súbita ou persistente exige interrupção do exercício e avaliação profissional.")
    }
}

@Composable
private fun ActiveTimerCard(timer: RestInterval, exerciseName: String, setNumber: Int?, now: Long, busy: Boolean, model: WorkoutSessionViewModel) {
    val elapsed = timer.elapsedAt(now)
    val remaining = timer.remainingAt(now)
    val display = when {
        timer.countMode == TimerCountMode.COUNT_UP -> formatTime(elapsed)
        remaining >= 0 -> formatTime(remaining)
        else -> "+${formatTime(-remaining)}"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Intervalo · $exerciseName${setNumber?.let { " · série $it" }.orEmpty()}", style = MaterialTheme.typography.titleMedium)
            Text(display, style = MaterialTheme.typography.headlineLarge)
            Text("Planejado: ${timer.plannedSeconds} s · alvo atual: ${timer.targetSeconds} s")
            if (remaining < 0 && timer.countMode == TimerCountMode.COUNTDOWN) Text("Tempo planejado atingido", color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (timer.state == TimerState.RUNNING) OutlinedButton(onClick = { model.pauseRest(timer.performedSetId) }, enabled = !busy) { Text("Pausar") }
                if (timer.state == TimerState.PAUSED) Button(onClick = { model.resumeRest(timer.performedSetId) }, enabled = !busy) { Text("Continuar") }
                OutlinedButton(onClick = { model.adjustRest(timer.performedSetId, -15) }, enabled = !busy) { Text("−15 s") }
                OutlinedButton(onClick = { model.adjustRest(timer.performedSetId, 15) }, enabled = !busy) { Text("+15 s") }
            }
            Button(onClick = { model.finishRest(timer.performedSetId) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Encerrar intervalo") }
        }
    }
}

@Composable
private fun FinishedTimerRecord(timer: RestInterval, correction: String, busy: Boolean, model: WorkoutSessionViewModel) {
    Text("Intervalo realizado: ${timer.actualSeconds?.let { "$it s" } ?: "não registrado"}${if (timer.approximate) " · aproximado" else ""}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(correction, { model.changeCorrection(timer.performedSetId, it) }, Modifier.weight(1f), label = { Text("Corrigir segundos") }, singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Button(onClick = { model.correctRest(timer.performedSetId) }, enabled = !busy) { Text("Salvar") }
    }
    TextButton(onClick = { model.deleteRest(timer.performedSetId) }, enabled = !busy) { Text("Excluir registro do intervalo") }
}

@Composable
private fun TimerPreferencesCard(state: WorkoutSessionState, model: WorkoutSessionViewModel) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cronômetro de descanso", style = MaterialTheme.typography.titleLarge)
            FilterChip(state.timerPreferences.enabled, { model.updateTimerPreference("enabled", (!state.timerPreferences.enabled).toString()) }, { Text(if (state.timerPreferences.enabled) "Ativado" else "Desativado") }, enabled = !state.busy)
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar preferências" else "Configurar cronômetro") }
            if (expanded) {
                Text("Início", style = MaterialTheme.typography.titleMedium)
                TimerStartMode.entries.forEach { option -> FilterChip(state.timerPreferences.startMode == option, { model.updateTimerPreference("startMode", option.name) }, { Text(option.label) }, enabled = !state.busy) }
                Text("Contagem", style = MaterialTheme.typography.titleMedium)
                TimerCountMode.entries.forEach { option -> FilterChip(state.timerPreferences.countMode == option, { model.updateTimerPreference("countMode", option.name) }, { Text(option.label) }, enabled = !state.busy) }
                Text("Aviso", style = MaterialTheme.typography.titleMedium)
                TimerAlertMode.entries.forEach { option -> FilterChip(state.timerPreferences.alertMode == option, { model.updateTimerPreference("alertMode", option.name) }, { Text(option.label) }, enabled = !state.busy) }
                FilterChip(state.timerPreferences.finishOnNextSet, { model.updateTimerPreference("finishOnNextSet", (!state.timerPreferences.finishOnNextSet).toString()) }, { Text("Encerrar ao iniciar próxima série") }, enabled = !state.busy)
                FilterChip(state.timerPreferences.recordActual, { model.updateTimerPreference("recordActual", (!state.timerPreferences.recordActual).toString()) }, { Text("Registrar intervalo realizado") }, enabled = !state.busy)
                OutlinedTextField(state.timerDefaultInput, { model.updateTimerPreference("defaultSeconds", it) }, Modifier.fillMaxWidth(), label = { Text("Tempo padrão livre (segundos)") }, singleLine = true, enabled = !state.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                state.timerPreferenceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun Rating(label: String, selected: Int?, onSelect: (Int?) -> Unit, disabled: Boolean) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..10).forEach { value -> FilterChip(selected == value, { onSelect(value) }, { Text(value.toString()) }, enabled = !disabled) }
    }
    Text("Toque novamente no valor selecionado para limpar.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CancelSessionButton(disabled: Boolean, onCancel: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    if (!confirming) TextButton(onClick = { confirming = true }, enabled = !disabled, modifier = Modifier.fillMaxWidth()) { Text("Cancelar sessão") }
    else Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cancelar esta sessão? Os registros feitos nela deixarão de ser uma sessão ativa.")
            Button(onClick = onCancel, enabled = !disabled, modifier = Modifier.fillMaxWidth()) { Text("Confirmar cancelamento") }
            TextButton(onClick = { confirming = false }, enabled = !disabled, modifier = Modifier.fillMaxWidth()) { Text("Continuar treinando") }
        }
    }
}

@Composable
fun WorkoutSessionSummaryScreen(
    session: WorkoutSession?,
    timers: List<RestInterval>,
    backLabel: String = "Voltar aos treinos",
    onCorrect: (() -> Unit)? = null,
    deleting: Boolean = false,
    deleteError: String? = null,
    onClearDeleteError: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    BackHandler(enabled = deleting) { /* Finish the local deletion before leaving. */ }
    Page {
    TextButton(onClick = onBack, enabled = !deleting) { Text(backLabel) }
    if (session == null) { Text("Resumo não encontrado", style = MaterialTheme.typography.headlineSmall); return@Page }
    Text("Treino concluído", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
    Text(session.planName, style = MaterialTheme.typography.titleLarge)
    Text("Início: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(session.startedAt))}")
    session.endedAt?.let { endedAt ->
        Text("Fim: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(endedAt))}")
        val durationMinutes = ((endedAt - session.startedAt).coerceAtLeast(0L) / 60_000L)
        Text("Duração: $durationMinutes min")
    }
    Text("Séries concluídas: ${session.completedSets}")
    Text("Séries ignoradas: ${session.skippedSets}")
    Text("Volume estimado: ${formatLoad(session.estimatedVolumeKg)} kg")
    session.effort?.let { Text("Esforço informado: $it/10") }
    session.discomfort?.let { Text("Desconforto informado: $it/10") }
    if (session.note.isNotBlank()) Text("Observação: ${session.note}")
    session.exercises.forEach { exercise ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text("Planejado: ${exercise.plannedSets} séries · ${exercise.plannedRepetitions} repetições · ${exercise.plannedLoadKg?.let { "${formatLoad(it)} kg" } ?: "sem carga"} · ${exercise.plannedRestSeconds} s")
                exercise.sets.forEach { set ->
                    Text("Série ${set.setNumber} executada: ${if (set.status == PerformedSetStatus.COMPLETED) "${set.repetitions} reps · ${set.loadKg?.let { "${formatLoad(it)} kg" } ?: "sem carga"}" else set.status.label}")
                    timers.firstOrNull { it.performedSetId == set.id && it.state == TimerState.FINISHED }?.let { timer ->
                        Text("Intervalo: ${timer.actualSeconds?.let { "$it s" } ?: "não registrado"}${if (timer.approximate) " · aproximado" else ""}")
                    }
                }
            }
        }
    }
    onCorrect?.let { correction ->
        OutlinedButton(onClick = correction, enabled = !deleting, modifier = Modifier.fillMaxWidth()) { Text("Corrigir registro") }
    }
    onDelete?.let { deletion ->
        DeleteCompletedSessionButton(deleting, deleteError, onClearDeleteError, deletion)
    }
    Text("O volume é uma estimativa baseada apenas nas séries com carga registrada.")
    }
}

@Composable
private fun DeleteCompletedSessionButton(
    deleting: Boolean,
    error: String?,
    onClearError: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    if (!confirming) {
        TextButton(onClick = { confirming = true; onClearError?.invoke() }, enabled = !deleting, modifier = Modifier.fillMaxWidth()) {
            Text("Excluir sessão do histórico")
        }
    } else {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Excluir esta sessão permanentemente? As séries e os intervalos registrados também serão apagados. A ficha de treino será preservada.")
                Button(onClick = onDelete, enabled = !deleting, modifier = Modifier.fillMaxWidth()) {
                    Text(if (deleting) "Excluindo…" else "Confirmar exclusão")
                }
                TextButton(onClick = { confirming = false; onClearError?.invoke() }, enabled = !deleting, modifier = Modifier.fillMaxWidth()) {
                    Text("Manter sessão")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
            }
        }
    }
}

@Composable
fun WorkoutSessionCorrectionScreen(sessionId: Long, model: WorkoutSessionViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val session = state.sessions.firstOrNull { it.id == sessionId && it.status == SessionStatus.COMPLETED }
    LaunchedEffect(session?.id) { session?.let { model.openCorrection(it.id) } }
    Page {
        TextButton(onClick = onBack, enabled = !state.busy) { Text("Voltar ao resumo") }
        if (session == null) {
            Text(if (state.loading) "Carregando registro…" else "Registro concluído não encontrado", style = MaterialTheme.typography.headlineSmall)
            return@Page
        }
        Text("Corrigir registro", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text(session.planName, style = MaterialTheme.typography.titleLarge)
        Text("A prescrição e a data da sessão serão preservadas. Corrija somente o que foi executado.")
        session.exercises.forEach { exercise ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleLarge)
                    Text("Planejado: ${exercise.plannedSets} séries · ${exercise.plannedRepetitions} repetições · ${exercise.plannedLoadKg?.let { "${formatLoad(it)} kg" } ?: "sem carga"}")
                    exercise.sets.forEach { set ->
                        val draft = state.drafts[set.id] ?: SetDraft(
                            set.repetitions?.toString() ?: exercise.plannedRepetitions.toString(),
                            set.loadKg?.let(::formatLoad) ?: exercise.plannedLoadKg?.let(::formatLoad).orEmpty(),
                        )
                        Text("Série ${set.setNumber} · ${set.status.label}", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                draft.repetitions,
                                { model.changeSet(set.id, "repetitions", it) },
                                Modifier.weight(1f),
                                label = { Text("Executado: reps") },
                                singleLine = true,
                                enabled = !state.busy,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = draft.errors.repetitions != null,
                            )
                            OutlinedTextField(
                                draft.load,
                                { model.changeSet(set.id, "load", it) },
                                Modifier.weight(1f),
                                label = { Text("Executado: kg") },
                                singleLine = true,
                                enabled = !state.busy,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = draft.errors.load != null,
                            )
                        }
                        draft.errors.repetitions?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        draft.errors.load?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(onClick = { model.correctSet(set.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Salvar correção da série") }
                        TextButton(onClick = { model.correctAsSkipped(set.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Marcar série como ignorada") }
                        if (state.correctedSetId == set.id) state.correctionMessage?.let { CorrectionConfirmation(it, model::clearCorrectionMessage) }
                    }
                }
            }
        }
        Text("Corrigir avaliação", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(state.note, model::changeNote, Modifier.fillMaxWidth(), label = { Text("Observação opcional") }, minLines = 2, enabled = !state.busy, supportingText = { Text("Até 500 caracteres") })
        Rating("Esforço opcional", state.effort, model::changeEffort, state.busy)
        Rating("Desconforto opcional", state.discomfort, model::changeDiscomfort, state.busy)
        state.feedbackError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
        Button(onClick = { model.saveFeedbackCorrection(session.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Salvar avaliação") }
        if (state.correctedSetId == null) state.correctionMessage?.let { CorrectionConfirmation(it, model::clearCorrectionMessage) }
        state.operationError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
        Text("Correções atualizam as métricas locais. O aplicativo não recomenda cargas nem altera a ficha planejada.")
    }
}

@Composable
private fun CorrectionConfirmation(message: String, onClose: () -> Unit) {
    Card(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(message, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Fechar") }
        }
    }
}

private fun formatLoad(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
private fun formatTime(totalSeconds: Int): String = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
private fun Context.vibrateForTimer() {
    val vibrator = getSystemService(Vibrator::class.java) ?: return
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250), -1))
}
