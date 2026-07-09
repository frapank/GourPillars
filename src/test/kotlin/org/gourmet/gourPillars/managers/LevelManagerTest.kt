package org.gourmet.gourPillars.managers

import org.gourmet.gourPillars.GourPillars
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LevelManagerTest {
    @BeforeAll
    fun setUpAll() {
        MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    @AfterEach
    fun tearDownEach() {
        val config = GourPillars.instance.config
        config.set("level.enabled", true)
        config.set("level.xp-per-level", 100)
        config.set("level.xp-rewards", null)
    }

    @Test
    fun `enabled reflects the config toggle`() {
        assertTrue(LevelManager.enabled)

        GourPillars.instance.config.set("level.enabled", false)

        assertFalse(LevelManager.enabled)
    }

    @Test
    fun `xpFor is 0 (off) for an unknown source, and when explicitly set to 0`() {
        assertEquals(0, LevelManager.xpFor("some-unconfigured-source"))

        GourPillars.instance.config.set("level.xp-rewards.kill", 0)
        assertEquals(0, LevelManager.xpFor("kill"))

        GourPillars.instance.config.set("level.xp-rewards.kill", 25)
        assertEquals(25, LevelManager.xpFor("kill"))
    }

    @Test
    fun `levelForXp uses xp-per-level thresholds`() {
        GourPillars.instance.config.set("level.xp-per-level", 100)

        assertEquals(1, LevelManager.levelForXp(0))
        assertEquals(1, LevelManager.levelForXp(99))
        assertEquals(2, LevelManager.levelForXp(100))
        assertEquals(3, LevelManager.levelForXp(250))
    }

    @Test
    fun `levelForXp coerces a non-positive xp-per-level to avoid dividing by zero`() {
        GourPillars.instance.config.set("level.xp-per-level", 0)

        assertEquals(1 + 50, LevelManager.levelForXp(50))
    }
}
