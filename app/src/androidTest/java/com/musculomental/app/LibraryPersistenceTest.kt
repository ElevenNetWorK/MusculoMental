package com.musculomental.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.LocalExerciseLibraryRepository
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.domain.MuscleRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryPersistenceTest {
    @Test fun seedPersistsAndRelationsWorkInBothDirections() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "library-test-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, ProfileDatabase::class.java, name).addMigrations(ProfileDatabase.MIGRATION_1_2, ProfileDatabase.MIGRATION_2_3, ProfileDatabase.MIGRATION_3_4, ProfileDatabase.MIGRATION_4_5).build()
        var database = open()
        try {
            var items = LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first()
            assertEquals(8, items.size)
            val reverseCurl = items.first { it.id == "rosca-inversa" }
            assertEquals(setOf("Braquial", "Braquiorradial"), reverseCurl.muscles.filter { it.role == MuscleRole.PRIMARY }.map { it.muscle.commonName }.toSet())
            assertTrue(items.filter { exercise -> exercise.muscles.any { it.muscle.id == "triceps" } }.map { it.id }.containsAll(listOf("supino-reto", "desenvolvimento", "triceps-halter")))
            database.close()
            database = open()
            items = LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first()
            assertEquals(8, items.size)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
