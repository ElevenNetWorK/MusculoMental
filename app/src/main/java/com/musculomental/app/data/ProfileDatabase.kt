package com.musculomental.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musculomental.app.domain.Experience
import com.musculomental.app.domain.ProfileRepository
import com.musculomental.app.domain.TrainingMode
import com.musculomental.app.domain.UserProfile
import com.musculomental.app.domain.validateProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val goal: String,
    val experience: String,
    val weeklyFrequency: Int,
    val mode: String,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Upsert
    suspend fun save(profile: ProfileEntity)
}

@Database(
    entities = [
        ProfileEntity::class, MuscleEntity::class, ExerciseEntity::class, ExerciseMuscleEntity::class,
        WorkoutPlanEntity::class, PlannedExerciseEntity::class,
        WorkoutSessionEntity::class, SessionExerciseEntity::class, PerformedSetEntity::class,
        RestIntervalEntity::class, TimerPreferencesEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun restTimerDao(): RestTimerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `muscle` (`id` TEXT NOT NULL, `commonName` TEXT NOT NULL, `anatomicalName` TEXT NOT NULL, `region` TEXT NOT NULL, `function` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `exercise` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `equipment` TEXT NOT NULL, `instructions` TEXT NOT NULL, `commonErrors` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_muscle` (`exerciseId` TEXT NOT NULL, `muscleId` TEXT NOT NULL, `role` TEXT NOT NULL, PRIMARY KEY(`exerciseId`, `muscleId`), FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`muscleId`) REFERENCES `muscle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscle_muscleId` ON `exercise_muscle` (`muscleId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_plan` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `status` TEXT NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `planned_exercise` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planId` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL, `sets` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL, `suggestedLoadKg` REAL, `restSeconds` INTEGER NOT NULL, FOREIGN KEY(`planId`) REFERENCES `workout_plan`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_exercise_planId` ON `planned_exercise` (`planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_exercise_exerciseId` ON `planned_exercise` (`exerciseId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_exercise_planId_position` ON `planned_exercise` (`planId`, `position`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourcePlanId` INTEGER NOT NULL, `planName` TEXT NOT NULL, `status` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `currentExercisePosition` INTEGER NOT NULL, `note` TEXT NOT NULL, `effort` INTEGER, `discomfort` INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `session_exercise` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `sourcePlannedExerciseId` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `exerciseName` TEXT NOT NULL, `position` INTEGER NOT NULL, `plannedSets` INTEGER NOT NULL, `plannedRepetitions` INTEGER NOT NULL, `plannedLoadKg` REAL, `plannedRestSeconds` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `workout_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercise_sessionId` ON `session_exercise` (`sessionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_exercise_sessionId_position` ON `session_exercise` (`sessionId`, `position`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `performed_set` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionExerciseId` INTEGER NOT NULL, `setNumber` INTEGER NOT NULL, `status` TEXT NOT NULL, `repetitions` INTEGER, `loadKg` REAL, `completedAt` INTEGER, FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercise`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_performed_set_sessionExerciseId` ON `performed_set` (`sessionExerciseId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_performed_set_sessionExerciseId_setNumber` ON `performed_set` (`sessionExerciseId`, `setNumber`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `rest_interval` (`performedSetId` INTEGER NOT NULL, `plannedSeconds` INTEGER NOT NULL, `targetSeconds` INTEGER NOT NULL, `countMode` TEXT NOT NULL, `state` TEXT NOT NULL, `accumulatedSeconds` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `finishedAt` INTEGER, `actualSeconds` INTEGER, `adjusted` INTEGER NOT NULL, `approximate` INTEGER NOT NULL, `alerted` INTEGER NOT NULL, PRIMARY KEY(`performedSetId`), FOREIGN KEY(`performedSetId`) REFERENCES `performed_set`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `timer_preferences` (`id` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `startMode` TEXT NOT NULL, `countMode` TEXT NOT NULL, `alertMode` TEXT NOT NULL, `recordActual` INTEGER NOT NULL, `finishOnNextSet` INTEGER NOT NULL, `defaultSeconds` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}

class LocalProfileRepository(private val dao: ProfileDao) : ProfileRepository {
    override fun observe(): Flow<UserProfile?> = dao.observe().map { row ->
        row?.let {
            UserProfile(it.displayName, it.goal, Experience.valueOf(it.experience), it.weeklyFrequency, TrainingMode.valueOf(it.mode))
        }
    }

    override suspend fun save(profile: UserProfile) {
        require(validateProfile(profile.displayName, profile.goal, profile.weeklyFrequency.toString()).isValid)
        dao.save(
            ProfileEntity(
                displayName = profile.displayName.trim(), goal = profile.goal.trim(),
                experience = profile.experience.name, weeklyFrequency = profile.weeklyFrequency, mode = profile.mode.name,
            )
        )
    }
}
