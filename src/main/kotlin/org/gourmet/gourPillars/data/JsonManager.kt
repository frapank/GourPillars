package org.gourmet.gourPillars.data
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.LevelBarManager
import org.gourmet.gourPillars.other.toMini
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class JsonManager {

    private val playerDataMap: HashMap<String, PlayerData> = HashMap()
    private val dataJson: File
    private val gourPillars: GourPillars = GourPillars.instance

    init {
        dataJson = File(gourPillars.dataFolder, "data.json")

        if (!gourPillars.dataFolder.exists()) {
            gourPillars.dataFolder.mkdirs()
        }

        if (!dataJson.exists()) {
            try {
                val gson: Gson = GsonBuilder().setPrettyPrinting().create()
                dataJson.createNewFile()
                FileWriter(dataJson).use { writer ->
                    gson.toJson(JsonObject(), writer)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        loadPlayerData()
        gourPillars.logger.warning("Init finito")
    }

    private fun loadPlayerData() {
        if (!dataJson.exists()) return
        try {
            FileReader(dataJson).use { reader ->
                val gson = Gson()
                val jsonObject = gson.fromJson(reader, JsonObject::class.java)

                if (!jsonObject.has("player_data")) {
                    gourPillars.logger.warning("Nessun player in data.json")
                    return
                }
                val playerDataJson: JsonObject = jsonObject.getAsJsonObject("player_data")
                for (playerName in playerDataJson.keySet()) {
                    val data = playerDataJson.getAsJsonObject(playerName)
                    val playerLevel = data.get("player_level").asInt
                    val playerXP = data.get("player_xp").asInt
                    val playerKills = data.get("player_kills").asInt
                    val playerWins = data.get("player_wins").asInt
                    val playerDeaths = data.get("player_deaths").asInt
                    val playerGamesPlayed = data.get("player_gamesplayed").asInt
                    val playerDefeats = playerGamesPlayed - playerWins
                    val playerData = PlayerData(playerName, playerLevel, playerXP, playerKills, playerWins, playerDeaths, playerDefeats, playerGamesPlayed)
                    playerDataMap[playerName] = playerData

                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun savePlayerData() {
        val jsonObject = JsonObject()
        val playerDataJson = JsonObject()

        playerDataMap.keys.forEach { playerName ->
            val playerData = playerDataMap[playerName] ?: return@forEach
            val data = JsonObject().apply {
                addProperty("player_level", playerData.level)
                addProperty("player_kills", playerData.kills)
                addProperty("player_wins", playerData.wins)
                addProperty("player_deaths", playerData.deaths)
                addProperty("player_gamesplayed", playerData.gamesPlayed)
                addProperty("player_xp", playerData.xp)
            }

            playerDataJson.add(playerName, data)
        }

        jsonObject.add("player_data", playerDataJson)

        try {
            FileWriter(dataJson).use { writer ->
                Gson().toJson(jsonObject, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getPlayerData(player: Player): PlayerData? {
        return playerDataMap[player.name]
    }

    fun setPlayerData(player: Player, data: PlayerData) {
        playerDataMap[player.name] = data
    }

    fun setPlayerDataIfAbsent(player: Player, data: PlayerData) {
        playerDataMap.putIfAbsent(player.name, data)
    }

    fun getPlayerDataMap(): HashMap<String, PlayerData>{
        return playerDataMap
    }

    fun getPlayerKD(player: Player) : Double {
        val playerData = getPlayerData(player) ?: PlayerData(player.name, 0, 0, 0, 0, 0, 0, 0)
        val kd: Double
        if(playerData.deaths == 0) {
            kd = playerData.kills.toDouble()
        } else if(playerData.kills == 0) {
            kd = 0.0
        } else {
            kd = playerData.kills.toDouble() / playerData.deaths
        }
        //kd = Math.round(kd * 100.0) / 100.0 //TODO test and check if round is needed
        return String.format("%.2f", kd).toDouble()
    }

    fun addXP(player: Player, xpToAdd: Int){
        val playerData = getPlayerData(player) ?: PlayerData(player.name, 0, 0, 0, 0, 0, 0, 0)
        playerData.xp += xpToAdd
        setPlayerData(player, playerData)
        savePlayerData()
        player.sendActionBar("<aqua>${xpToAdd}<green>XP+".toMini())
        if(playerData.xp >= 3000){
            playerData.level += 1
            playerData.xp = 0
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            player.sendMessage("<green>Sei salito al livello <aqua>${playerData.level}".toMini())
            LevelBarManager.updateLevelInBar(player)
        }
    }

    fun setLevel(player: Player, level: Int){
        val playerData = getPlayerData(player) ?: PlayerData(player.name, 0, 0, 0, 0, 0, 0, 0)
        playerData.level = level
        setPlayerData(player, playerData)
        savePlayerData()
    }
}