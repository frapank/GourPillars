package org.gourmet.gourPillars.managers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.File
import java.sql.SQLException

class DatabaseManager {

    private data class DatabaseSettings(
        val host: String,
        val port: Int,
        val database: String,
        val username: String,
        val password: String,
        val useSsl: Boolean,
        val allowPublicKeyRetrieval: Boolean,
        val poolSize: Int
    )

    private companion object {
        private val IDENTIFIER_REGEX = Regex("^[A-Za-z0-9_]+$")
    }

    private val settings = loadSettings()
    private lateinit var dataSource: HikariDataSource

    val playersStats = HashMap<Player, PlayerStats>()
    var isOnline = false
        private set
    var lastError: String? = null
        private set

    init {
        checkAndCreateDatabase()
    }

    private fun loadSettings(): DatabaseSettings {
        val plugin = GourPillars.instance
        val file = File(plugin.dataFolder, "database.yml")
        if (!file.exists()) {
            plugin.saveResource("database.yml", false)
        }
        val config = YamlConfiguration.loadConfiguration(file)

        val host = config.getString("host")?.trim()?.takeIf { it.isNotBlank() }
            ?: invalid("host", "localhost")

        val port = config.getInt("port", 3306)
            .takeIf { it in 1..65535 } ?: invalid("port", 3306)

        val database = config.getString("database")?.trim()
            ?.takeIf { IDENTIFIER_REGEX.matches(it) } ?: invalid("database", "dream")

        val username = config.getString("username")?.trim()
            ?.takeIf { IDENTIFIER_REGEX.matches(it) } ?: invalid("username", "root")

        val password = config.getString("password") ?: ""

        val useSsl = config.getBoolean("use-ssl", false)
        val allowPublicKeyRetrieval = config.getBoolean("allow-public-key-retrieval", true)

        val poolSize = config.getInt("pool-size", 10)
            .takeIf { it in 1..50 } ?: invalid("pool-size", 10)

        return DatabaseSettings(host, port, database, username, password, useSsl, allowPublicKeyRetrieval, poolSize)
    }

    private fun <T> invalid(field: String, default: T): T {
        Logger.warning("Valore mancante o non valido per '$field' in database.yml, uso il default: $default")
        return default
    }

    private fun jdbcUrl(includeDatabase: Boolean): String {
        val path = if (includeDatabase) "/${settings.database}" else "/"
        return "jdbc:mysql://${settings.host}:${settings.port}$path" +
            "?useSSL=${settings.useSsl}&allowPublicKeyRetrieval=${settings.allowPublicKeyRetrieval}&characterEncoding=UTF-8"
    }

    private fun createHikariDataSource(jdbcUrl: String): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = jdbcUrl
        config.username = settings.username
        config.password = settings.password
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.maximumPoolSize = settings.poolSize
        // Fail fast on startup instead of blocking the main thread for Hikari's 30s default.
        config.connectionTimeout = 5000
        return HikariDataSource(config)
    }

    private fun markOffline(message: String) {
        Logger.warning(message)
        isOnline = false
        lastError = message
    }

    private fun checkAndCreateDatabase() {
        val databaseCreateQuery =
            "CREATE DATABASE IF NOT EXISTS `${settings.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS pillars_stats (
                name VARCHAR(255) NOT NULL PRIMARY KEY,
                kills INT DEFAULT 0,
                wins INT DEFAULT 0,
                xp INT DEFAULT 0,
                level INT DEFAULT 1,
                playedGame INT DEFAULT 0,
                bestWinStreak INT DEFAULT 0,
                currentWinStreak INT DEFAULT 0
            )
        """.trimIndent()

        try {
            createHikariDataSource(jdbcUrl(includeDatabase = false)).use { tempDataSource ->
                tempDataSource.connection.use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeUpdate(databaseCreateQuery)
                    }
                }
            }
            Logger.info("Database '${settings.database}' creato/verificato con successo")
        } catch (e: Exception) {
            markOffline("Errore nella creazione del database: ${e.message}")
            return
        }

        try {
            dataSource = createHikariDataSource(jdbcUrl(includeDatabase = true))
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(createTableQuery)
                }
            }
            Logger.info("Tabella 'pillars_stats' creata/verificata con successo")
            isOnline = true
            lastError = null
        } catch (e: Exception) {
            markOffline("Errore nella creazione della tabella pillars_stats: ${e.message}")
        }
    }

    fun createUser(player: Player) {
        if (!isOnline) return
        val insertQuery = "INSERT IGNORE INTO pillars_stats (name) VALUES (?)"
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(insertQuery).use { stmt ->
                    stmt.setString(1, player.name)
                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Database error adding user stats: ${e.message}")
        }
    }

    fun updateStatistics(
        playerName: String,
        kills: Int,
        wins: Int,
        xp: Int,
        level: Int,
        playedGame: Int,
        bestWinStreak: Int,
        currentWinStreak: Int
    ) {
        if (!isOnline) return
        val query = """
            INSERT INTO pillars_stats (name, kills, wins, xp, level, playedGame, bestWinStreak, currentWinStreak)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                kills = ?, wins = ?, xp = ?, level = ?,
                playedGame = ?, bestWinStreak = ?, currentWinStreak = ?
        """.trimIndent()

        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.setInt(2, kills)
                    stmt.setInt(3, wins)
                    stmt.setInt(4, xp)
                    stmt.setInt(5, level)
                    stmt.setInt(6, playedGame)
                    stmt.setInt(7, bestWinStreak)
                    stmt.setInt(8, currentWinStreak)

                    stmt.setInt(9, kills)
                    stmt.setInt(10, wins)
                    stmt.setInt(11, xp)
                    stmt.setInt(12, level)
                    stmt.setInt(13, playedGame)
                    stmt.setInt(14, bestWinStreak)
                    stmt.setInt(15, currentWinStreak)

                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Database error updating statistics: ${e.message}")
        }
    }

    fun getStatistics(playerName: String): PlayerStats? {
        if (!isOnline) return null
        val query = "SELECT kills, wins, xp, level, playedGame, bestWinStreak, currentWinStreak FROM pillars_stats WHERE name = ? LIMIT 1"
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            return PlayerStats(
                                name = playerName,
                                kills = rs.getInt("kills"),
                                wins = rs.getInt("wins"),
                                xp = rs.getInt("xp"),
                                level = rs.getInt("level"),
                                playedGame = rs.getInt("playedGame"),
                                bestWinStreak = rs.getInt("bestWinStreak"),
                                currentWinStreak = rs.getInt("currentWinStreak")
                            )
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Database problem fetching statistics: ${e.message}")
        }
        return null
    }

    data class PlayerStats(
        val name: String,
        var kills: Int = 0,
        var wins: Int = 0,
        var xp: Int = 0,
        var level: Int = 0,
        var playedGame: Int = 0,
        var bestWinStreak: Int = 0,
        var currentWinStreak: Int = 0
    )
}
