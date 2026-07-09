package org.gourmet.gourPillars.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.File
import java.sql.PreparedStatement
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

    private fun runUpdate(
        query: String,
        errorMessage: String,
        bind: (PreparedStatement) -> Unit,
    ): CompletableFuture<Void?> {
        if (!isOnline) return CompletableFuture.completedFuture(null)
        val source = dataSource ?: return CompletableFuture.completedFuture(null)
        return async(null, errorMessage) {
            source.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    bind(stmt)
                    stmt.executeUpdate()
                }
            }
            null
        }
    }

    private fun runUpdate(
        query: String,
        playerName: String,
        errorMessage: String,
    ): CompletableFuture<Void?> = runUpdate(query, errorMessage) { it.setString(1, playerName) }

    override fun incrementKills(playerName: String): CompletableFuture<Void?> =
        runUpdate("UPDATE pillars_stats SET kills = kills + 1 WHERE name = ?", playerName, "Database error updating kills")

    override fun incrementWins(playerName: String): CompletableFuture<Void?> =
        runUpdate("UPDATE pillars_stats SET wins = wins + 1 WHERE name = ?", playerName, "Database error updating wins")

    override fun incrementGamesPlayed(playerName: String): CompletableFuture<Void?> =
        runUpdate(
            "UPDATE pillars_stats SET playedGame = playedGame + 1 WHERE name = ?",
            playerName,
            "Database error updating playedGame",
        )

    override fun incrementWinStreak(playerName: String): CompletableFuture<Void?> =
        runUpdate(
            """
            UPDATE pillars_stats
            SET currentWinStreak = currentWinStreak + 1,
                bestWinStreak = MAX(bestWinStreak, currentWinStreak + 1)
            WHERE name = ?
            """.trimIndent(),
            playerName,
            "Database error updating win streak",
        )

    override fun resetWinStreak(playerName: String): CompletableFuture<Void?> =
        runUpdate(
            """
            UPDATE pillars_stats
            SET bestWinStreak = MAX(bestWinStreak, currentWinStreak),
                currentWinStreak = 0
            WHERE name = ?
            """.trimIndent(),
            playerName,
            "Database error resetting win streak",
        )

    override fun incrementXp(
        playerName: String,
        amount: Int,
        xpPerLevel: Int,
    ): CompletableFuture<Void?> =
        runUpdate(
            """
            UPDATE pillars_stats
            SET xp = xp + ?,
                level = MAX(level, 1 + (xp + ?) / ?)
            WHERE name = ?
            """.trimIndent(),
            "Database error updating xp",
        ) { stmt ->
            stmt.setInt(1, amount)
            stmt.setInt(2, amount)
            stmt.setInt(3, xpPerLevel)
            stmt.setString(4, playerName)
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
