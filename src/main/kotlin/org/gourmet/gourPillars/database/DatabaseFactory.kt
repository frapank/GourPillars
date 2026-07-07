package org.gourmet.gourPillars.database

import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.File

fun checkDatabase(): Database {
    val plugin = GourPillars.instance
    val file = File(plugin.dataFolder, "database.yml")
    if (!file.exists()) {
        plugin.saveResource("database.yml", false)
    }
    val config = YamlConfiguration.loadConfiguration(file)

    return when (config.getString("storage-type")?.trim()?.lowercase()) {
        "sqlite" -> {
            SQLiteDatabase(config)
        }

        "mysql", null -> {
            MySQLDatabase(config)
        }

        else -> {
            Logger.warning(
                "Unknown storage-type '${config.getString("storage-type")}' in database.yml, defaulting to mysql",
            )
            MySQLDatabase(config)
        }
    }
}
