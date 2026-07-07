package org.gourmet.gourPillars.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.File
import java.util.concurrent.CompletableFuture

class SQLiteDatabase(
    config: YamlConfiguration,
) : AsyncSqlDatabase(poolSize = 1) {
    private val file =
        File(
            GourPillars.instance.dataFolder,
            config.getString("sqlite.file")?.trim()?.takeIf { it.isNotBlank() } ?: "database.db",
        )
    private var dataSource: HikariDataSource? = null

    init {
        checkAndCreateDatabase()
    }

    private fun createHikariDataSource(): HikariDataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
        hikariConfig.driverClassName = "org.sqlite.JDBC"
        // SQLite serializes writes at the file level, a single pooled connection avoids
        // "database is locked" errors under concurrent access.
        hikariConfig.maximumPoolSize = 1
        hikariConfig.connectionTimeout = 5000
        return HikariDataSource(hikariConfig)
    }

    private fun checkAndCreateDatabase() {
        val createTableQuery =
            """
            CREATE TABLE IF NOT EXISTS pillars_stats (
                name TEXT NOT NULL PRIMARY KEY,
                kills INTEGER DEFAULT 0,
                wins INTEGER DEFAULT 0,
                xp INTEGER DEFAULT 0,
                level INTEGER DEFAULT 1,
                playedGame INTEGER DEFAULT 0,
                bestWinStreak INTEGER DEFAULT 0,
                currentWinStreak INTEGER DEFAULT 0
            )
            """.trimIndent()

        try {
            file.parentFile?.mkdirs()
            val newDataSource = createHikariDataSource()
            newDataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(createTableQuery)
                }
            }
            dataSource = newDataSource
            Logger.info("SQLite database '${file.name}' created/verified successfully")
            markOnline()
        } catch (e: Exception) {
            markOffline("Error creating the pillars_stats table: ${e.message}")
        }
    }

    override fun createUser(playerName: String): CompletableFuture<Void?> {
        if (!isOnline) return CompletableFuture.completedFuture(null)
        val source = dataSource ?: return CompletableFuture.completedFuture(null)
        return async(null, "Database error adding user stats") {
            val insertQuery = "INSERT OR IGNORE INTO pillars_stats (name) VALUES (?)"
            source.connection.use { conn ->
                conn.prepareStatement(insertQuery).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.executeUpdate()
                }
            }
            null
        }
    }

    override fun updateStatistics(
        playerName: String,
        kills: Int,
        wins: Int,
        xp: Int,
        level: Int,
        playedGame: Int,
        bestWinStreak: Int,
        currentWinStreak: Int,
    ): CompletableFuture<Void?> {
        if (!isOnline) return CompletableFuture.completedFuture(null)
        val source = dataSource ?: return CompletableFuture.completedFuture(null)
        return async(null, "Database error updating statistics") {
            val query =
                """
                INSERT OR REPLACE INTO pillars_stats (name, kills, wins, xp, level, playedGame, bestWinStreak, currentWinStreak)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

            source.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.setInt(2, kills)
                    stmt.setInt(3, wins)
                    stmt.setInt(4, xp)
                    stmt.setInt(5, level)
                    stmt.setInt(6, playedGame)
                    stmt.setInt(7, bestWinStreak)
                    stmt.setInt(8, currentWinStreak)
                    stmt.executeUpdate()
                }
            }
            null
        }
    }

    override fun getStatistics(playerName: String): CompletableFuture<PlayerStats?> {
        if (!isOnline) return CompletableFuture.completedFuture(null)
        val source = dataSource ?: return CompletableFuture.completedFuture(null)
        return async(null, "Database problem fetching statistics") {
            val query =
                "SELECT kills, wins, xp, level, playedGame, bestWinStreak, currentWinStreak " +
                    "FROM pillars_stats WHERE name = ? LIMIT 1"
            source.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            PlayerStats(
                                name = playerName,
                                kills = rs.getInt("kills"),
                                wins = rs.getInt("wins"),
                                xp = rs.getInt("xp"),
                                level = rs.getInt("level"),
                                playedGame = rs.getInt("playedGame"),
                                bestWinStreak = rs.getInt("bestWinStreak"),
                                currentWinStreak = rs.getInt("currentWinStreak"),
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        super.close()
        dataSource?.close()
    }
}
