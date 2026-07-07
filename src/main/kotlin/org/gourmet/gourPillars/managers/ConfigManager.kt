package org.gourmet.gourPillars.managers

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object ConfigManager {
    /**
     * Compares the live config.yml against the plugin's bundled default and
     * adds any option missing from it (e.g. after updating to a version that
     * introduced new settings), logging a warning for each one added.
     */
    fun applyMissingDefaults() {
        val plugin = GourPillars.instance
        val defaultsStream = plugin.getResource("config.yml") ?: return
        val defaults =
            defaultsStream.use {
                YamlConfiguration.loadConfiguration(InputStreamReader(it, StandardCharsets.UTF_8))
            }

        val added = mergeMissingKeys(defaults, plugin.config)
        if (added > 0) {
            plugin.saveConfig()
            Logger.warning("Added $added missing config option(s) from the default template, check config.yml")
        }
    }

    private fun mergeMissingKeys(
        defaults: ConfigurationSection,
        target: ConfigurationSection,
    ): Int {
        var added = 0
        for (key in defaults.getKeys(false)) {
            val defaultSection = defaults.getConfigurationSection(key)

            if (defaultSection != null) {
                val targetSection = target.getConfigurationSection(key) ?: target.createSection(key)
                added += mergeMissingKeys(defaultSection, targetSection)
            } else if (!target.isSet(key)) {
                val defaultValue = defaults.get(key)
                Logger.warning("Missing config option '${fullPath(target, key)}', added default: $defaultValue")
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
