package com.eggplant.detector.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EggplantRepositoryTest {
    @Test
    fun `bundled catalog seeds only an empty local catalog`() {
        assertTrue(shouldSeedBundledCatalog(0))
        assertFalse(shouldSeedBundledCatalog(1))
        assertFalse(shouldSeedBundledCatalog(8))
    }
}
