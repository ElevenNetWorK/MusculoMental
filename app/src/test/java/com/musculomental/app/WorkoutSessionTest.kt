package com.musculomental.app

import com.musculomental.app.domain.PerformedSet
import com.musculomental.app.domain.PerformedSetStatus
import com.musculomental.app.domain.SessionExercise
import com.musculomental.app.domain.SessionStatus
import com.musculomental.app.domain.WorkoutSession
import com.musculomental.app.domain.validateSessionFeedback
import com.musculomental.app.domain.validateSetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionTest {
    @Test fun `performed values are validated independently from planned values`() {
        assertTrue(validateSetEntry("10", "20,5").isValid)
        assertTrue(validateSetEntry("10", "").isValid)
        listOf("", "0", "1001", "texto").forEach { assertFalse(validateSetEntry(it, "20").isValid) }
        listOf("-1", "1001", "texto").forEach { assertFalse(validateSetEntry("10", it).isValid) }
    }

    @Test fun `optional feedback has bounded values`() {
        assertNull(validateSessionFeedback("Tudo certo", null, null))
        assertNull(validateSessionFeedback("", 1, 10))
        assertNotNull(validateSessionFeedback("x".repeat(501), null, null))
        assertNotNull(validateSessionFeedback("", 0, null))
        assertNotNull(validateSessionFeedback("", null, 11))
    }

    @Test fun `summary counts states and volume only from completed sets`() {
        val session = WorkoutSession(
            1, 2, "Treino", SessionStatus.COMPLETED, 1, 2, 0, "", null, null,
            listOf(SessionExercise(1, "supino", "Supino", 0, 3, 10, 20.0, 60, listOf(
                PerformedSet(1, 1, PerformedSetStatus.COMPLETED, 10, 20.0, 2),
                PerformedSet(2, 2, PerformedSetStatus.COMPLETED, 8, 20.0, 3),
                PerformedSet(3, 3, PerformedSetStatus.SKIPPED, null, null, null),
            ))),
        )
        assertEquals(2, session.completedSets)
        assertEquals(1, session.skippedSets)
        assertEquals(360.0, session.estimatedVolumeKg, 0.0)
    }
}
