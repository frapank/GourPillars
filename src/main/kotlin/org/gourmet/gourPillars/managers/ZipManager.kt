package org.gourmet.gourPillars.managers

import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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
                if (!deleteWorldFolder(worldFolder)) {
                    Logger.warning(
                        "Could not fully delete the folder of '$worldName' before restoring it: " +
                            "the server is still holding some of its files, so the reset may be incomplete",
                    )
                }

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

        // Flush the world first, or the snapshot can miss what was just built in it.
        if (Bukkit.isPrimaryThread()) Bukkit.getWorld(worldName)?.save()

        val backupFile = File(backupFolder, "$worldName-backup.zip")
        val pendingFile = File(backupFolder, "$worldName-backup.zip.tmp")

        if (!zipFolder(worldFolder, pendingFile)) {
            pendingFile.delete()
            Logger.warning("Backup of $worldName failed, the previous one was left untouched")
            return
        }

        if (backupFile.exists() && !backupFile.delete()) {
            pendingFile.delete()
            Logger.warning("Could not replace the existing backup of $worldName (is it open in another program?)")
            return
        }
        if (!pendingFile.renameTo(backupFile)) {
            pendingFile.delete()
            Logger.warning("Could not put the new backup of $worldName in place")
            return
        }

        Logger.info("Backup of $worldName saved to ${backupFile.absolutePath}")
    }

    /**
     * Zips [sourceFolder] into [target], returning whether it worked.
     *
     * Files the server keeps locked are skipped instead of failing the whole backup: on Windows
     * `session.lock` (and occasionally others) cannot be opened while the world is loaded, which
     * used to abort the zip and leave the arena without a snapshot to reset from. Linux lets those
     * same files be read, which is why the problem only ever showed up on Windows.
     *
     * The walk is closed explicitly for the same reason: a leaked directory stream keeps Windows
     * handles on the world folder open, and the reset later fails to delete it.
     */
    internal fun zipFolder(
        sourceFolder: File,
        target: File,
    ): Boolean {
        val sourcePath = sourceFolder.toPath()
        var skipped = 0

        try {
            ZipOutputStream(FileOutputStream(target)).use { zipOut ->
                Files.walk(sourcePath).use { paths ->
                    for (path in paths) {
                        val file = path.toFile()
                        if (file.isDirectory) continue
                        if (file.name in LOCKED_FILE_NAMES) continue

                        // Zip entries are '/'-separated by spec; Windows would otherwise write '\'.
                        val entryName = sourcePath.relativize(path).toString().replace(File.separatorChar, '/')
                        val input =
                            try {
                                file.inputStream()
                            } catch (e: IOException) {
                                skipped++
                                Logger.warning("Skipping locked file '$entryName' in the backup: ${e.message}")
                                continue
                            }
                        input.use {
                            zipOut.putNextEntry(ZipEntry(entryName))
                            it.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Logger.warning("Failed to zip ${sourceFolder.name}: ${e.message}")
            return false
        }

        if (skipped > 0) Logger.warning("$skipped file(s) were locked and left out of the backup of ${sourceFolder.name}")
        return true
    }

    /**
     * Deletes the world folder before the backup is unpacked over it. Windows releases the handles
     * of a just-unloaded world lazily, so a single attempt can leave files behind (and the arena
     * would keep whatever players built in it); retry a few times before giving up.
     */
    private fun deleteWorldFolder(folder: File): Boolean {
        repeat(DELETE_ATTEMPTS) {
            folder.deleteRecursively()
            if (!folder.exists()) return true
            Thread.sleep(DELETE_RETRY_MILLIS)
        }
        return !folder.exists()
    }

    private companion object {
        /** Held open by the server for as long as the world is loaded; never worth backing up. */
        val LOCKED_FILE_NAMES = setOf("session.lock")
        const val DELETE_ATTEMPTS = 3
        const val DELETE_RETRY_MILLIS = 500L
    }
}
