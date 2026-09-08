package com.musculomental.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musculomental.app.domain.Equipment
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.MuscleRole
import com.musculomental.app.feature.profile.Page

@Composable
fun LibraryScreen(model: LibraryViewModel, onBack: () -> Unit, onExercise: (String) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val filters by model.activeFilters.collectAsStateWithLifecycle()
    Page {
        TextButton(onClick = onBack) { Text("Voltar") }
        Text("Biblioteca", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text("Catálogo demonstrativo · conteúdo em revisão", color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(filters.query, model::search, Modifier.fillMaxWidth(), label = { Text("Buscar exercício ou músculo") }, singleLine = true)
        Text("Região", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Peito", "Ombros", "Braços", "Antebraços", "Pernas", "Glúteos").forEach { region ->
                FilterChip(filters.region == region, { model.filterRegion(region) }, { Text(region) })
            }
        }
        Text("Equipamento", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Equipment.entries.forEach { equipment ->
                FilterChip(filters.equipment == equipment, { model.filterEquipment(equipment) }, { Text(equipment.label) })
            }
        }
        if (filters.query.isNotBlank() || filters.region != null || filters.equipment != null) {
            TextButton(onClick = model::clearFilters) { Text("Limpar filtros") }
        }
        when {
            state.loading -> { CircularProgressIndicator(); Text("Carregando biblioteca…") }
            state.error -> {
                Text("Não foi possível carregar a biblioteca. Tente novamente.", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                Button(onClick = model::reload, modifier = Modifier.fillMaxWidth()) { Text("Tentar novamente") }
            }
            state.exercises.isEmpty() -> Text("Nenhum exercício encontrado. Tente remover um filtro.")
            else -> state.exercises.forEach { exercise -> ExerciseCard(exercise) { onExercise(exercise.id) } }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleLarge)
            Text(exercise.equipment.label)
            Text("Principal: " + exercise.muscles.filter { it.role == MuscleRole.PRIMARY }.joinToString { it.muscle.commonName })
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Ver exercício") }
        }
    }
}

@Composable
fun ExerciseDetailScreen(exercise: Exercise?, onBack: () -> Unit, onMuscle: (String) -> Unit) = Page {
    TextButton(onClick = onBack) { Text("Voltar à biblioteca") }
    if (exercise == null) {
        Text("Exercício não encontrado", style = MaterialTheme.typography.headlineSmall)
        return@Page
    }
    Text(exercise.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
    Text(exercise.contentStatus, color = MaterialTheme.colorScheme.primary)
    Text("Equipamento: ${exercise.equipment.label}")
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text("Mídia demonstrativa pendente", style = MaterialTheme.typography.titleMedium)
        Text("A origem e os direitos de uso das imagens e vídeos ainda precisam ser definidos.")
    } }
    DetailList("Como executar", exercise.instructions)
    DetailList("Erros comuns", exercise.commonErrors)
    Text("Músculos envolvidos", style = MaterialTheme.typography.titleLarge)
    exercise.muscles.forEach { relation ->
        TextButton(onClick = { onMuscle(relation.muscle.id) }) {
            Text("${relation.muscle.commonName} · ${if (relation.role == MuscleRole.PRIMARY) "principal" else "auxiliar"}")
        }
    }
    HorizontalDivider()
    Text("Conteúdo educativo. Ajuste amplitude e execução às suas condições e procure um profissional diante de dor ou limitação.")
}

@Composable
fun MuscleDetailScreen(muscleId: String, exercises: List<Exercise>, onBack: () -> Unit, onExercise: (String) -> Unit) = Page {
    val muscle = exercises.flatMap { it.muscles }.map { it.muscle }.firstOrNull { it.id == muscleId }
    TextButton(onClick = onBack) { Text("Voltar") }
    if (muscle == null) {
        Text("Músculo não encontrado", style = MaterialTheme.typography.headlineSmall)
        return@Page
    }
    Text(muscle.commonName, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
    Text(muscle.anatomicalName, style = MaterialTheme.typography.titleMedium)
    Text("Região: ${muscle.region}")
    Text(muscle.function)
    Text("Exercícios relacionados", style = MaterialTheme.typography.titleLarge)
    exercises.filter { exercise -> exercise.muscles.any { it.muscle.id == muscleId } }.forEach { exercise ->
        Button(onClick = { onExercise(exercise.id) }, modifier = Modifier.fillMaxWidth()) { Text(exercise.name) }
    }
    Text("Conteúdo demonstrativo em revisão. Informações gerais não substituem avaliação profissional.")
}

@Composable
private fun DetailList(title: String, items: List<String>) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    items.forEachIndexed { index, item -> Text("${index + 1}. $item") }
}
