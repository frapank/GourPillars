package org.gourmet.gourPillars.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.other.Logger
import java.util.concurrent.CompletableFuture

class MySQLDatabase private constructor(
    private val settings: DatabaseSettings,
) : AsyncSqlDatabase(settings.poolSize) {
    constructor(config: YamlConfiguration) : this(loadSettings(config))

    private data class DatabaseSettings(
        val host: String,
        val port: Int,
        val database: String,
        val username: String,
        val password: String,
        val useSsl: Boolean,
        val allowPublicKeyRetrieval: Boolean,
        val poolSize: Int,
    )

    private companion object {
        private val IDENTIFIER_REGEX = Regex("^[A-Za-z0-9_]+$")

        private fun loadSettings(config: YamlConfiguration): DatabaseSettings {
            val host =
                config.getString("host")?.trim()?.takeIf { it.isNotBlank() }
                    ?: invalid("host", "localhost")

            val port =
                config
                    .getInt("port", 3306)
                    .takeIf { it in 1..65535 } ?: invalid("port", 3306)

            val database =
                config
                    .getString("database")
                    ?.trim()
                    ?.takeIf { IDENTIFIER_REGEX.matches(it) } ?: invalid("database", "gourpillars")

            val username =
                config
                    .getString("username")
                    ?.trim()
                    ?.takeIf { IDENTIFIER_REGEX.matches(it) } ?: invalid("username", "root")

            val password = config.getString("password") ?: ""

            val useSsl = config.getBoolean("use-ssl", false)
            val allowPublicKeyRetrieval = config.getBoolean("allow-public-key-retrieval", true)

            val poolSize =
                config
                    .getInt("pool-size", 10)
                    .takeIf { it in 1..50 } ?: invalid("pool-size", 10)

            return DatabaseSettings(host, port, database, username, password, useSsl, allowPublicKeyRetrieval, poolSize)
        }

        private fun <T> invalid(
            field: String,
            default: T,
        ): T {
            Logger.warning("Missing or invalid value for '$field' in database.yml, using default: $default")
            return default
        }
    }

    private var dataSource: HikariDataSource? = null

    init {
        checkAndCreateDatabase()
    }

    private fun jdbcUrl(includeDatabase: Boolean): String {
        val path = if (includeDatabase) "/${settings.database}" else "/"
        return "jdbc:mysql://${settings.host}:${settings.port}$path" +
            "?useSSL=${settings.useSsl}&allowPublicKeyRetrieval=${settings.allowPublicKeyRetrieval}&characterEncoding=UTF-8"
    }

    private fun createHikariDataSource(jdbcUrl: String): HikariDataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        hikariConfig.username = settings.username
        hikariConfig.password = settings.password
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        hikariConfig.maximumPoolSize = settings.poolSize
        // Fail fast on startup instead of blocking the main thread for Hikari's 30s default.
        hikariConfig.connectionTimeout = 5000
        return HikariDataSource(hikariConfig)
    }

    private fun checkAndCreateDatabase() {
        val databaseCreateQuery =
            "CREATE DATABASE IF NOT EXISTS `${settings.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

        val createTableQuery =
            """
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
            Logger.info("Database '${settings.database}' created/verified successfully")
        } catch (e: Exception) {
            markOffline("Error creating the database: ${e.message}")
            return
        }

        try {
            val newDataSource = createHikariDataSource(jdbcUrl(includeDatabase = true))
            newDataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(createTableQuery)
                }
            }
            dataSource = newDataSource
            Logger.info("Table 'pillars_stats' created/verified successfully")
            markOnline()
        } catch (e: Exception) {
            markOffline("Error creating the pillars_stats table: ${e.message}")
        }
    }

    override fun createUser(playerName: String): CompletableFuture<Void?> {
        if (!isOnline) return CompletableFuture.completedFuture(null)
        val source = dataSource ?: return CompletableFuture.completedFuture(null)
        return async(null, "Database error adding user stats") {
            val insertQuery = "INSERT IGNORE INTO pillars_stats (name) VALUES (?)"
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
        playerName: String,
        errorMessage: String,
    ): CompletableFuture<Void?> {
        if (!isOnline) return CompletableFuture.completedFuture(null)
        val source = dataSource ?: return CompletableFuture.completedFuture(null)
        return async(null, errorMessage) {
            source.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.executeUpdate()
                }
            }
            null
        }
    }

    private fun runUpdate(
        query: String,
        intParam: Int,
        playerName: String,
        errorMessage: String,
    ): CompletableFuture<Void?> {
        if (!isOnline) return CompletableFuture.completedFuture(null)
        val source = dataSource ?: return CompletableFuture.completedFuture(null)
        return async(null, errorMessage) {
            source.connection.use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setInt(1, intParam)
                    stmt.setString(2, playerName)
                    stmt.executeUpdate()
                }
            }
            null
        }
    }

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
                bestWinStreak = GREATEST(bestWinStreak, currentWinStreak + 1)
            WHERE name = ?
            """.trimIndent(),
            playerName,
            "Database error updating win streak",
        )

    override fun resetWinStreak(playerName: String): CompletableFuture<Void?> =
        runUpdate(
            """
            UPDATE pillars_stats
            SET bestWinStreak = GREATEST(bestWinStreak, currentWinStreak),
                currentWinStreak = 0
            WHERE name = ?
            """.trimIndent(),
            playerName,
            "Database error resetting win streak",
        )

    override fun incrementXp(
        playerName: String,
        amount: Int,
    ): CompletableFuture<Void?> =
        runUpdate("UPDATE pillars_stats SET xp = xp + ? WHERE name = ?", amount, playerName, "Database error updating xp")

    override fun setLevel(
        playerName: String,
        level: Int,
    ): CompletableFuture<Void?> =
        runUpdate("UPDATE pillars_stats SET level = ? WHERE name = ?", level, playerName, "Database error updating level")

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
