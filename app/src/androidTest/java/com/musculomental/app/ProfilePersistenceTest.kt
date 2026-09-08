package com.musculomental.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.LocalProfileRepository
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.domain.Experience
import com.musculomental.app.domain.TrainingMode
import com.musculomental.app.domain.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProfilePersistenceTest {
    @Test fun profileSurvivesDatabaseReopenAndEditing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "profile-test-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, ProfileDatabase::class.java, databaseName).build()
        var database = open()
        try {
            val original = UserProfile("Pessoa teste", "Consistência", Experience.INTERMEDIATE, 4, TrainingMode.AUTONOMOUS)
            LocalProfileRepository(database.profileDao()).save(original)
            database.close()
            database = open()
            val repository = LocalProfileRepository(database.profileDao())
            assertEquals(original, repository.observe().first())
            val updated = original.copy(goal = "Nova rotina", weeklyFrequency = 2, mode = TrainingMode.STUDENT_DEMO)
            repository.save(updated)
            database.close()
            database = open()
            assertEquals(updated, LocalProfileRepository(database.profileDao()).observe().first())
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
