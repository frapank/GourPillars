package org.gourmet.gourPillars.managers
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipManager {

    private val backupFolder = File(GourPillars.instance.dataFolder, "backups").apply { mkdirs() }

    fun restoreBackup(worldName: String) {
        val backupFile = File(backupFolder, "$worldName-backup.zip")
        val worldFolder = File(Bukkit.getWorldContainer(), worldName)

        if (!backupFile.exists()) {
            Bukkit.getLogger().warning("Nessun backup trovato per $worldName!")
            return
        }

        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            Bukkit.unloadWorld(world, false)
        }

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

        object : BukkitRunnable() {
            override fun run() {
                val wc = WorldCreator(worldName)
                val newWorld = Bukkit.createWorld(wc)
                val spawn = newWorld?.spawnLocation
                for (x in -2..2) {
                    for (z in -2..2) {
                        newWorld?.loadChunk(spawn?.blockX!! / 16 + x, spawn?.blockZ!! / 16 + z)
                    }
                }
                newWorld?.keepSpawnInMemory = true
                newWorld?.isAutoSave = false
                newWorld?.save()

                Bukkit.getLogger().info("Backup di $worldName caricato!")
            }
        }.runTaskLater(GourPillars.instance, 80L)

        if (!File(worldFolder, "level.dat").exists()) {
            Bukkit.getLogger().warning("Attenzione: level.dat mancante! Il mondo potrebbe non caricarsi correttamente.")
        }
    }

    fun saveBackup(worldName: String) {
        val worldFolder = File(Bukkit.getWorldContainer(), worldName)
        if (!worldFolder.exists()) {
            Bukkit.getLogger().warning("Il mondo $worldName non esiste!")
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

        Bukkit.getLogger().info("Backup di $worldName salvato in ${backupFile.absolutePath}")
    }
}
