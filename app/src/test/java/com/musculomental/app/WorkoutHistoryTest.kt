package com.musculomental.app

import com.musculomental.app.domain.PerformedSet
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.SessionExercise
import com.musculomental.app.domain.SessionStatus
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.domain.buildWorkoutHistory
import com.musculomental.app.domain.calendarSessions
import com.musculomental.app.domain.exerciseProgress
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutHistoryTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    @Test fun historyCountsOnlyCompletedSessionsInLocalWeekAndMonth() {
        val now = time(2026, 9, 8, 12)
        val sessions = listOf(
            session(1, SessionStatus.COMPLETED, time(2026, 9, 7, 9), 3_600),
            session(2, SessionStatus.COMPLETED, time(2026, 9, 6, 9), 1_800),
            session(3, SessionStatus.COMPLETED, time(2026, 8, 30, 9), 600),
            session(4, SessionStatus.PAUSED, null, 0),
        )

        val history = buildWorkoutHistory(sessions, now, zone)

        assertEquals(3, history.completedSessions.size)
        assertEquals(1, history.sessionsThisWeek)
        assertEquals(2, history.sessionsThisMonth)
        assertEquals(6_000L, history.totalDurationSeconds)
    }

    @Test fun progressIsChronologicalAndUsesMaximumLoadPerSession() {
        val sessions = listOf(
            session(2, SessionStatus.COMPLETED, time(2026, 9, 8, 10), 600, loads = listOf(25.0, 30.0), repetitions = listOf(8, 6)),
            session(1, SessionStatus.COMPLETED, time(2026, 9, 1, 10), 600, loads = listOf(20.0, 22.5), repetitions = listOf(10, 8)),
        )

        val points = exerciseProgress(sessions, "supino-reto", zone)
        val record = buildWorkoutHistory(sessions, time(2026, 9, 8, 12), zone).records.single()

        assertEquals(listOf(22.5, 30.0), points.map { it.maximumLoadKg })
        assertEquals(listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8)), points.map { it.date })
        assertEquals(30.0, record.maximumLoadKg!!, 0.001)
        assertEquals(10, record.maximumRepetitions)
        assertEquals(200.0, record.bestSetVolumeKg!!, 0.001)
    }

    @Test fun calendarAndVolumeIgnoreCancelledDataAndMissingLoads() {
        val completed = session(1, SessionStatus.COMPLETED, time(2026, 9, 8, 10), 600, loads = listOf(10.0, null), repetitions = listOf(10, 12))
        val cancelled = session(2, SessionStatus.CANCELLED, time(2026, 9, 8, 11), 600, loads = listOf(100.0), repetitions = listOf(10))

        val history = buildWorkoutHistory(listOf(completed, cancelled), time(2026, 9, 8, 12), zone)
        val grouped = calendarSessions(listOf(completed, cancelled), zone)

        assertEquals(100.0, history.totalVolumeKg, 0.001)
        assertEquals(listOf(1L), grouped.getValue(LocalDate.of(2026, 9, 8)).map { it.id })
        assertEquals(1, history.records.size)
        assertNull(history.records.firstOrNull { it.exerciseId == "cancelled" })
    }

    private fun session(
        id: Long,
        status: SessionStatus,
        endedAt: Long?,
        durationSeconds: Long,
        loads: List<Double?> = listOf(20.0),
        repetitions: List<Int> = List(loads.size) { 10 },
    ): WorkoutSession {
        val end = endedAt ?: time(2026, 9, 8, 10)
        return WorkoutSession(
            id = id,
            sourcePlanId = 1,
            planName = "Treino $id",
            status = status,
            startedAt = end - durationSeconds * 1_000,
            endedAt = endedAt,
            currentExercisePosition = 0,
            note = "",
            effort = null,
            discomfort = null,
            exercises = listOf(
                SessionExercise(
                    id = id,
                    exerciseId = if (status == SessionStatus.CANCELLED) "cancelled" else "supino-reto",
                    exerciseName = if (status == SessionStatus.CANCELLED) "Cancelado" else "Supino reto",
                    position = 0,
                    plannedSets = loads.size,
                    plannedRepetitions = 10,
                    plannedLoadKg = 20.0,
                    plannedRestSeconds = 60,
                    sets = loads.mapIndexed { index, load ->
                        PerformedSet(id * 10 + index, index + 1, PerformedSetStatus.COMPLETED, repetitions[index], load, end)
                    },
                )
            ),
        )
    }

    private fun time(year: Int, month: Int, day: Int, hour: Int): Long =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()
}
