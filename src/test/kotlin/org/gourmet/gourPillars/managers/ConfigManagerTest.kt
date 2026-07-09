package org.gourmet.gourPillars.managers

import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigManagerTest {
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

    private fun loadBundledConfig(): YamlConfiguration =
        GourPillars.instance.getResource("config.yml")!!.use {
            YamlConfiguration.loadConfiguration(InputStreamReader(it, StandardCharsets.UTF_8))
        }

    @Test
    fun `a removed lobby-items entry is not recreated`() {
        val target = loadBundledConfig()
        target.set("lobby-items.casual-match", null)

        ConfigManager.applyMissingDefaults("config.yml", target) {}

        assertFalse(target.isSet("lobby-items.casual-match"))
        assertTrue(target.isSet("lobby-items.mode-selector"))
        assertTrue(target.isSet("lobby-items.cosmetics"))
    }

    @Test
    fun `an entirely missing lobby-items section is backfilled`() {
        val target = loadBundledConfig()
        target.set("lobby-items", null)

        ConfigManager.applyMissingDefaults("config.yml", target) {}

        assertTrue(target.isSet("lobby-items.mode-selector"))
        assertTrue(target.isSet("lobby-items.cosmetics"))
        assertTrue(target.isSet("lobby-items.casual-match"))
    }

    @Test
    fun `a removed vote GUI item is not recreated`() {
        val target = loadBundledConfig()
        target.set("gui.vote.items.day-vote", null)

        ConfigManager.applyMissingDefaults("config.yml", target) {}

        assertFalse(target.isSet("gui.vote.items.day-vote"))
        assertTrue(target.isSet("gui.vote.items.no-event"))
    }

    @Test
    fun `a missing non-freeform option is still backfilled`() {
        val target = loadBundledConfig()
        target.set("game.match-duration-seconds", null)

        ConfigManager.applyMissingDefaults("config.yml", target) {}

        assertTrue(target.isSet("game.match-duration-seconds"))
    }

    @Test
    fun `a lobby-items value corrupted into a non-section is still self-healed`() {
        val target = loadBundledConfig()
        target.set("lobby-items", "oops")

        ConfigManager.applyMissingDefaults("config.yml", target) {}

        assertTrue(target.isConfigurationSection("lobby-items"))
        assertTrue(target.isSet("lobby-items.mode-selector"))
        assertTrue(target.isSet("lobby-items.casual-match"))
    }
}
