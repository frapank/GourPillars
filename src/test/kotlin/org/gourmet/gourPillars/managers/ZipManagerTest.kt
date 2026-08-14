package org.gourmet.gourPillars.managers

import org.gourmet.gourPillars.GourPillars
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockbukkit.mockbukkit.MockBukkit
import java.io.File
import java.util.zip.ZipFile

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipManagerTest {
    @BeforeAll
    fun setUpAll() {
        MockBukkit.mock()
        MockBukkit.createMockPlugin("PlaceholderAPI")
        MockBukkit.load(GourPillars::class.java)
    }

    @AfterAll
    fun tearDownAll() {
        MockBukkit.unmock()
    }

    private fun sampleWorld(root: File): File {
        val world = File(root, "arena-world").apply { mkdirs() }
        File(world, "level.dat").writeText("level")
        File(world, "region").mkdirs()
        File(world, "region/r.0.0.mca").writeText("region data")
        // Held open by the server while the world is loaded: on Windows it can't even be opened.
        File(world, "session.lock").writeText("lock")
        return world
    }

    private fun entryNames(zip: File): Set<String> =
        ZipFile(zip).use {
            it
                .entries()
                .toList()
                .map { entry -> entry.name }
                .toSet()
        }

    @Test
    fun `the world files end up in the zip, the lock file does not`(
        @TempDir root: File,
    ) {
        val world = sampleWorld(root)
        val target = File(root, "backup.zip")

        assertTrue(ZipManager().zipFolder(world, target))

        val names = entryNames(target)
        assertTrue(names.contains("level.dat"))
        assertTrue(names.contains("region/r.0.0.mca"), "nested entries must be '/'-separated, names were $names")
        assertFalse(names.contains("session.lock"), "the file the server keeps locked must be skipped")
    }

    @Test
    fun `a file that cannot be read is skipped instead of failing the whole backup`(
        @TempDir root: File,
    ) {
        val world = sampleWorld(root)
        val unreadable = File(world, "playerdata/locked.dat")
        unreadable.parentFile.mkdirs()
        unreadable.writeText("data")
        // A directory where a file is expected fails to open exactly like a Windows-locked file does.
        assertTrue(unreadable.delete() && unreadable.mkdir())

        val target = File(root, "backup.zip")

        assertTrue(ZipManager().zipFolder(world, target), "one unreadable file must not lose the whole snapshot")
        assertTrue(entryNames(target).contains("level.dat"))
    }

    @Test
    fun `the backup content survives a zip round trip`(
        @TempDir root: File,
    ) {
        val world = sampleWorld(root)
        val target = File(root, "backup.zip")
        ZipManager().zipFolder(world, target)

        val restored = File(root, "restored").apply { mkdirs() }
        ZipFile(target).use { zip ->
            zip.entries().toList().forEach { entry ->
                val file = File(restored, entry.name)
                file.parentFile.mkdirs()
                zip.getInputStream(entry).use { input -> file.outputStream().use { input.copyTo(it) } }
            }
        }

        assertEquals("level", File(restored, "level.dat").readText())
        assertEquals("region data", File(restored, "region/r.0.0.mca").readText())
    }
}
