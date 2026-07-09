package org.gourmet.gourPillars.database

import org.bukkit.configuration.file.YamlConfiguration
import org.gourmet.gourPillars.GourPillars
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockbukkit.mockbukkit.MockBukkit
import java.io.File
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SQLiteDatabaseXpTest {
    private lateinit var database: SQLiteDatabase
    private lateinit var dbFile: File

    @BeforeAll
    fun setUpAll() {
        MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)

        val fileName = "xp-test-${System.nanoTime()}.db"
        val config = YamlConfiguration()
        config.set("sqlite.file", fileName)
        database = SQLiteDatabase(config)
        dbFile = File(GourPillars.instance.dataFolder, fileName)
    }

    @AfterAll
    fun tearDownAll() {
        database.close()
        MockBukkit.unmock()
    }

    private fun setLevelDirectly(
        playerName: String,
        level: Int,
    ) {
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.prepareStatement("UPDATE pillars_stats SET level = ? WHERE name = ?").use { stmt ->
                stmt.setInt(1, level)
                stmt.setString(2, playerName)
                stmt.executeUpdate()
            }
        }
    }

    @Test
    fun `incrementXp accumulates xp and levels up once the threshold is crossed`() {
        database.createUser("Alice").get()

        val first = database.incrementXp("Alice", 60, 100).get()
        assertEquals(60, first?.xp)
        assertEquals(1, first?.level)

        val second = database.incrementXp("Alice", 50, 100).get()
        assertEquals(110, second?.xp)
        assertEquals(2, second?.level)
    }

    @Test
    fun `incrementXp never regresses a level that's already higher than the computed one`() {
        database.createUser("Bob").get()
        // Simulate another server instance (sharing this database) having already bumped the
        // level ahead of what this instance's xp total alone would compute.
        setLevelDirectly("Bob", 9)

        val result = database.incrementXp("Bob", 1, 100).get()

        assertEquals(9, result?.level)
    }
}
