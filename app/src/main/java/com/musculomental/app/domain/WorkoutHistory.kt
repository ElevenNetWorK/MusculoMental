package com.musculomental.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class ExerciseProgressPoint(
    val sessionId: Long,
    val date: LocalDate,
    val maximumLoadKg: Double,
    val sessionVolumeKg: Double,
)

data class ExerciseRecord(
    val exerciseId: String,
    val exerciseName: String,
    val maximumLoadKg: Double?,
    val maximumRepetitions: Int?,
    val bestSetVolumeKg: Double?,
)

data class WorkoutHistorySummary(
    val completedSessions: List<WorkoutSession>,
    val sessionsThisWeek: Int,
    val sessionsThisMonth: Int,
    val totalDurationSeconds: Long,
    val totalVolumeKg: Double,
    val records: List<ExerciseRecord>,
)

fun buildWorkoutHistory(
    sessions: List<WorkoutSession>,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): WorkoutHistorySummary {
    val completed = sessions
        .filter { it.status == SessionStatus.COMPLETED && it.endedAt != null }
        .sortedByDescending { it.endedAt }
    val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(7)
    val month = YearMonth.from(today)
    val dated = completed.map { it to it.historyDate(zoneId) }

    return WorkoutHistorySummary(
        completedSessions = completed,
        sessionsThisWeek = dated.count { (_, date) -> !date.isBefore(weekStart) && date.isBefore(weekEnd) },
        sessionsThisMonth = dated.count { (_, date) -> YearMonth.from(date) == month },
        totalDurationSeconds = completed.sumOf { session ->
            ((session.endedAt!! - session.startedAt) / 1_000L).coerceAtLeast(0L)
        },
        totalVolumeKg = completed.sumOf { it.estimatedVolumeKg },
        records = exerciseRecords(completed),
    )
}

fun calendarSessions(
    sessions: List<WorkoutSession>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, List<WorkoutSession>> = sessions
    .filter { it.status == SessionStatus.COMPLETED && it.endedAt != null }
    .sortedByDescending { it.endedAt }
    .groupBy { it.historyDate(zoneId) }

fun exerciseProgress(
    sessions: List<WorkoutSession>,
    exerciseId: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<ExerciseProgressPoint> = sessions
    .filter { it.status == SessionStatus.COMPLETED && it.endedAt != null }
    .mapNotNull { session ->
        val sets = session.exercises
            .filter { it.exerciseId == exerciseId }
            .flatMap { it.sets }
            .filter { it.status == PerformedSetStatus.COMPLETED }
        val maximumLoad = sets.mapNotNull { it.loadKg }.maxOrNull() ?: return@mapNotNull null
        ExerciseProgressPoint(
            sessionId = session.id,
            date = session.historyDate(zoneId),
            maximumLoadKg = maximumLoad,
            sessionVolumeKg = sets.sumOf { (it.loadKg ?: 0.0) * (it.repetitions ?: 0) },
        )
    }
    .sortedWith(compareBy<ExerciseProgressPoint> { it.date }.thenBy { it.sessionId })

private fun exerciseRecords(sessions: List<WorkoutSession>): List<ExerciseRecord> = sessions
    .flatMap { session -> session.exercises.map { it.exerciseId to it } }
    .groupBy({ it.first }, { it.second })
    .map { (exerciseId, exercises) ->
        val completedSets = exercises.flatMap { it.sets }.filter { it.status == PerformedSetStatus.COMPLETED }
        ExerciseRecord(
            exerciseId = exerciseId,
            exerciseName = exercises.first().exerciseName,
            maximumLoadKg = completedSets.mapNotNull { it.loadKg }.maxOrNull(),
            maximumRepetitions = completedSets.mapNotNull { it.repetitions }.maxOrNull(),
            bestSetVolumeKg = completedSets.mapNotNull { set ->
                val repetitions = set.repetitions ?: return@mapNotNull null
                val load = set.loadKg ?: return@mapNotNull null
                repetitions * load
            }.maxOrNull(),
        )
    }
    .filter { it.maximumLoadKg != null || it.maximumRepetitions != null }
    .sortedBy { it.exerciseName }

private fun WorkoutSession.historyDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(requireNotNull(endedAt)).atZone(zoneId).toLocalDate()
