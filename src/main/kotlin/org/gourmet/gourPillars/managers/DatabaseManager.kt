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
    private val playersData = HashMap<Player, PlayerData>()
    var isOnline = false

    fun setupDatabase() {
        if (pass == null) return

        val baseUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
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
                        "deaths INT DEFAULT 0, " +
                        "defeats INT DEFAULT 0, " +
                        "kills DOUBLE DEFAULT 0.0, " +
                        "wins INT DEFAULT 0, " +
                        "gameplayed INT DEFAULT 0, " +
                        "xp INT DEFAULT 0, " +
                        "FOREIGN KEY(name) REFERENCES players(name) ON DELETE CASCADE" +
                        ")")

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
                    Logger.info("Tables 'players' and 'statistics' created/checked successfully")
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Errore nella creazione delle tabelle: ${e.message}")
            isOnline = false
        }
    }

    fun initDatabase() {
        if (pass == null) return

        val query =
            "SELECT p.name, p.fragments, s.deaths, s.defeats, s.kills, s.wins, s.gameplayed, s.xp " +
                    "FROM players p LEFT JOIN statistics s ON p.name = s.name"

        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(query).use { rs ->
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
                Logger.info("User and initial statistics created for ${player.name}")
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
        deaths: Int,
        defeats: Int,
        kills: Double,
        wins: Int,
        gameplayed: Int,
        xp: Int
    ) {
        val query =
            "INSERT INTO statistics (name, deaths, defeats, kills, wins, gameplayed, xp) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE deaths = ?, defeats = ?, kills = ?, wins = ?, gameplayed = ?, xp = ?"

        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.setInt(2, deaths)
                    stmt.setInt(3, defeats)
                    stmt.setDouble(4, kills)
                    stmt.setInt(5, wins)
                    stmt.setInt(6, gameplayed)
                    stmt.setInt(7, xp)

                    stmt.setInt(8, deaths)
                    stmt.setInt(9, defeats)
                    stmt.setDouble(10, kills)
                    stmt.setInt(11, wins)
                    stmt.setInt(12, gameplayed)
                    stmt.setInt(13, xp)
                    stmt.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Logger.warning("Database error updating statistics: ${e.message}")
        }
    }

    fun getStatistics(playerName: String): PlayerStats? {
        val query = "SELECT deaths, defeats, kills, wins, gameplayed, xp FROM statistics WHERE name = ? LIMIT 1"
        try {
            DriverManager.getConnection(url, user, pass).use { conn ->
                conn.prepareStatement(query).use { stmt ->
                    stmt.setString(1, playerName)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            return PlayerStats(
                                playerName,
                                rs.getInt("deaths"),
                                rs.getInt("defeats"),
                                rs.getDouble("kills"),
                                rs.getInt("wins"),
                                rs.getInt("gameplayed"),
                                rs.getInt("xp")
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
        return -1
    }

    fun decreaseInfamyForAll(amount: Int) {
        // No longer applicable in current stats model
    }

    class PlayerStats(
        val name: String,
        var deaths: Int,
        var defeats: Int,
        var kills: Double,
        var wins: Int,
        var gameplayed: Int,
        var xp: Int
    )

    class PlayerData(player: Player, databaseManager: DatabaseManager) {
        val name: String = player.name
        var fragments: Int = databaseManager.getFragmentByName(name)
        var stats: PlayerStats? = databaseManager.getStatistics(name)
    }
}
