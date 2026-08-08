package com.eggplant.detector.data.history

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRetentionTest {
    @Test
    fun `expiry cutoff is exactly thirty days before cleanup time`() {
        val now = LocalDateTime.of(2026, 8, 7, 12, 0)

        assertEquals(LocalDateTime.of(2026, 7, 8, 12, 0), historyExpiryCutoff(now))
    }
}
