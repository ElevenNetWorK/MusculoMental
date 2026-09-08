package com.musculomental.app

import androidx.lifecycle.SavedStateHandle
import com.musculomental.app.domain.Experience
import com.musculomental.app.domain.ProfileRepository
import com.musculomental.app.domain.TrainingMode
import com.musculomental.app.domain.UserProfile
import com.musculomental.app.domain.validateProfile
import com.musculomental.app.feature.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun finish() { Dispatchers.resetMain() }

    @Test fun `required fields and frequency boundaries are validated`() {
        assertFalse(validateProfile("  ", "", "3").isValid)
        listOf("", "0", "8", "-1", "2.5", "texto").forEach {
            assertNotNull(validateProfile("Pessoa", "Rotina", it).frequency)
        }
        assertTrue(validateProfile("Pessoa", "Rotina", "1").isValid)
        assertTrue(validateProfile("Pessoa", "Rotina", "7").isValid)
        assertNotNull(validateProfile("a".repeat(61), "Rotina", "3").name)
        assertNotNull(validateProfile("Pessoa", "a".repeat(121), "3").goal)
    }

    @Test fun `invalid form never writes a profile`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = ProfileViewModel(repository, SavedStateHandle())
        model.save()
        advanceUntilIdle()
        assertEquals(0, repository.writes)
        assertFalse(model.state.value.saved)
        assertNotNull(model.state.value.errors.name)
    }

    @Test fun `failed save retains draft and supports retry`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failSave = true }
        val handle = SavedStateHandle()
        val model = ProfileViewModel(repository, handle)
        model.change("name", "  Pessoa teste  ")
        model.change("goal", "  Criar rotina  ")
        model.change("mode", TrainingMode.STUDENT_DEMO.name)
        model.change("experience", Experience.ADVANCED.name)
        model.save()
        advanceUntilIdle()
        assertTrue(model.state.value.saveError)
        assertFalse(model.state.value.saving)
        assertEquals("  Pessoa teste  ", model.name.value)
        repository.failSave = false
        model.save()
        advanceUntilIdle()
        assertTrue(model.state.value.saved)
        assertEquals("Pessoa teste", repository.value.value?.displayName)
        assertEquals(TrainingMode.STUDENT_DEMO, repository.value.value?.mode)
        assertEquals(Experience.ADVANCED, repository.value.value?.experience)
    }

    @Test fun `load failure can be retried without deleting stored profile`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failLoad = true }
        val model = ProfileViewModel(repository, SavedStateHandle())
        advanceUntilIdle()
        assertTrue(model.state.value.loadError)
        repository.failLoad = false
        repository.value.value = UserProfile("Teste", "Rotina", Experience.NEW, 1, TrainingMode.AUTONOMOUS)
        model.reload()
        advanceUntilIdle()
        assertFalse(model.state.value.loadError)
        assertEquals("Teste", model.state.value.profile?.displayName)
    }

    @Test fun `restored draft is not overwritten by profile loading`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("name" to "Rascunho", "goal" to "Objetivo em edição", "frequency" to "4"))
        val model = ProfileViewModel(FakeRepository(), handle)
        advanceUntilIdle()
        assertEquals("Rascunho", model.name.value)
        assertEquals("4", model.frequency.value)
    }

    private class FakeRepository : ProfileRepository {
        val value = MutableStateFlow<UserProfile?>(null)
        var writes = 0
        var failSave = false
        var failLoad = false
        override fun observe(): Flow<UserProfile?> = if (failLoad) flow { error("Read failed") } else value
        override suspend fun save(profile: UserProfile) {
            if (failSave) error("Write failed")
            writes++
            value.value = profile
        }
    }
}
