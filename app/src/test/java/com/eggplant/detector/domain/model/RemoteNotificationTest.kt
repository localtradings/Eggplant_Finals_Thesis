package com.eggplant.detector.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteNotificationTest {
    private val notification = RemoteNotification(
        id = "11111111-1111-4111-8111-111111111111",
        category = "announcement",
        titleEn = "New guide",
        bodyEn = "A new guide is available.",
        titleFil = "Bagong gabay",
        bodyFil = "May bagong gabay.",
        publishedAt = "2026-08-08T10:00:00Z",
    )

    @Test
    fun `remote notification has a stable read key and language fallback`() {
        assertEquals("remote:11111111-1111-4111-8111-111111111111", notification.key)
        assertEquals("New guide", notification.title("en"))
        assertEquals("May bagong gabay.", notification.body("fil"))
        assertEquals("New guide", notification.title("unknown"))
    }
}
