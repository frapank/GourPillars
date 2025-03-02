package org.gourmet.gourPillars.other.messages

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import java.io.File

class LanguageManager {

    private lateinit var langFile: File
    private lateinit var langConfig: FileConfiguration
    private val plugin = GourPillars.instance

    fun saveDefaultLanguageFile() {
        langFile = File(plugin.dataFolder, "language.yml")
        if (!langFile.exists()) {
            plugin.saveResource("language.yml", false)
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile)
        MessageData.load() // Carica i messaggi subito dopo l'inizializzazione
    }

    fun getLanguageConfig(): FileConfiguration {
        check(::langConfig.isInitialized) { "Language config non inizializzato" }
        return langConfig
    }

    fun reloadLanguage() {
        langConfig = YamlConfiguration.loadConfiguration(langFile)
        MessageData.load()
    }
}