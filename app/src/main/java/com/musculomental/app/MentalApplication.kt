package com.musculomental.app

import android.app.Application
import androidx.room.Room
import com.musculomental.app.data.LocalExerciseLibraryRepository
import com.musculomental.app.data.LocalProfileRepository
import com.musculomental.app.data.LocalWorkoutPlanRepository
import com.musculomental.app.data.LocalWorkoutSessionRepository
import com.musculomental.app.data.LocalRestTimerRepository
import com.musculomental.app.data.ProfileDatabase

class MentalApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, ProfileDatabase::class.java, "musculo-mental.db")
            .addMigrations(ProfileDatabase.MIGRATION_1_2, ProfileDatabase.MIGRATION_2_3, ProfileDatabase.MIGRATION_3_4, ProfileDatabase.MIGRATION_4_5)
            .build()
    }
    val profileRepository by lazy { LocalProfileRepository(database.profileDao()) }
    val exerciseLibraryRepository by lazy { LocalExerciseLibraryRepository(database.exerciseDao()) }
    val workoutPlanRepository by lazy { LocalWorkoutPlanRepository(database.workoutPlanDao()) }
    val workoutSessionRepository by lazy { LocalWorkoutSessionRepository(database.workoutSessionDao()) }
    val restTimerRepository by lazy { LocalRestTimerRepository(database.restTimerDao()) }
}
