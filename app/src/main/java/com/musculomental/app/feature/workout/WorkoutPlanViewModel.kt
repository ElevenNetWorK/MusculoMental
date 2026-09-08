package com.musculomental.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.PlannedExerciseDraft
import com.musculomental.app.domain.WorkoutPlan
import com.musculomental.app.domain.WorkoutPlanErrors
import com.musculomental.app.domain.WorkoutPlanRepository
import com.musculomental.app.domain.WorkoutPlanStatus
import com.musculomental.app.domain.validateWorkoutPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutPlansState(
    val loading: Boolean = true,
    val plans: List<WorkoutPlan> = emptyList(),
    val showingArchived: Boolean = false,
    val error: Boolean = false,
    val operationError: Boolean = false,
)

data class WorkoutEditorState(
    val id: Long? = null,
    val name: String = "",
    val exercises: List<PlannedExerciseDraft> = emptyList(),
    val errors: WorkoutPlanErrors = WorkoutPlanErrors(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val saveError: Boolean = false,
)

class WorkoutPlanViewModel(private val repository: WorkoutPlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutPlansState())
    val state: StateFlow<WorkoutPlansState> = _state.asStateFlow()
    private val _editor = MutableStateFlow(WorkoutEditorState())
    val editor: StateFlow<WorkoutEditorState> = _editor.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            try {
                repository.observePlans().collect { plans -> _state.update { it.copy(loading = false, plans = plans, error = false) } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun showArchived(value: Boolean) = _state.update { it.copy(showingArchived = value) }
    fun visiblePlans(): List<WorkoutPlan> = state.value.plans.filter {
        it.status == if (state.value.showingArchived) WorkoutPlanStatus.ARCHIVED else WorkoutPlanStatus.ACTIVE
    }
    fun plan(id: Long): WorkoutPlan? = state.value.plans.firstOrNull { it.id == id }

    fun beginCreate() { _editor.value = WorkoutEditorState() }
    fun beginEdit(id: Long) {
        val plan = plan(id) ?: return
        _editor.value = WorkoutEditorState(
            id = id,
            name = plan.name,
            exercises = plan.exercises.sortedBy { it.position }.map {
                PlannedExerciseDraft(it.exerciseId, it.exerciseName, it.sets.toString(), it.repetitions.toString(), it.suggestedLoadKg?.let(::formatLoad).orEmpty(), it.restSeconds.toString())
            },
        )
    }

    fun changeName(value: String) = _editor.update { it.copy(name = value, errors = it.errors.copy(name = null), saveError = false) }
    fun addExercise(exercise: Exercise) = _editor.update {
        it.copy(exercises = it.exercises + PlannedExerciseDraft(exercise.id, exercise.name), errors = it.errors.copy(exercises = null), saveError = false)
    }
    fun removeExercise(index: Int) = _editor.update { it.copy(exercises = it.exercises.filterIndexed { i, _ -> i != index }, saveError = false) }
    fun moveExercise(index: Int, direction: Int) {
        val target = index + direction
        _editor.update { current ->
            if (index !in current.exercises.indices || target !in current.exercises.indices) current
            else current.copy(exercises = current.exercises.toMutableList().apply { add(target, removeAt(index)) }, saveError = false)
        }
    }
    fun changeExercise(index: Int, field: String, value: String) = _editor.update { current ->
        current.copy(
            exercises = current.exercises.mapIndexed { i, item ->
                if (i != index) item else when (field) {
                    "sets" -> item.copy(sets = value)
                    "repetitions" -> item.copy(repetitions = value)
                    "load" -> item.copy(suggestedLoadKg = value)
                    "rest" -> item.copy(restSeconds = value)
                    else -> item
                }
            },
            errors = current.errors.copy(exerciseErrors = current.errors.exerciseErrors - index),
            saveError = false,
        )
    }

    fun save() {
        val current = editor.value
        val validation = validateWorkoutPlan(current.name, current.exercises)
        if (!validation.isValid) { _editor.update { it.copy(errors = validation) }; return }
        viewModelScope.launch {
            _editor.update { it.copy(saving = true, saveError = false) }
            try {
                val id = repository.save(current.id, current.name, current.exercises)
                _editor.update { it.copy(saving = false, savedId = id) }
            } catch (_: Exception) {
                _editor.update { it.copy(saving = false, saveError = true) }
            }
        }
    }

    fun consumeSaved() = _editor.update { it.copy(savedId = null) }
    fun duplicate(id: Long) = operation { repository.duplicate(id) }
    fun setArchived(id: Long, archived: Boolean) = operation { repository.setArchived(id, archived) }
    fun consumeOperationError() = _state.update { it.copy(operationError = false) }

    private fun operation(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (_: Exception) { _state.update { it.copy(operationError = true) } }
        }
    }

    private fun formatLoad(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
