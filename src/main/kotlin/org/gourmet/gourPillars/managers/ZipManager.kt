package org.gourmet.gourPillars.managers

import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipManager {
    private val backupFolder = File(GourPillars.instance.dataFolder, "backups").apply { mkdirs() }

    fun restoreBackup(
        worldName: String,
        onComplete: () -> Unit = {},
    ) {
        val backupFile = File(backupFolder, "$worldName-backup.zip")
        val worldFolder = File(Bukkit.getWorldContainer(), worldName)

        if (!backupFile.exists()) {
            Logger.warning("No backup found for $worldName!")
            onComplete()
            return
        }

        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            world.players.forEach { player -> GourPillars.spawnManager.teleportPlayerToSpawn(player) }
            if (!Bukkit.unloadWorld(world, false)) {
                Logger.warning("Could not unload world '$worldName' (still occupied?), skipping this reset")
                onComplete()
                return
            }
        }

        object : BukkitRunnable() {
            override fun run() {
                worldFolder.deleteRecursively()
                Thread.sleep(1000)

                ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        val file = File(worldFolder, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile.mkdirs()
                            file.outputStream().use { zipIn.copyTo(it) }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                File(worldFolder, "session.lock").delete()

                if (!File(worldFolder, "level.dat").exists()) {
                    Logger.warning("Warning: level.dat missing! The world might not load correctly.")
                }

                object : BukkitRunnable() {
                    override fun run() {
                        val newWorld = Bukkit.createWorld(WorldCreator(worldName))
                        val spawn = newWorld?.spawnLocation

                        if (newWorld != null && spawn != null) {
                            val chunkX = spawn.blockX / 16
                            val chunkZ = spawn.blockZ / 16
                            for (x in -2..2) {
                                for (z in -2..2) {
                                    newWorld.loadChunk(chunkX + x, chunkZ + z)
                                }
                            }
                            newWorld.keepSpawnInMemory = true
                            newWorld.isAutoSave = false
                            newWorld.save()
                        }

                        Logger.info("Backup of $worldName loaded!")
                        onComplete()
                    }
                }.runTask(GourPillars.instance)
            }
        }.runTaskAsynchronously(GourPillars.instance)
    }

    fun saveBackup(worldName: String) {
        val worldFolder = File(Bukkit.getWorldContainer(), worldName)
        if (!worldFolder.exists()) {
            Logger.warning("World $worldName does not exist!")
            return
        }

        val backupFile = File(backupFolder, "$worldName-backup.zip")
        if (backupFile.exists()) backupFile.delete()

        ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
            Files.walk(worldFolder.toPath()).forEach { path ->
                val file = path.toFile()
                if (file.isDirectory) return@forEach

                val entryName = worldFolder.toPath().relativize(path).toString()
                zipOut.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }

        Logger.info("Backup of $worldName saved to ${backupFile.absolutePath}")
    }
}
