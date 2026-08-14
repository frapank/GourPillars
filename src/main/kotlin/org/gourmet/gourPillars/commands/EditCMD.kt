package org.gourmet.gourPillars.commands

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.ZipManager
import org.gourmet.gourPillars.managers.game.ArenaReloadResult
import org.gourmet.gourPillars.other.LocationAlignment
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.toMini
import org.gourmet.gourPillars.task.ArenaEditVisualizer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.io.File
import java.io.IOException
import java.util.UUID

private fun Location.loadedWorld(): World? = if (isWorldLoaded) world else null

data class ArenaEdit(
    val editorId: UUID,
    var name: String? = null,
    var minPlayers: Int? = null,
    var slowFallingTime: Int? = null,
    var spawnHeight: Int? = null,
    var privateArena: Boolean = false,
    var deathSpawn: Location? = null,
    var regionLocationOne: Location? = null,
    var regionLocationSecond: Location? = null,
    val locations: MutableMap<Int, Location> = sortedMapOf<Int, Location>(),
    var loadedFromFile: Boolean = false,
    var unsavedChanges: Boolean = false,
    var showMarkers: Boolean = true,
    var alignLocations: Boolean = true,
    var returnLocation: Location? = null,
    var previousGameMode: GameMode? = null,
) {
    val world: World?
        get() =
            locations.values.firstOrNull()?.loadedWorld()
                ?: regionLocationOne?.loadedWorld()
                ?: regionLocationSecond?.loadedWorld()
                ?: deathSpawn?.loadedWorld()

    val minHeight: Int?
        get() = withBothCorners { one, two -> minOf(one.blockY, two.blockY) }

    val maxHeight: Int?
        get() = withBothCorners { one, two -> maxOf(one.blockY, two.blockY) }

    fun isInsideRegion(location: Location): Boolean {
        val one = regionLocationOne ?: return true
        val two = regionLocationSecond ?: return true
        return location.blockX in minOf(one.blockX, two.blockX)..maxOf(one.blockX, two.blockX) &&
            location.blockY in minOf(one.blockY, two.blockY)..maxOf(one.blockY, two.blockY) &&
            location.blockZ in minOf(one.blockZ, two.blockZ)..maxOf(one.blockZ, two.blockZ)
    }

    private fun <T> withBothCorners(block: (Location, Location) -> T): T? {
        val one = regionLocationOne ?: return null
        val two = regionLocationSecond ?: return null
        return block(one, two)
    }
}

@Command("edit")
@CommandPermission("gpillars.admin")
object EditCMD {
    private const val PREFIX = "<dark_gray>[<gold>Arena<yellow>Editor<dark_gray>]<reset> "
    private const val LINE = "<dark_gray><strikethrough>                                        "
    private const val OK_MARK = "<green>✔"
    private const val WARN_MARK = "<yellow>⚠"
    private const val MISSING_MARK = "<red>✘"
    private const val STOP_CONFIRM_MILLIS = 15_000L
    private const val MARKER_INTERVAL_TICKS = 10L
    private const val DEFAULT_MIN_PLAYERS = 2
    private const val DEFAULT_SLOW_FALLING = 1
    private const val DEFAULT_SPAWN_HEIGHT = 0
    private val nameFormat = Regex("[A-Za-z0-9_-]{1,32}")

    private val arenaManager get() = GourPillars.arenaManager
    private val zipManager = ZipManager()
    private val sessions: MutableMap<UUID, ArenaEdit> = LinkedHashMap()
    private val pendingStopConfirm: MutableMap<UUID, Long> = HashMap()
    private var actionBarTask: BukkitTask? = null

    @Subcommand()
    fun editMain(player: Player) {
        val session = sessions[player.uniqueId]
        if (session == null) {
            player.info("No editing session running. Use <white>/edit startEditing</white> to create an arena.")
        } else {
            player.info("Editing <white>${session.displayName()}</white>. Use <white>/edit showStatus</white> to see what is missing.")
        }
        sendHelp(player)
    }

    @Subcommand("help")
    fun help(player: Player) = sendHelp(player)

    @Subcommand("startEditing", "start")
    fun startEditing(player: Player) {
        val running = sessions[player.uniqueId]
        if (running != null) {
            player.error("You are already editing <white>${running.displayName()}</white>.")
            player.info(
                "Use <white>/edit showStatus</white> to review it, <white>/edit saveArena</white> to keep it " +
                    "or <white>/edit stopEditing</white> to discard it.",
            )
            return
        }
        if (!canStart(player)) return

        openSession(player, ArenaEdit(player.uniqueId))
        player.success("Editing session started.")
        player.info("Stand where you want each element and use the commands below — <white>/edit showStatus</white> shows your progress.")
        player.info("Coloured markers show what you set, only to you:")
        sendMarkerLegend(player)
        sendHelp(player)
    }

    @Subcommand("loadArena <name>", "load <name>")
    fun loadExisting(
        player: Player,
        name: String,
    ) {
        if (sessions.containsKey(player.uniqueId)) {
            player.error("Finish your current session first (<white>/edit saveArena</white> or <white>/edit stopEditing</white>).")
            return
        }
        if (!canStart(player)) return
        if (!isValidName(player, name)) return

        val file = File(arenasFolder(), "$name.yml")
        if (!file.isFile) {
            player.error("No arena file named <white>$name</white>.")
            listArenaFiles(player)
            return
        }

        val session = readSessionFromFile(player, file) ?: return

        openSession(player, session)
        player.success("Loaded arena <white>$name</white> for editing.")
        sendToArena(player, session)
        printCheck(player, session)
    }

    @Subcommand("stopEditing", "stop")
    fun stopEditing(player: Player) {
        val session = requireSession(player) ?: return

        val now = System.currentTimeMillis()
        val confirmDeadline = pendingStopConfirm[player.uniqueId]
        if (session.unsavedChanges && (confirmDeadline == null || confirmDeadline < now)) {
            pendingStopConfirm[player.uniqueId] = now + STOP_CONFIRM_MILLIS
            player.warn("You have unsaved changes on <white>${session.displayName()}</white>.")
            player.info(
                "Run <white>/edit stopEditing</white> again within 15s to discard them, or <white>/edit saveArena</white> to keep them.",
            )
            return
        }

        closeSession(player.uniqueId)
        restoreEditor(player, session)
        player.success("Editing session closed, you are back where you started.")
    }

    @Subcommand("setName <name>", "name <name>")
    fun setName(
        player: Player,
        name: String,
    ) {
        val session = requireSession(player) ?: return
        if (!isValidName(player, name)) return

        session.name = name
        session.unsavedChanges = true
        player.success("Name set to <white>$name</white>.")

        if (!session.loadedFromFile && File(arenasFolder(), "$name.yml").isFile) {
            player.warn("An arena file named <white>$name</white> already exists — saving will overwrite it.")
            player.info("Use <white>/edit stopEditing</white> then <white>/edit loadArena $name</white> to edit that one instead.")
        }
    }

    @Subcommand("setMinPlayers <min>", "minplayers <min>")
    fun setMinPlayers(
        player: Player,
        min: Int,
    ) {
        val session = requireSession(player) ?: return
        if (min < 1) {
            player.error("The minimum player count has to be at least 1.")
            return
        }

        session.minPlayers = min
        session.unsavedChanges = true
        player.success("Minimum players set to <white>$min</white>.")
        if (session.locations.isNotEmpty() && min > session.locations.size) {
            player.warn(
                "Only ${session.locations.size} spawn(s) exist, so the match could never start. " +
                    "Add more spawns or lower this value.",
            )
        }
    }

    @Subcommand("setRegionOne", "pos1")
    fun setRegionOne(player: Player) {
        val session = requireSession(player) ?: return
        if (!isUsableWorld(player, session)) return

        session.regionLocationOne = player.location
        session.unsavedChanges = true
        player.success("First region corner set at <white>${player.location.pretty()}</white>.")
        reportRegion(player, session)
    }

    @Subcommand("setRegionTwo", "pos2")
    fun setRegionTwo(player: Player) {
        val session = requireSession(player) ?: return
        if (!isUsableWorld(player, session)) return

        session.regionLocationSecond = player.location
        session.unsavedChanges = true
        player.success("Second region corner set at <white>${player.location.pretty()}</white>.")
        reportRegion(player, session)
    }

    @Subcommand("setDeathSpawn")
    fun setDeathSpawn(player: Player) {
        val session = requireSession(player) ?: return
        if (!isUsableWorld(player, session)) return

        val aligned = alignFor(session, player.location)
        session.deathSpawn = aligned.location
        session.unsavedChanges = true
        player.success("Death spawn set at <white>${aligned.location.pretty()}</white>.")
        reportAlignment(player, aligned)
        player.info("Eliminated players and spectators are teleported here.")
    }

    @Subcommand("setSlowFalling <seconds>", "setFallingTime <seconds>")
    fun setFallingTime(
        player: Player,
        seconds: Int,
    ) {
        val session = requireSession(player) ?: return
        if (seconds < 0) {
            player.error("The slow falling duration can't be negative.")
            return
        }

        session.slowFallingTime = seconds
        session.unsavedChanges = true
        player.success("Slow falling set to <white>${seconds}s</white> at the start of the match.")
    }

    @Subcommand("setSpawnHeight <blocks>")
    fun setSpawnHeight(
        player: Player,
        blocks: Int,
    ) {
        val session = requireSession(player) ?: return
        if (blocks < 0) {
            player.error("The spawn height offset can't be negative.")
            return
        }

        session.spawnHeight = blocks
        session.unsavedChanges = true
        player.success("Glass cages will be placed <white>$blocks</white> block(s) above each spawn.")
    }

    @Subcommand("setPrivate <value>", "private <value>")
    fun setPrivate(
        player: Player,
        value: Boolean,
    ) {
        val session = requireSession(player) ?: return

        session.privateArena = value
        session.unsavedChanges = true
        if (value) {
            player.success("Arena marked as private — nobody will be able to join it.")
        } else {
            player.success("Arena marked as public.")
        }
    }

    @Subcommand("setSpawn <number>", "spawn <number>")
    fun setSpawn(
        player: Player,
        number: Int,
    ) {
        val session = requireSession(player) ?: return
        if (number < 1) {
            player.error("Spawn numbers start at 1.")
            return
        }
        if (!isUsableWorld(player, session)) return

        val aligned = alignFor(session, player.location)
        val spawnLocation = aligned.location
        val replaced = session.locations.put(number, spawnLocation) != null
        session.unsavedChanges = true

        if (replaced) {
            player.success("Spawn <white>$number</white> moved to <white>${spawnLocation.pretty()}</white>.")
        } else {
            player.success("Spawn <white>$number</white> added at <white>${spawnLocation.pretty()}</white>.")
        }
        reportAlignment(player, aligned)
        player.info("The arena now holds <white>${session.locations.size}</white> player(s).")

        if (!session.isInsideRegion(spawnLocation)) {
            player.warn("This spawn is outside the region: players there won't be able to place blocks.")
        }
    }

    @Subcommand("removeSpawn <number>", "delSpawn <number>")
    fun deleteSpawn(
        player: Player,
        number: Int,
    ) {
        val session = requireSession(player) ?: return

        if (session.locations.remove(number) == null) {
            player.error("No spawn numbered <white>$number</white>.")
            return
        }
        session.unsavedChanges = true
        player.success("Spawn <white>$number</white> removed, <white>${session.locations.size}</white> left.")
    }

    @Subcommand("listSpawns", "spawns")
    fun listSpawns(player: Player) {
        val session = requireSession(player) ?: return

        if (session.locations.isEmpty()) {
            player.error("No spawn set yet — stand on a pillar and run <white>/edit setSpawn 1</white>.")
            return
        }

        player.sendMessage(LINE.toMini())
        player.sendMessage("$PREFIX<gold>Spawns <dark_gray>(<white>${session.locations.size}<dark_gray>)".toMini())
        session.locations.forEach { (index, location) ->
            val outside = if (session.isInsideRegion(location)) "" else " <yellow>(outside region)"
            player.sendMessage("<dark_gray> ▪ <yellow>$index <dark_gray>→ <gray>${location.pretty()}$outside".toMini())
        }
        player.sendMessage(LINE.toMini())
    }

    @Subcommand("teleportToSpawn <number>", "tp <number>")
    fun teleportToSpawn(
        player: Player,
        number: Int,
    ) {
        val session = requireSession(player) ?: return

        val location =
            session.locations[number] ?: run {
                player.error("No spawn numbered <white>$number</white>.")
                return
            }
        player.teleport(location)
        player.success("Teleported to spawn <white>$number</white>.")
    }

    @Subcommand("teleportToArena", "tp")
    fun teleportToArena(player: Player) {
        val session = requireSession(player) ?: return

        val anchor =
            arenaAnchor(session) ?: run {
                player.error("Nothing is set yet, so there is nowhere to send you.")
                player.info("Fly to the arena world yourself and start with <white>/edit setSpawn 1</white>.")
                return
            }
        if (player.world == anchor.world) {
            player.info("You are already in <white>${anchor.world.name}</white> — sending you to the arena anyway.")
        }
        player.teleport(anchor)
        player.success("Teleported to <white>${anchor.pretty()}</white>.")
    }

    @Subcommand("showStatus", "check")
    fun checkArena(player: Player) {
        val session = requireSession(player) ?: return
        printCheck(player, session)
    }

    @Subcommand("toggleAlignment", "snap")
    fun toggleAlignment(player: Player) {
        val session = requireSession(player) ?: return

        session.alignLocations = !session.alignLocations
        if (session.alignLocations) {
            player.success("Alignment assist on: spots you set are centred on the block and squared up when you are close to a 45° angle.")
        } else {
            player.success("Alignment assist off: spots are stored exactly where and how you stand.")
        }
    }

    @Subcommand("toggleMarkers", "view")
    fun toggleMarkers(player: Player) {
        val session = requireSession(player) ?: return

        session.showMarkers = !session.showMarkers
        if (session.showMarkers) {
            player.success("Markers shown again.")
            sendMarkerLegend(player)
        } else {
            player.success("Markers hidden. Run <white>/edit toggleMarkers</white> to bring them back.")
        }
    }

    @Subcommand("saveArena", "save")
    fun saveArena(player: Player) {
        val session = requireSession(player) ?: return

        val problems = validate(session)
        if (problems.isNotEmpty()) {
            player.error("The arena can't be saved yet:")
            problems.forEach { problem -> player.sendMessage("<dark_gray> ▪ <red>${problem.escapeTags()}".toMini()) }
            player.info("Run <white>/edit showStatus</white> for the full status.")
            return
        }

        val name = session.name!!
        val world = session.world!!
        val minHeight = session.minHeight!!
        val maxHeight = session.maxHeight!!
        val minPlayers = session.minPlayers ?: DEFAULT_MIN_PLAYERS
        val slowFalling = session.slowFallingTime ?: DEFAULT_SLOW_FALLING
        val spawnHeight = session.spawnHeight ?: DEFAULT_SPAWN_HEIGHT

        player.info("Heights taken from the region: <white>Y $minHeight</white> → <white>Y $maxHeight</white>.")
        if (session.minPlayers == null) player.info("min-players not set → using the default (<white>$minPlayers</white>).")
        if (session.slowFallingTime == null) player.info("slow-falling-time not set → using the default (<white>${slowFalling}s</white>).")

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)

        val arenaConfig = YamlConfiguration()
        arenaConfig.set("world", world.name)
        arenaConfig.set("private-arena", session.privateArena)
        arenaConfig.set("min-players", minPlayers)
        arenaConfig.set("max-height", maxHeight)
        arenaConfig.set("min-height", minHeight)
        arenaConfig.set("slow-falling-time", slowFalling)
        arenaConfig.set("spawn-height", spawnHeight)

        arenaConfig.writeLocation("main-spawn", session.deathSpawn!!)
        arenaConfig.writeLocation("region.loc-1", session.regionLocationOne!!, withRotation = false)
        arenaConfig.writeLocation("region.loc-2", session.regionLocationSecond!!, withRotation = false)
        session.locations.forEach { (index, location) -> arenaConfig.writeLocation("spawns.$index", location) }

        try {
            val arenasFolder = arenasFolder().apply { mkdirs() }
            arenaConfig.save(File(arenasFolder, "$name.yml"))
        } catch (e: IOException) {
            Logger.warning("Failed to save arena '$name': ${e.message}")
            player.error("Failed to save arena <white>$name</white>: ${e.message}")
            return
        }

        session.unsavedChanges = false
        session.loadedFromFile = true
        player.success("Arena <white>$name</white> saved to <white>arenas/$name.yml</white>.")

        applyLiveReload(player, name)
        player.info("You are still editing <white>$name</white> — keep tweaking, or run <white>/edit stopEditing</white> when you're done.")

        try {
            zipManager.saveBackup(world.name)
        } catch (e: Exception) {
            Logger.warning("Failed to back up the world of arena '$name': ${e.message}")
            player.warn("The arena was saved, but the world backup failed — check the console.")
        }
    }

    private fun validate(session: ArenaEdit): List<String> {
        val problems = mutableListOf<String>()

        if (session.name.isNullOrEmpty()) problems += "no name — use /edit setName <name>"
        if (session.locations.isEmpty()) problems += "no spawn — stand on a pillar and use /edit setSpawn 1"
        if (session.deathSpawn == null) problems += "no death spawn — use /edit setDeathSpawn"
        if (session.regionLocationOne == null) problems += "no first region corner — use /edit setRegionOne"
        if (session.regionLocationSecond == null) problems += "no second region corner — use /edit setRegionTwo"

        val world = session.world
        if (world != null) {
            val strayLocations =
                buildList {
                    session.deathSpawn?.let { if (it.world != world) add("the death spawn") }
                    session.regionLocationOne?.let { if (it.world != world) add("the first region corner") }
                    session.regionLocationSecond?.let { if (it.world != world) add("the second region corner") }
                    if (session.locations.values.any { it.world != world }) add("some spawns")
                }
            if (strayLocations.isNotEmpty()) {
                problems += "${strayLocations.joinToString(", ")} are not in world '${world.name}' — an arena can only span one world"
            }
        }

        val minPlayers = session.minPlayers ?: DEFAULT_MIN_PLAYERS
        if (session.locations.isNotEmpty() && minPlayers > session.locations.size) {
            problems += "min-players ($minPlayers) is higher than the ${session.locations.size} spawn(s): the match could never start"
        }

        val minHeight = session.minHeight
        val maxHeight = session.maxHeight
        if (minHeight != null && maxHeight != null && minHeight >= maxHeight) {
            problems += "both region corners are at Y $minHeight: put one at the floor and one above the arena"
        }

        return problems
    }

    private fun applyLiveReload(
        player: Player,
        name: String,
    ) {
        when (arenaManager.reloadArena(name)) {
            ArenaReloadResult.RELOADED -> {
                player.success("Arena reloaded — it is joinable right now, no restart needed.")
            }

            ArenaReloadResult.BUSY -> {
                player.warn(
                    "The file is updated, but <white>$name</white> is busy with a match: it keeps the old " +
                        "settings until you save again once that match ends (or the server restarts).",
                )
            }

            ArenaReloadResult.INVALID -> {
                player.warn("The file was written but could not be loaded back — check the console for the reason.")
            }

            ArenaReloadResult.NOT_FOUND -> {
                player.warn("The file was written but could not be found again — check the console for the reason.")
            }
        }
    }

    private fun printCheck(
        player: Player,
        session: ArenaEdit,
    ) {
        val locations = session.locations
        val world = session.world

        player.sendMessage(LINE.toMini())
        player.sendMessage("$PREFIX<gold>${session.displayName()}".toMini())
        player.sendMessage(
            row("Name", session.name != null, session.name ?: "not set — /edit setName <name>"),
        )
        player.sendMessage(
            row("World", world != null, world?.name ?: "picked from the first location you set"),
        )
        player.sendMessage(
            row(
                "Spawns",
                locations.isNotEmpty(),
                if (locations.isEmpty()) "not set — /edit setSpawn 1" else "${locations.size} (max players: ${locations.size})",
            ),
        )
        player.sendMessage(
            rowWithDefault(
                "Min players",
                session.minPlayers?.toString(),
                "$DEFAULT_MIN_PLAYERS (default)",
            ),
        )
        player.sendMessage(
            row(
                "Region",
                session.regionLocationOne != null && session.regionLocationSecond != null,
                regionSummary(session),
            ),
        )
        player.sendMessage(
            row(
                "Death spawn",
                session.deathSpawn != null,
                session.deathSpawn?.pretty() ?: "not set — /edit setDeathSpawn",
            ),
        )
        player.sendMessage(derivedRow("Min height", session.minHeight, "region floor, players die below it"))
        player.sendMessage(derivedRow("Max height", session.maxHeight, "region ceiling"))
        player.sendMessage(
            rowWithDefault(
                "Slow falling",
                session.slowFallingTime?.let { "${it}s" },
                "${DEFAULT_SLOW_FALLING}s (default)",
            ),
        )
        player.sendMessage(
            rowWithDefault(
                "Spawn height",
                session.spawnHeight?.toString(),
                "$DEFAULT_SPAWN_HEIGHT (default)",
            ),
        )
        val privateLabel = "Private".padEnd(12)
        player.sendMessage("<dark_gray> ▪ <gray>$privateLabel<dark_gray>→ <white>${if (session.privateArena) "yes" else "no"}".toMini())

        if (session.showMarkers) sendMarkerLegend(player)

        val problems = validate(session)
        if (problems.isEmpty()) {
            player.sendMessage("$PREFIX<green>Ready to save <dark_gray>→ <white>/edit saveArena".toMini())
        } else {
            player.sendMessage("$PREFIX<red>Not ready yet <dark_gray>(<white>${problems.size}<dark_gray> problem(s))".toMini())
            problems.forEach { problem -> player.sendMessage("<dark_gray>   ▪ <red>${problem.escapeTags()}".toMini()) }
        }
        player.sendMessage(LINE.toMini())
    }

    private fun row(
        label: String,
        set: Boolean,
        value: String,
    ): Component {
        val mark = if (set) OK_MARK else MISSING_MARK
        val color = if (set) "<white>" else "<gray>"
        return "<dark_gray> $mark <gray>${label.padEnd(12)}<dark_gray>→ $color${value.escapeTags()}".toMini()
    }

    private fun derivedRow(
        label: String,
        value: Int?,
        source: String,
    ): Component =
        if (value != null) {
            "<dark_gray> $OK_MARK <gray>${label.padEnd(12)}<dark_gray>→ <aqua>$value <gray>(from the $source)".toMini()
        } else {
            "<dark_gray> $MISSING_MARK <gray>${label.padEnd(12)}<dark_gray>→ <gray>set the region, it comes with it".toMini()
        }

    private fun rowWithDefault(
        label: String,
        value: String?,
        fallback: String?,
    ): Component =
        when {
            value != null -> "<dark_gray> $OK_MARK <gray>${label.padEnd(12)}<dark_gray>→ <white>$value".toMini()
            fallback != null -> "<dark_gray> $WARN_MARK <gray>${label.padEnd(12)}<dark_gray>→ <aqua>$fallback".toMini()
            else -> "<dark_gray> $MISSING_MARK <gray>${label.padEnd(12)}<dark_gray>→ <gray>set the region first".toMini()
        }

    private fun sendMarkerLegend(player: Player) {
        player.sendMessage(
            (
                "<dark_gray> ▪ <gray>Markers <dark_gray>→ <color:#4FC3F7>region <dark_gray>· " +
                    "<color:#FF5252>min height <dark_gray>· <color:#FFC107>max height <dark_gray>· " +
                    "<color:#69F0AE>spawns <dark_gray>· <color:#E040FB>death spawn <dark_gray>(<white>/edit toggleMarkers<dark_gray>)"
            ).toMini(),
        )
    }

    private fun regionSummary(session: ArenaEdit): String {
        val one = session.regionLocationOne
        val two = session.regionLocationSecond
        return when {
            one != null && two != null -> "${one.prettyBlock()} → ${two.prettyBlock()}"
            one != null -> "only corner 1 set — /edit setRegionTwo"
            two != null -> "only corner 2 set — /edit setRegionOne"
            else -> "not set — /edit setRegionOne and /edit setRegionTwo"
        }
    }

    private fun startSessionTask() {
        if (actionBarTask?.isCancelled == false) return
        actionBarTask =
            Bukkit.getScheduler().runTaskTimer(
                GourPillars.instance,
                Runnable {
                    if (sessions.isEmpty()) {
                        stopSessionTask()
                        return@Runnable
                    }
                    sessions.forEach { (editorId, session) ->
                        val editor = Bukkit.getPlayer(editorId) ?: return@forEach
                        editor.sendActionBar(actionBar(session))
                        if (session.showMarkers) ArenaEditVisualizer.render(editor, session)
                    }
                },
                MARKER_INTERVAL_TICKS,
                MARKER_INTERVAL_TICKS,
            )
    }

    private fun stopSessionTask() {
        actionBarTask?.cancel()
        actionBarTask = null
    }

    private fun actionBar(session: ArenaEdit): Component {
        val name = session.name?.let { "<white>$it" } ?: "<red>unnamed"
        val spawns = if (session.locations.isEmpty()) "<red>0" else "<white>${session.locations.size}"
        val region = if (session.regionLocationOne != null && session.regionLocationSecond != null) OK_MARK else MISSING_MARK
        val death = if (session.deathSpawn != null) OK_MARK else MISSING_MARK
        val heights =
            if (session.minHeight == null) "<red>?" else "<aqua>${session.minHeight}-${session.maxHeight}"
        val unsaved = if (session.unsavedChanges) " <dark_gray>| <yellow>unsaved" else ""

        return (
            "<gold>✎ $name <dark_gray>| <gray>spawns $spawns <dark_gray>| <gray>region $region " +
                "<dark_gray>| <gray>death $death <dark_gray>| <gray>Y $heights$unsaved"
        ).toMini()
    }

    fun handleQuit(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        closeSession(player.uniqueId)
        restoreEditor(player, session, teleportBack = false)
        Logger.info("Closed the arena editing session of ${player.name} (disconnected)")
    }

    private fun openSession(
        player: Player,
        session: ArenaEdit,
    ) {
        session.returnLocation = player.location.clone()
        session.previousGameMode = player.gameMode
        sessions[player.uniqueId] = session
        GourPillars.isEditing = true
        startSessionTask()

        if (player.gameMode != GameMode.CREATIVE) {
            player.gameMode = GameMode.CREATIVE
            player.info("Put you in creative for the session; your ${session.previousGameMode?.name?.lowercase()} mode comes back on stop.")
        }
    }

    private fun restoreEditor(
        player: Player,
        session: ArenaEdit,
        teleportBack: Boolean = true,
    ) {
        session.previousGameMode?.let { previous ->
            if (player.gameMode != previous) player.gameMode = previous
        }
        if (!teleportBack) return

        val back = session.returnLocation
        if (back?.loadedWorld() != null) {
            player.teleport(back)
        } else {
            GourPillars.spawnManager.teleportPlayerToSpawn(player)
        }
    }

    private fun closeSession(editorId: UUID) {
        sessions.remove(editorId)
        pendingStopConfirm.remove(editorId)
        GourPillars.isEditing = sessions.isNotEmpty()
        if (sessions.isEmpty()) stopSessionTask()
    }

    private fun requireSession(player: Player): ArenaEdit? {
        val session = sessions[player.uniqueId]
        if (session == null) {
            player.error("You are not editing any arena.")
            player.info(
                "Use <white>/edit startEditing</white> for a new one, or <white>/edit loadArena \\<name></white> to change an existing one.",
            )
        }
        return session
    }

    private fun sendToArena(
        player: Player,
        session: ArenaEdit,
    ) {
        val world = session.world ?: return
        if (player.world == world) {
            player.info(
                "You are already in <white>${world.name}</white> — use <white>/edit teleportToArena</white> to jump to the arena itself.",
            )
            return
        }

        val anchor = arenaAnchor(session)
        if (anchor == null) {
            player.warn("That arena has no usable location to send you to; go to <white>${world.name}</white> yourself.")
            return
        }
        player.teleport(anchor)
        player.success("Teleported to <white>${world.name}</white>, at <white>${anchor.pretty()}</white>.")
    }

    private fun arenaAnchor(session: ArenaEdit): Location? {
        val world = session.world ?: return null
        return session.deathSpawn
            ?: session.locations.values.firstOrNull()
            ?: session.regionLocationOne
            ?: session.regionLocationSecond
            ?: world.spawnLocation
    }

    private fun alignFor(
        session: ArenaEdit,
        location: Location,
    ): LocationAlignment.Aligned =
        if (session.alignLocations) {
            LocationAlignment.align(location)
        } else {
            LocationAlignment.Aligned(location.clone(), emptyList())
        }

    private fun reportAlignment(
        player: Player,
        aligned: LocationAlignment.Aligned,
    ) {
        if (aligned.changes.isEmpty()) return
        player.info(
            "Nudged for you: ${aligned.changes.joinToString(", ")} <dark_gray>(<white>/edit toggleAlignment<dark_gray> to stop this)",
        )
    }

    private fun canStart(player: Player): Boolean {
        if (arenaManager.isPlayerInArena(player)) {
            player.error("You are in a match — use <white>/leave</white> first.")
            return false
        }
        if (arenaManager.isSpectating(player)) {
            player.error("You are spectating — use <white>/leave</white> first.")
            return false
        }
        return true
    }

    private fun isUsableWorld(
        player: Player,
        session: ArenaEdit,
    ): Boolean {
        val lobbyWorld = GourPillars.spawnManager.getConfiguredWorld()
        if (lobbyWorld != null && player.world == lobbyWorld) {
            player.error("You are in the lobby world (<white>${lobbyWorld.name}</white>) — arenas must live in their own world.")
            return false
        }

        val arenaWorld = session.world
        if (arenaWorld != null && arenaWorld != player.world) {
            player.error(
                "This arena is being built in <white>${arenaWorld.name}</white>, " +
                    "but you are in <white>${player.world.name}</white>.",
            )
            player.info(
                "An arena file stores a single world: go back to <white>${arenaWorld.name}</white>, " +
                    "or start over with <white>/edit stopEditing</white>.",
            )
            return false
        }
        return true
    }

    private fun isValidName(
        player: Player,
        name: String,
    ): Boolean {
        if (!nameFormat.matches(name)) {
            player.error("Invalid arena name <white>${name.escapeTags()}</white>.")
            player.info("Use 1-32 letters, digits, <white>-</white> or <white>_</white> (it becomes the file name).")
            return false
        }
        return true
    }

    private fun reportRegion(
        player: Player,
        session: ArenaEdit,
    ) {
        if (session.regionLocationOne == null || session.regionLocationSecond == null) {
            player.info("Now stand at the opposite corner and run the other <white>setRegion</white> command.")
            return
        }
        player.info("Region set: <white>${regionSummary(session)}</white>")
        player.info(
            "Heights come with it: players are eliminated below <white>Y ${session.minHeight}</white>, " +
                "ceiling at <white>Y ${session.maxHeight}</white>.",
        )
    }

    private fun listArenaFiles(player: Player) {
        val names =
            arenasFolder()
                .listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
                ?.map { it.nameWithoutExtension }
                ?.sorted()
                .orEmpty()

        if (names.isEmpty()) {
            player.info("No arena exists yet — use <white>/edit startEditing</white> to build the first one.")
        } else {
            player.info("Available: <white>${names.joinToString(", ").escapeTags()}</white>")
        }
    }

    private fun readSessionFromFile(
        player: Player,
        file: File,
    ): ArenaEdit? {
        val config = YamlConfiguration.loadConfiguration(file)
        val worldName = config.getString("world")
        if (worldName.isNullOrBlank()) {
            player.error("<white>${file.name}</white> has no <white>world</white> set, so it can't be opened.")
            return null
        }
        val world =
            Bukkit.getWorld(worldName) ?: run {
                player.error("The world <white>${worldName.escapeTags()}</white> of that arena is not loaded.")
                player.info("Load it first (with Multiverse: <white>/mv load ${worldName.escapeTags()}</white>) and run the command again.")
                return null
            }

        val session =
            ArenaEdit(
                editorId = player.uniqueId,
                name = file.nameWithoutExtension,
                minPlayers = config.takeIfSet("min-players"),
                slowFallingTime = config.takeIfSet("slow-falling-time"),
                spawnHeight = config.takeIfSet("spawn-height"),
                privateArena = config.getBoolean("private-arena", false),
                deathSpawn = config.getConfigurationSection("main-spawn")?.readLocation(world),
                regionLocationOne = config.getConfigurationSection("region.loc-1")?.readLocation(world),
                regionLocationSecond = config.getConfigurationSection("region.loc-2")?.readLocation(world),
                loadedFromFile = true,
            )

        val storedMin = config.takeIfSet("min-height")
        val storedMax = config.takeIfSet("max-height")
        if ((storedMin != null && storedMin != session.minHeight) || (storedMax != null && storedMax != session.maxHeight)) {
            player.warn(
                "That file sets its own heights (<white>$storedMin</white> → <white>$storedMax</white>); " +
                    "saving replaces them with the region ones (<white>${session.minHeight}</white> → " +
                    "<white>${session.maxHeight}</white>).",
            )
        }

        var unreadableSpawns = 0
        config.getConfigurationSection("spawns")?.let { spawns ->
            for (key in spawns.getKeys(false)) {
                val index = key.toIntOrNull()
                val location = spawns.getConfigurationSection(key)?.readLocation(world)
                if (index == null || location == null) {
                    unreadableSpawns++
                    continue
                }
                session.locations[index] = location
            }
        }
        if (unreadableSpawns > 0) {
            player.warn("$unreadableSpawns spawn entr(y/ies) in that file could not be read and will be lost if you save.")
        }
        return session
    }

    private fun sendHelp(player: Player) {
        player.sendMessage(LINE.toMini())
        player.sendMessage("$PREFIX<gold>Commands".toMini())
        helpEntries.forEach { (usage, description, shortForm) ->
            val alias = if (shortForm.isEmpty()) "" else " <dark_gray>[<gray>${shortForm.escapeTags()}<dark_gray>]"
            player.sendMessage("<dark_gray> ▪ <yellow>/edit ${usage.escapeTags()} <dark_gray>- <gray>$description$alias".toMini())
        }
        player.sendMessage("<dark_gray> <gray>The short form in brackets does the same thing.".toMini())
        player.sendMessage(LINE.toMini())
    }

    private val helpEntries: List<Triple<String, String, String>> =
        listOf(
            Triple("startEditing", "start a new arena", "start"),
            Triple("loadArena <name>", "edit an arena that already exists", "load"),
            Triple("setName <name>", "set the arena name (and its file name)", "name"),
            Triple("setSpawn <number>", "add/move a player spawn where you stand", "spawn"),
            Triple("removeSpawn <number>", "remove a spawn", "delSpawn"),
            Triple("listSpawns", "list every spawn", "spawns"),
            Triple("teleportToArena", "go to the arena you are editing", "tp"),
            Triple("teleportToSpawn <number>", "go to one of its spawns", "tp <number>"),
            Triple("setRegionOne / setRegionTwo", "the two opposite corners: they set the arena's height limits too", "pos1 / pos2"),
            Triple("setDeathSpawn", "where eliminated players and spectators go", ""),
            Triple("setMinPlayers <min>", "players needed to start the countdown", "minplayers"),
            Triple("setSlowFalling <seconds>", "slow falling given at the start", "setFallingTime"),
            Triple("setSpawnHeight <blocks>", "raise the glass cages above the spawns", ""),
            Triple("setPrivate <true|false>", "hide the arena from joining players", "private"),
            Triple("showStatus", "show what is set and what is missing", "check"),
            Triple("toggleMarkers", "turn the particle markers on and off", "view"),
            Triple("toggleAlignment", "turn the block-centring/view assist on and off", "snap"),
            Triple("saveArena", "write the file and load the arena", "save"),
            Triple("stopEditing", "close the session without saving", "stop"),
        )

    private fun ArenaEdit.displayName(): String = name ?: "unnamed arena"

    private fun arenasFolder(): File = File(GourPillars.instance.dataFolder, "arenas")

    private fun YamlConfiguration.takeIfSet(path: String): Int? = if (isSet(path)) getInt(path) else null

    private fun YamlConfiguration.writeLocation(
        path: String,
        location: Location,
        withRotation: Boolean = true,
    ) {
        set("$path.x", location.x)
        set("$path.y", location.y)
        set("$path.z", location.z)
        if (withRotation) {
            set("$path.yaw", location.yaw.toDouble())
            set("$path.pitch", location.pitch.toDouble())
        }
    }

    private fun ConfigurationSection.readLocation(world: World): Location? {
        if (!isSet("x") || !isSet("y") || !isSet("z")) return null
        return Location(
            world,
            getDouble("x"),
            getDouble("y"),
            getDouble("z"),
            getDouble("yaw", 0.0).toFloat(),
            getDouble("pitch", 0.0).toFloat(),
        )
    }

    private fun Location.pretty(): String = "${world.name} $blockX, $blockY, $blockZ"

    private fun Location.prettyBlock(): String = "$blockX, $blockY, $blockZ"

    private fun String.escapeTags(): String = replace("<", "\\<")

    private fun Player.success(message: String) = sendMessage("$PREFIX<green>$message".toMini())

    private fun Player.error(message: String) = sendMessage("$PREFIX<red>$message".toMini())

    private fun Player.warn(message: String) = sendMessage("$PREFIX<yellow>$message".toMini())

    private fun Player.info(message: String) = sendMessage("$PREFIX<gray>$message".toMini())
}
