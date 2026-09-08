package com.musculomental.app.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musculomental.app.domain.WorkoutHistorySummary
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.domain.buildWorkoutHistory
import com.musculomental.app.domain.calendarSessions
import com.musculomental.app.domain.exerciseProgress
import com.musculomental.app.feature.profile.Page
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.roundToInt

private enum class HistoryTab(val label: String) { SUMMARY("Resumo"), CALENDAR("Calendário"), PROGRESS("Evolução"), RECORDS("Recordes") }

@Composable
fun WorkoutHistoryScreen(
    sessions: List<WorkoutSession>,
    loading: Boolean,
    error: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSession: (Long) -> Unit,
) = Page {
    TextButton(onClick = onBack) { Text("Voltar") }
    Text("Histórico", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
    Text("Seus registros e sua evolução ficam somente neste aparelho.")
    when {
        loading -> { CircularProgressIndicator(); Text("Carregando histórico…") }
        error -> {
            Text("Não foi possível carregar o histórico. Seus registros não foram apagados.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
        else -> {
            val history = buildWorkoutHistory(sessions)
            if (history.completedSessions.isEmpty()) {
                Text("Nenhum treino concluído ainda", style = MaterialTheme.typography.headlineSmall)
                Text("Ao finalizar uma sessão, ela aparecerá aqui com a prescrição e os valores executados.")
            } else {
                var selectedTab by rememberSaveable { mutableStateOf(HistoryTab.SUMMARY.name) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HistoryTab.entries.forEach { tab ->
                        FilterChip(selected = selectedTab == tab.name, onClick = { selectedTab = tab.name }, label = { Text(tab.label) })
                    }
                }
                when (HistoryTab.valueOf(selectedTab)) {
                    HistoryTab.SUMMARY -> HistorySummary(history, onSession)
                    HistoryTab.CALENDAR -> HistoryCalendar(history.completedSessions, onSession)
                    HistoryTab.PROGRESS -> HistoryProgress(history.completedSessions)
                    HistoryTab.RECORDS -> HistoryRecords(history)
                }
            }
        }
    }
}

@Composable
private fun HistorySummary(history: WorkoutHistorySummary, onSession: (Long) -> Unit) {
    Text("Frequência", style = MaterialTheme.typography.headlineSmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("Esta semana", history.sessionsThisWeek.toString(), Modifier.weight(1f))
        MetricCard("Este mês", history.sessionsThisMonth.toString(), Modifier.weight(1f))
        MetricCard("Concluídos", history.completedSessions.size.toString(), Modifier.weight(1f))
    }
    Text("${history.sessionsThisWeek} ${if (history.sessionsThisWeek == 1) "treino" else "treinos"} esta semana")
    Text("Tempo total: ${formatDuration(history.totalDurationSeconds)} · Volume estimado: ${formatLoad(history.totalVolumeKg)} kg")
    Text("Sessões concluídas", style = MaterialTheme.typography.headlineSmall)
    history.completedSessions.forEach { session -> SessionCard(session, onSession) }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) = Card(modifier) {
    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SessionCard(session: WorkoutSession, onSession: (Long) -> Unit) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(session.planName, style = MaterialTheme.typography.titleMedium)
        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(requireNotNull(session.endedAt))))
        Text("${session.completedSets} séries concluídas · ${session.exercises.size} exercícios")
        Text("${formatDuration(((session.endedAt - session.startedAt) / 1_000L).coerceAtLeast(0L))} · ${formatLoad(session.estimatedVolumeKg)} kg estimados")
        OutlinedButton(onClick = { onSession(session.id) }, modifier = Modifier.fillMaxWidth()) { Text("Ver detalhes") }
    }
}

@Composable
private fun HistoryCalendar(sessions: List<WorkoutSession>, onSession: (Long) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val grouped = calendarSessions(sessions)
    val months = grouped.keys.map(YearMonth::from).distinct().sortedDescending()
    var selectedMonthText by rememberSaveable { mutableStateOf(months.first().toString()) }
    var selectedDateText by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMonth = YearMonth.parse(selectedMonthText).takeIf { it in months } ?: months.first()
    Text("Calendário de sessões", style = MaterialTheme.typography.headlineSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        months.forEach { month ->
            FilterChip(
                selected = selectedMonth == month,
                onClick = { selectedMonthText = month.toString(); selectedDateText = null },
                label = { Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy", locale))) },
            )
        }
    }
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(day.getDisplayName(TextStyle.SHORT, locale).take(3), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
        }
    }
    val first = selectedMonth.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val cells = List(offset) { null } + (1..selectedMonth.lengthOfMonth()).map(selectedMonth::atDay)
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            (week + List(7 - week.size) { null }).forEach { date ->
                val count = date?.let { grouped[it]?.size } ?: 0
                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (date != null) {
                        TextButton(onClick = { if (count > 0) selectedDateText = date.toString() }, enabled = count > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(date.dayOfMonth.toString(), fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal)
                                if (count > 0) Text("•$count", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
    selectedDateText?.let(LocalDate::parse)?.let { date ->
        Text(date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", locale)), style = MaterialTheme.typography.titleMedium)
        grouped[date].orEmpty().forEach { SessionCard(it, onSession) }
    }
}

@Composable
private fun HistoryProgress(sessions: List<WorkoutSession>) {
    val exercises = sessions.flatMap { it.exercises }.distinctBy { it.exerciseId }.sortedBy { it.exerciseName }
    var selectedId by rememberSaveable { mutableStateOf(exercises.first().exerciseId) }
    if (exercises.none { it.exerciseId == selectedId }) selectedId = exercises.first().exerciseId
    val selected = exercises.first { it.exerciseId == selectedId }
    val points = exerciseProgress(sessions, selectedId)
    Text("Evolução de carga", style = MaterialTheme.typography.headlineSmall)
    Text("Maior carga registrada em cada sessão concluída.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        exercises.forEach { exercise ->
            FilterChip(selected = selectedId == exercise.exerciseId, onClick = { selectedId = exercise.exerciseId }, label = { Text(exercise.exerciseName) })
        }
    }
    if (points.isEmpty()) Text("Ainda não há carga registrada para ${selected.exerciseName}.")
    else {
        ProgressChart(points.map { it.maximumLoadKg }, selected.exerciseName)
        points.asReversed().forEach { point ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(point.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    Text("${formatLoad(point.maximumLoadKg)} kg", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProgressChart(values: List<Double>, exerciseName: String) {
    val color = MaterialTheme.colorScheme.primary
    val label = "Evolução de $exerciseName: ${values.joinToString { "${formatLoad(it)} kg" }}"
    Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = label }) {
        val minimum = values.minOrNull() ?: 0.0
        val maximum = values.maxOrNull() ?: minimum
        val range = (maximum - minimum).takeIf { it > 0.0 } ?: 1.0
        val left = 16.dp.toPx()
        val right = size.width - 16.dp.toPx()
        val top = 16.dp.toPx()
        val bottom = size.height - 16.dp.toPx()
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) size.width / 2 else left + (right - left) * index / (values.size - 1)
            val y = bottom - ((value - minimum) / range).toFloat() * (bottom - top)
            androidx.compose.ui.geometry.Offset(x, y)
        }
        points.zipWithNext().forEach { (start, end) -> drawLine(color, start, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round) }
        points.forEach { drawCircle(color, 6.dp.toPx(), it) }
    }
}

@Composable
private fun HistoryRecords(history: WorkoutHistorySummary) {
    Text("Recordes pessoais", style = MaterialTheme.typography.headlineSmall)
    Text("Melhores valores registrados em séries concluídas. Servem para acompanhamento e não são recomendação de carga.")
    if (history.records.isEmpty()) Text("Ainda não há séries concluídas para calcular recordes.")
    history.records.forEach { record ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(record.exerciseName, style = MaterialTheme.typography.titleMedium)
                record.maximumLoadKg?.let { Text("Maior carga: ${formatLoad(it)} kg") }
                record.maximumRepetitions?.let { Text("Mais repetições em uma série: $it") }
                record.bestSetVolumeKg?.let { Text("Maior volume em uma série: ${formatLoad(it)} kg") }
            }
        }
    }
}

private fun formatLoad(value: Double): String = if (value % 1.0 == 0.0) value.roundToInt().toString() else "%.1f".format(value)
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes} min"
}
