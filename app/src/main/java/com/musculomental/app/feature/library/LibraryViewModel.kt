package com.musculomental.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musculomental.app.domain.Equipment
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.ExerciseLibraryRepository
import com.musculomental.app.domain.Muscle
import com.musculomental.app.domain.filterExercises
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class LibraryFilters(val query: String = "", val region: String? = null, val equipment: Equipment? = null)
data class LibraryState(val loading: Boolean = true, val exercises: List<Exercise> = emptyList(), val error: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(private val repository: ExerciseLibraryRepository) : ViewModel() {
    private val filters = MutableStateFlow(LibraryFilters())
    private val refresh = MutableStateFlow(0)
    private val all = refresh.flatMapLatest {
        repository.observeExercises()
            .map { LibraryState(loading = false, exercises = it) }
            .onStart { emit(LibraryState(loading = true)) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(LibraryState(loading = false, error = true))
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryState())
    val state = combine(all, filters) { data, active ->
        data.copy(exercises = filterExercises(data.exercises, active.query, active.region, active.equipment))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())
    val activeFilters = filters

    fun search(value: String) = filters.update { it.copy(query = value) }
    fun filterRegion(value: String?) = filters.update { it.copy(region = if (it.region == value) null else value) }
    fun filterEquipment(value: Equipment?) = filters.update { it.copy(equipment = if (it.equipment == value) null else value) }
    fun clearFilters() { filters.value = LibraryFilters() }
    fun reload() = refresh.update { it + 1 }
    fun exercise(id: String): Exercise? = all.value.exercises.firstOrNull { it.id == id }
    fun allExercises(): List<Exercise> = all.value.exercises
    fun muscle(id: String): Muscle? = all.value.exercises.flatMap { it.muscles }.map { it.muscle }.firstOrNull { it.id == id }
}
