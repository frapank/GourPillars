package org.gourmet.gourPillars.managers

import org.bukkit.entity.Player
import org.gourmet.gourPillars.other.Logger
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseManager {
    private val url: String =
        "jdbc:mysql://localhost:3306/luckofpillars?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
    private val user: String = "root"
    private val pass: String? = "dream_db"
    val playersData = HashMap<Player, PlayerData>()
    var isOnline = false

    fun setupDatabase() {
        if (pass == null) return

        val baseUrl =
            "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
        val createDatabaseQuery =
            "CREATE DATABASE IF NOT EXISTS luckofpillars CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

        val createPlayerTableQuery =
            "CREATE TABLE IF NOT EXISTS players (" +
                    "name VARCHAR(255) NOT NULL PRIMARY KEY, " +
                    "fragments INT DEFAULT 0" +
                    ")"

        val createStatisticsTableQuery = (
                "CREATE TABLE IF NOT EXISTS statistics (" +
                        "name VARCHAR(255) NOT NULL PRIMARY KEY, " +
                        "kills INT DEFAULT 0, " +
                        "wins INT DEFAULT 0, " +
                        "xp INT DEFAULT 0, " +
                        "level INT DEFAULT 0, " +
                        "playedGame INT DEFAULT 0, " +
                        "bestWinStreak INT DEFAULT 0, " +
                        "currentWinStreak INT DEFAULT 0, " +
                        "FOREIGN KEY(name) REFERENCES players(name) ON DELETE CASCADE" +
                        ")"
                )

        val alterAddPlayedGame =
            "ALTER TABLE statistics ADD COLUMN IF NOT EXISTS playedGame INT DEFAULT 0"
        val alterAddBestWinStreak =
            "ALTER TABLE statistics ADD COLUMN IF NOT EXISTS bestWinStreak INT DEFAULT 0"
        val alterAddCurrentWinStreak =
            "ALTER TABLE statistics ADD COLUMN IF NOT EXISTS currentWinStreak INT DEFAULT 0"

        try {
            DriverManager.getConnection(baseUrl, user, pass).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(createDatabaseQuery)
                    Logger.info("Database 'luckofpillars' created/checked successfully")
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Errore nella creazione del database: ${e.message}")
            isOnline = false
            return
        }

        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeUpdate(createPlayerTableQuery)
                    stmt.executeUpdate(createStatisticsTableQuery)
                    stmt.executeUpdate(alterAddPlayedGame)
                    stmt.executeUpdate(alterAddBestWinStreak)
                    stmt.executeUpdate(alterAddCurrentWinStreak)
                    Logger.info("Tables 'players' e 'statistics' create/controllate con nuovi campi")
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Errore nella creazione o modifica delle tabelle: ${e.message}")
            isOnline = false
        }
    }

    fun initDatabase() {
        if (pass == null) return

        val query =
            "SELECT p.name, p.fragments, s.kills, s.wins, s.xp, s.level, s.playedGame, s.bestWinStreak, s.currentWinStreak " +
                    "FROM players p LEFT JOIN statistics s ON p.name = s.name"

        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(query).use { _ ->
                        Logger.info("Database connection established")
                        isOnline = true
                    }
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Problem with the database: ${e.message}")
            isOnline = false
        }
    }

    fun createUser(player: Player) {
        val insertPlayer = "INSERT IGNORE INTO players (name) VALUES (?)"
        val insertStats = "INSERT IGNORE INTO statistics (name) VALUES (?)"

        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(insertPlayer).use { stmt1 ->
                    stmt1.setString(1, player.name)
                    stmt1.executeUpdate()
                }
                conn.prepareStatement(insertStats).use { stmt2 ->
                    stmt2.setString(1, player.name)
                    stmt2.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Database error adding user and stats: ${e.message}")
        }
    }

    fun updateFragments(playerName: String, newFragments: Int) {
        val query = "UPDATE players SET fragments = ? WHERE name = ?"
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setInt(1, newFragments)
                    stmt.setString(2, playerName)
                    if (stmt.executeUpdate() > 0) Logger.info("Fragments updated for $playerName")
                    else Logger.warning("No player found with name: $playerName")
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Database error updating fragments: ${e.message}")
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
        val query =
            "INSERT INTO statistics (name, kills, wins, xp, level, playedGame, bestWinStreak, currentWinStreak) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE kills = ?, wins = ?, xp = ?, level = ?, " +
                    "playedGame = ?, bestWinStreak = ?, currentWinStreak = ?"

        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
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
        val query = "SELECT kills, wins, xp, level, playedGame, bestWinStreak, currentWinStreak " +
                "FROM statistics WHERE name = ? LIMIT 1"
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            return PlayerStats(
                                playerName,
                                rs.getInt("kills"),
                                rs.getInt("wins"),
                                rs.getInt("xp"),
                                rs.getInt("level"),
                                rs.getInt("playedGame"),
                                rs.getInt("bestWinStreak"),
                                rs.getInt("currentWinStreak")
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

    fun getFragmentByName(playerName: String): Int {
        val query = "SELECT fragments FROM players WHERE name = ? LIMIT 1"
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) return rs.getInt("fragments")
                    }
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Database problem, can't find user: ${e.message}")
        }
        return 0
    }

    class PlayerStats(
        val name: String,
        var kills: Int,
        var wins: Int,
        var xp: Int,
        var level: Int,
        var playedGame: Int,
        var bestWinStreak: Int,
        var currentWinStreak: Int
    )

    class PlayerData(player: Player, databaseManager: DatabaseManager) {
        val name: String = player.name
        var fragments: Int = databaseManager.getFragmentByName(name)
        var stats: PlayerStats? = databaseManager.getStatistics(name)
    }
}
