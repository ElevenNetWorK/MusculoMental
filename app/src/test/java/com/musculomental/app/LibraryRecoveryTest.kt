package com.musculomental.app

import com.musculomental.app.domain.Equipment
import com.musculomental.app.domain.Exercise
import com.musculomental.app.domain.ExerciseLibraryRepository
import com.musculomental.app.feature.library.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRecoveryTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun finish() { Dispatchers.resetMain() }

    @Test fun `retry reloads library after failure and keeps active search`() = runTest(dispatcher) {
        val repository = FailingOnceLibraryRepository()
        val model = LibraryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        model.search("supino")
        advanceUntilIdle()

        assertTrue(model.state.value.error)
        assertEquals("supino", model.activeFilters.value.query)

        model.reload()
        advanceUntilIdle()

        assertFalse(model.state.value.loading)
        assertFalse(model.state.value.error)
        assertEquals(listOf("supino-reto"), model.state.value.exercises.map { it.id })
        assertEquals(2, repository.observations)
    }

    private class FailingOnceLibraryRepository : ExerciseLibraryRepository {
        var observations = 0
        override fun observeExercises() = flow {
            observations += 1
            if (observations == 1) error("falha simulada")
            emit(listOf(Exercise("supino-reto", "Supino reto", Equipment.BARBELL, emptyList(), emptyList(), emptyList())))
        }
    }
}
