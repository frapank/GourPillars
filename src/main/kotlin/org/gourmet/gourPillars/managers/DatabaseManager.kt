package org.gourmet.gourPillars.managers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.other.Logger
import java.sql.SQLException

class DatabaseManager {
    private val url: String =
        "jdbc:mysql://localhost:3306/dream?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
    private val user: String = "root"
    private val pass: String = "dream_db"

    private lateinit var dataSource: HikariDataSource

    val playersStats = HashMap<Player, PlayerStats>()
    var isOnline = false

    init {
        checkAndCreateDatabase()
    }

    private fun createHikariDataSource(jdbcUrl: String): HikariDataSource {

        val config = HikariConfig()
        config.jdbcUrl = jdbcUrl
        config.username = user
        config.password = pass
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.maximumPoolSize = 10
        return HikariDataSource(config)
    }

    private fun checkAndCreateDatabase() {
        val baseUrl =
            "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
        val databaseCreateQuery =
            "CREATE DATABASE IF NOT EXISTS dream CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

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
            val tempDataSource = createHikariDataSource(baseUrl)
            tempDataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(databaseCreateQuery)
                    Logger.info("Database 'dream' creato/verificato con successo")
                }
            }
            tempDataSource.close()
        } catch (e: SQLException) {
            Logger.warning("Errore nella creazione del database: ${e.message}")
            isOnline = false
            return
        }

        try {
            dataSource = createHikariDataSource(url)
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(createTableQuery)
                    Logger.info("Tabella 'pillars_stats' creata/verificata con successo")
                    isOnline = true
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Errore nella creazione della tabella pillars_stats: ${e.message}")
            isOnline = false
        }
    }

    fun createUser(player: Player) {
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
