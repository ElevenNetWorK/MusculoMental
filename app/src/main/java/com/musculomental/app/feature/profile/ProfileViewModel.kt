package com.musculomental.app.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musculomental.app.domain.Experience
import com.musculomental.app.domain.ProfileErrors
import com.musculomental.app.domain.ProfileRepository
import com.musculomental.app.domain.TrainingMode
import com.musculomental.app.domain.UserProfile
import com.musculomental.app.domain.validateProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val loadError: Boolean = false,
    val saving: Boolean = false,
    val saveError: Boolean = false,
    val saved: Boolean = false,
    val errors: ProfileErrors = ProfileErrors(),
)

class ProfileViewModel(private val repository: ProfileRepository, private val handle: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()
    val name = handle.getStateFlow("name", "")
    val goal = handle.getStateFlow("goal", "")
    val frequency = handle.getStateFlow("frequency", "3")
    val mode = handle.getStateFlow("mode", TrainingMode.AUTONOMOUS.name)
    val experience = handle.getStateFlow("experience", Experience.BEGINNER.name)
    private var loadJob: Job? = null

    init { reload() }

    fun reload() {
        loadJob?.cancel()
        mutableState.update { it.copy(loading = true, loadError = false) }
        loadJob = viewModelScope.launch {
            try {
                repository.observe().collect { profile ->
                    mutableState.update { it.copy(loading = false, loadError = false, profile = profile) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, loadError = true) }
            }
        }
    }

    fun beginEdit() {
        val profile = state.value.profile
        handle["name"] = profile?.displayName.orEmpty()
        handle["goal"] = profile?.goal.orEmpty()
        handle["frequency"] = (profile?.weeklyFrequency ?: 3).toString()
        handle["mode"] = (profile?.mode ?: TrainingMode.AUTONOMOUS).name
        handle["experience"] = (profile?.experience ?: Experience.BEGINNER).name
        mutableState.update { it.copy(errors = ProfileErrors(), saveError = false, saved = false) }
    }

    fun change(field: String, value: String) {
        if (state.value.saving) return
        require(field in setOf("name", "goal", "frequency", "mode", "experience"))
        handle[field] = value
        mutableState.update { it.copy(saveError = false) }
    }

    fun consumeSaved() { mutableState.update { it.copy(saved = false) } }

    fun save() {
        if (state.value.saving) return
        val errors = validateProfile(name.value, goal.value, frequency.value)
        mutableState.update { it.copy(errors = errors, saveError = false) }
        if (!errors.isValid) return
        val profile = UserProfile(
            name.value.trim(), goal.value.trim(), Experience.valueOf(experience.value),
            frequency.value.toInt(), TrainingMode.valueOf(mode.value),
        )
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.save(profile)
                mutableState.update { it.copy(profile = profile, saving = false, saved = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, saveError = true) }
            }
        }
    }
}
