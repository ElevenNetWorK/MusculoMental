package com.musculomental.app

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.musculomental.app.data.ProfileDatabase
import com.musculomental.app.data.LocalExerciseLibraryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProfileMigrationTest {
    @Test fun migrationFromVersionOneKeepsTheExistingProfile() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "profile-migration-${System.nanoTime()}.db"
        val legacy = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        legacy.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `displayName` TEXT NOT NULL, `goal` TEXT NOT NULL, `experience` TEXT NOT NULL, `weeklyFrequency` INTEGER NOT NULL, `mode` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        legacy.execSQL(
            "INSERT INTO user_profile (id, displayName, goal, experience, weeklyFrequency, mode) VALUES (1, 'Ana', 'Força', 'INTERMEDIATE', 4, 'GYM')"
        )
        legacy.version = 1
        legacy.close()

        val database = Room.databaseBuilder(context, ProfileDatabase::class.java, name)
            .addMigrations(ProfileDatabase.MIGRATION_1_2, ProfileDatabase.MIGRATION_2_3, ProfileDatabase.MIGRATION_3_4, ProfileDatabase.MIGRATION_4_5)
            .build()
        try {
            val profile = database.profileDao().observe().first()
            assertNotNull(profile)
            assertEquals("Ana", profile?.displayName)
            assertEquals("Força", profile?.goal)
            assertEquals(4, profile?.weeklyFrequency)
            assertEquals(8, LocalExerciseLibraryRepository(database.exerciseDao()).observeExercises().first().size)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
