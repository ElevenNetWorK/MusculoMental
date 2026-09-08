package com.musculomental.app

import com.musculomental.app.domain.RestInterval
import com.musculomental.app.domain.RestTimerPreferences
import com.musculomental.app.domain.TimerCountMode
import com.musculomental.app.domain.TimerState
import com.musculomental.app.domain.validateTimerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RestTimerTest {
    @Test fun `running timer derives elapsed and remaining time from wall clock`() {
        val timer = RestInterval(1, 60, 75, TimerCountMode.COUNTDOWN, TimerState.RUNNING, 10, 1_000, null, null, true, false, false)
        assertEquals(15, timer.elapsedAt(6_000))
        assertEquals(60, timer.remainingAt(6_000))
        assertEquals(10, timer.elapsedAt(0))
    }

    @Test fun `default timer duration has explicit limits`() {
        assertNull(validateTimerPreferences(RestTimerPreferences(defaultSeconds = 60)))
        assertNotNull(validateTimerPreferences(RestTimerPreferences(defaultSeconds = 4)))
        assertNotNull(validateTimerPreferences(RestTimerPreferences(defaultSeconds = 3601)))
    }
}
