package org.gourmet.gourPillars.task

import org.bukkit.*
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.data.PlayerData
import org.gourmet.gourPillars.managers.arena.*
import org.gourmet.gourPillars.other.Utils
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.other.toMini


class GameTask(private val arena: Arena, private val plugin: JavaPlugin): BukkitRunnable(){

    lateinit var alivePlayer: MutableMap<Player, Int>
    private var running = false
    private var secondsPassed = 300
    private var lastPlayer: Player? = null
    private val jsonManager = GourPillars.jsonManager
    private var lavaLevel = arena.minHeight
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    override fun run(){
        running = true
        lavaLevel = arena.minHeight
        alivePlayer = mutableMapOf()
        setupEvent()
        removeAllGlass()
        preparePlayer()
        setTimeByVote()
        startRandomItemTask()
        lavaEventManager()
        object : BukkitRunnable() {
            override fun run() {
                secondsPassed--
                if (!running) cancel()

                updateScoreBoard()

                if (alivePlayer.size <= 1 || secondsPassed == 0) {
                    handleEndGame()
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }


    private fun lavaEventManager(){
        if(arena.gameEvent != GameEvents.LAVA) return
        object : BukkitRunnable(){
            override fun run() {
                if(!running) cancel()
                arena.region.replaceYLevelWithLava(lavaLevel)
                lavaLevel++

            }

        }.runTaskTimer(GourPillars.instance, 0L, 4 * 20)
    }

    private fun setupEvent() {
        if (arena.knockbackVote.isEmpty() && arena.lavaEvent.isEmpty()) return
        if (arena.knockbackVote.size == arena.lavaEvent.size) return
        if(arena.noEventVote.size >= arena.knockbackVote.size && arena.noEventVote.size >= arena.lavaEvent.size) return

        arena.gameEvent = if (arena.knockbackVote.size >= arena.lavaEvent.size) {
            GameEvents.KNOCKBACK
        } else {
            GameEvents.LAVA
        }
    }

    private fun setTimeByVote() {
        val worldName = arena.spawnMap.keys.first().world.name
        val world = Bukkit.getWorld(worldName)
        if (world != null) {
            if (arena.nightVote.size <= arena.dayVote.size) {
                world.time = 6000
            } else {
                world.time = 18000
            }
        }
    }

    private fun updateScoreBoard(){
        arena.waitingPlayer.forEach{player ->
            arena.scoreboardManager.setGameScoreboard(player)
        }
    }

    private fun getWinner(): Player?{
        return when(alivePlayer.size){
            0 -> lastPlayer
            1 -> alivePlayer.keys.first()
            else -> alivePlayer.maxByOrNull { it.value }?.key
        }
    }

    private fun handleEndGame() {
        val winner = getWinner()
        //arena.sendTitleToPlayerInGame("&aGioco terminato!", "&e${winner?.name} &fha vinto")
        //arena.sendDynamicTitleToPlayerInGame(MessageData.ARENA_TITLE_END, MessageData.ARENA_SUBTITLE_END, "{winner}" to winner?.name)
        if (winner != null) {
            arena.sendDynamicTitleToPlayerInGame(MessageData.ARENA_TITLE_END, MessageData.ARENA_SUBTITLE_END, "{winner}" to winner.name)
            arena.waitingPlayer.forEach { messagePlayer ->
                messagePlayer.sendDynamicMessage(MessageData.WIN_GAME, "{winner}" to winner.name) //TODO: Mettere la placeholder winner

            }
        }
        //Update the statistic only for the winner
        arena.nightVote.clear()
        arena.dayVote.clear()
        arena.knockbackVote.clear()
        arena.lavaEvent.clear()
        if (winner != null) {
            winner.isInvulnerable = true
            val winnerData = jsonManager.getPlayerData(winner) ?: PlayerData(winner.name, 0, 0, 0, 0,0, 0, 0)
            winnerData.wins += 1
            winnerData.gamesPlayed += 1
            jsonManager.setPlayerData(winner, winnerData)
            jsonManager.savePlayerData()
            jsonManager.addXP(winner, 300)
            playVictoryEffects(winner)
        }
        //Update the defeat statistic for all players
        arena.waitingPlayer.forEach { playersForData ->
            val playerData = GourPillars.jsonManager.getPlayerData(playersForData) ?: PlayerData(playersForData.name,0,0,0,0,0,0,0)
            playerData.defeats = playerData.gamesPlayed - playerData.wins
            GourPillars.jsonManager.setPlayerData(playersForData, playerData)
            GourPillars.jsonManager.savePlayerData()
        }
        //Arena reset
        object : BukkitRunnable(){
            override fun run(){
                running = false
                secondsPassed = 300
                arena.gameState = State.STOPPED
                arena.waitingPlayer.forEach { player ->
                    GourPillars.spawnManager.teleportPlayerToSpawn(player)
                    GourPillars.lobbyScoreboardManager.setScoreboard(player)
                    player.inventory.clear()
                    player.health = 20.0
                    player.foodLevel = 20
                }
                arena.spawnMap.forEach{ (location, _) ->
                    arena.spawnMap[location] = null
                }
                arena.waitingPlayer.clear()
                alivePlayer.clear()

                cancel()
                arena.resetArenaTask.run()
            }
        }.runTaskLater(plugin, 80L)
    }

    private fun removeAllGlass(){
        arena.spawnMap.forEach{ (location, _) ->
            Utils.setGlass(false, location)
        }
    }

    private fun preparePlayer(){
        val effect = PotionEffect(PotionEffectType.SLOW_FALLING, arena.slowFallingTime * 20, 0)
        arena.waitingPlayer.forEach { player: Player ->
            alivePlayer[player] = 0
        }
        alivePlayer.forEach{(player, _) ->
            player.isInvulnerable = false
            player.addPotionEffect(effect)
            player.inventory.clear()
            player.gameMode = GameMode.SURVIVAL
            player.health = 20.0
            player.closeInventory()
            arena.scoreboardManager.setGameScoreboard(player)
        }
    }

    private fun startRandomItemTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (!running) {
                    cancel()
                    return
                }

                alivePlayer.forEach { (player, _ )->
                    giveRandomItem(player)
                }
            }
        }.runTaskTimer(GourPillars.instance, 0L, 70L)
    }

    private fun giveRandomItem(player: Player) {
        val randomMaterial = getRandomMaterial()
        val itemStack = ItemStack(randomMaterial)

        player.inventory.addItem(itemStack)

    }

    private fun getRandomMaterial(): Material {

        val nonPlacableMaterials = setOf(
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.BARRIER,
            Material.STRUCTURE_BLOCK,
            Material.JIGSAW,
            Material.DEBUG_STICK,
            Material.KNOWLEDGE_BOOK,
            Material.LIGHT,
            Material.STRUCTURE_VOID,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.BEDROCK,
            Material.SPAWNER,
            Material.ENDER_DRAGON_SPAWN_EGG,
            Material.WITHER_SPAWN_EGG,
            Material.SNOWBALL,
            Material.SHIELD
        )

        val armorTrimMaterials = Material.entries
            .filter { it.name.endsWith("_ARMOR_TRIM_SMITHING_TEMPLATE") }
            .toSet()


        val materials = Material.entries
            .filter { it.isItem  }
            .filter { m: Material ->
                !m.isEmpty && !m.isLegacy && m.isItem && m.isEnabledByFeature(Bukkit.getWorld("world")!!) && m !in nonPlacableMaterials && m !in armorTrimMaterials
            }

        return materials.random()
    }


    private fun eliminationProcess(player: Player){
        val kills = alivePlayer[player]
        if(alivePlayer.size <= 1)
            lastPlayer = player
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        arena.reloadInGameScoreboard()
        player.teleport(arena.spawnMainLocation)
        arena.waitingPlayer.forEach { playerSound ->
            playerSound.playSound(playerSound.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f)
        }
        val playerData = GourPillars.jsonManager.getPlayerData(player) ?: PlayerData(player.name,0,0,0,0,0,0,0)
        playerData.defeats += 1
        GourPillars.jsonManager.setPlayerData(player, playerData)
        GourPillars.jsonManager.savePlayerData()
        //player.sendMessage("<gray>--------------------------".toMini())
        //player.sendMessage("<dark_gray>•<gray> Game length: <yellow>${getTimeFormatted()}".toMini())
        //player.sendMessage("<dark_gray>•<gray> Kills: <yellow>${kills}".toMini())
        //player.sendMessage("<dark_gray>•<gray> Map: <yellow>${arena.name}".toMini())
        //player.sendMessage("<gray>--------------------------".toMini())
        //player.sendMessage(("<hover:show_text:\"<green>Start a new game\">" +
        //        "<click:run_command:/joinrandom>" +
        //        "<gray>Click Here to <green>play again").toMini())
        //player.sendMessage("<gray>--------------------------".toMini())

        player.sendDynamicMessage(MessageData.END_GAME,
            "{time}" to getTimeFormatted(),
            "{kills}" to kills.toString(),
            "{map}" to arena.name)
    }

    fun playerEliminated(player: Player){
        eliminationProcess(player)
        arena.waitingPlayer.forEach { receiverPlayer ->
            if(receiverPlayer != player) //receiverPlayer.sendMessage("$prefix <yellow><green>${player.name}</green> e' stato eliminato</yellow>".toMini())
                receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED, "{player}" to player.name)
        }

    }

    //fall damage death message
    fun playerEliminatedFall(player: Player){
        eliminationProcess(player)
        arena.waitingPlayer.forEach { receiverPlayer ->
            if(receiverPlayer != player) //receiverPlayer.sendMessage("$prefix <yellow><green>${player.name}</green> e' caduto</yellow>".toMini())
                receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED_FALL, "{player}" to player.name)
        }

    }

    //player kill by player death message
    fun playerEliminated(player: Player, killer: Player){
        if(alivePlayer.size <= 1)
            lastPlayer = player
        alivePlayer.remove(player)
        player.gameMode = GameMode.SPECTATOR
        arena.waitingPlayer.forEach { receiverPlayer ->
            if(receiverPlayer != player) //receiverPlayer.sendMessage("$prefix <yellow><green>${player.name}</green> e' stato eliminato da <green>${killer.name}</green></yellow>".toMini())
                receiverPlayer.sendDynamicMessage(MessageData.ARENA_PLAYER_ELIMINATED_KILL,
                    "{player}" to player.name,
                    "{killer}" to killer.name)
        }
        if(alivePlayer.contains(killer)){
            val oldKills = alivePlayer[killer]!! + 1
            alivePlayer[killer] = oldKills
        }
        arena.reloadInGameScoreboard()

    }


    fun getTimeFormatted(): String{
        val minutes = secondsPassed / 60
        val remainingSeconds = secondsPassed % 60

        val minuteText = if (minutes == 1) "1 minuto" else "$minutes minuti"
        val secondText = if (remainingSeconds == 1) "1 secondo" else "$remainingSeconds secondi"

        return if (minutes > 0) "$minuteText $secondText" else secondText
    }

    private fun playVictoryEffects(winner: Player) {
        val world = winner.world

        arena.waitingPlayer.forEach { player ->
            player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
        }

        if(arena.containPlayer(winner)) {
            object : BukkitRunnable() {
                var count = 0
                override fun run() {
                    if (count >= 3) {
                        cancel()
                        return
                    }
                    val firework = world.spawn(winner.location, Firework::class.java)
                    val meta = firework.fireworkMeta
                    meta.addEffect(
                        FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL_LARGE)
                            .withColor(Color.RED, Color.BLUE, Color.YELLOW)
                            .withFlicker()
                            .build()
                    )
                    meta.power = 1
                    firework.fireworkMeta = meta
                    count++
                }
            }.runTaskTimer(plugin, 0L, 20L)
        }
    }
}


