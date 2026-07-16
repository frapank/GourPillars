package org.gourmet.gourPillars.managers

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object ConfigManager {
    // Options from older versions that no longer exist (game events belong to addon
    // plugins now, see docs/api.md). Stripped from existing configs so they can't
    // resurface as dead entries.
    private val OBSOLETE_KEYS =
        listOf(
            "game.knockback-multiplier",
            "game.lava-rise-interval-seconds",
            "game.border",
            "game.events.lava",
            "game.events.knockback",
            "game.events.border",
            "gui.vote.items.lava-event",
            "gui.vote.items.knockback-event",
            "gui.vote.items.border-event",
        )

    /**
     * Compares the live config.yml against the plugin's bundled default and
     * adds any option missing from it (e.g. after updating to a version that
     * introduced new settings), logging a warning for each one added.
     * Also removes options that no longer exist in this version.
     */
    fun applyMissingDefaults() {
        val plugin = GourPillars.instance
        val removed = removeObsoleteKeys(plugin.config)
        if (removed > 0) {
            plugin.saveConfig()
            Logger.warning(
                "Removed $removed obsolete option(s) from config.yml: game events are now provided by addon plugins, see docs/api.md",
            )
        }
        applyMissingDefaults("config.yml", plugin.config) { plugin.saveConfig() }
    }

    private fun removeObsoleteKeys(target: FileConfiguration): Int {
        var removed = 0
        for (path in OBSOLETE_KEYS) {
            if (target.isSet(path)) {
                target.set(path, null)
                removed++
            }
        }
        return removed
    }

    /**
     * Same as [applyMissingDefaults] but for any bundled resource/config pair (e.g. language.yml).
     */
    fun applyMissingDefaults(
        resourceName: String,
        target: FileConfiguration,
        save: () -> Unit,
    ) {
        val plugin = GourPillars.instance
        val defaultsStream = plugin.getResource(resourceName) ?: return
        val defaults =
            defaultsStream.use {
                YamlConfiguration.loadConfiguration(InputStreamReader(it, StandardCharsets.UTF_8))
            }

        val added = mergeMissingKeys(defaults, target)
        if (added > 0) {
            save()
            Logger.warning("Added $added missing option(s) to $resourceName from the default template, check $resourceName")
        }
    }

    // User-editable named collections (add/remove/reorder entries freely, see docs/config.md):
    // only backfilled wholesale when missing entirely, never merged entry-by-entry, so an entry
    // the user removed stays removed instead of being silently recreated on the next startup.
    private val FREEFORM_SECTIONS = setOf("lobby-items", "gui.vote.items")

    private fun mergeMissingKeys(
        defaults: ConfigurationSection,
        target: ConfigurationSection,
    ): Int {
        var added = 0
        for (key in defaults.getKeys(false)) {
            val path = fullPath(target, key)
            if (path in FREEFORM_SECTIONS && target.isConfigurationSection(key)) continue

            val defaultSection = defaults.getConfigurationSection(key)
            if (defaultSection != null) {
                val targetSection = target.getConfigurationSection(key) ?: target.createSection(key)
                added += mergeMissingKeys(defaultSection, targetSection)
            } else if (!target.isSet(key)) {
                val defaultValue = defaults.get(key)
                Logger.warning("Missing config option '$path', added default: $defaultValue")
                target.set(key, defaultValue)
                added++
            }
        }
        return added
    }

    private fun fullPath(
        section: ConfigurationSection,
        key: String,
    ): String {
        val currentPath = section.currentPath
        return if (currentPath.isNullOrEmpty()) key else "$currentPath.$key"
    }
}
