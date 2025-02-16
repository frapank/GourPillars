package org.gourmet.gourPillars.commands

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.LevelBarManager
import org.gourmet.gourPillars.managers.arena.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.bukkit.annotation.CommandPermission

object StatsCMD {

    private val jsonManager = GourPillars.jsonManager

    @Command("stats")
    fun statsCommand(player: Player){
        val playerData = jsonManager.getPlayerData(player)
        val kills = playerData?.kills ?: "error"
        val death = playerData?.deaths ?: "error"
        val wins = playerData?.wins ?: "error"
        val defeats = playerData?.defeats ?: "error"
        val gamesPlayed = playerData?.gamesPlayed ?: "error"
        val xp = playerData?.xp ?: "error"
        val level = playerData?.level ?: "error"

        player.sendMessage("<gradient:#c061cb:#3584e4>✦━━━━━ Stats ━━━━━✦</gradient>".toMini())
        player.sendMessage("")
        player.sendMessage("<gray>👤 Player<gray>:</gray> <yellow>${player.name}</yellow>".toMini())
        player.sendMessage("<gray>💀 Deaths<gray>:</gray> <yellow>$death</yellow>".toMini())
        player.sendMessage("<gray>⚔ Defeats<gray>:</gray> <yellow>$defeats</yellow>".toMini())
        player.sendMessage("<gray>🏹 Kills:</gray> <yellow>$kills</yellow>".toMini())
        player.sendMessage("<gray>🏆 Wins:</gray> <yellow>$wins</yellow>".toMini())
        player.sendMessage("<gray>🎮 Games Played:</gray> <yellow>$gamesPlayed</yellow>".toMini())
        player.sendMessage("<gray>⭐ XP:</gray> <yellow>$xp</yellow>".toMini())
        player.sendMessage("<gray>⬆ Level:</gray> <yellow>$level</yellow>".toMini())
        player.sendMessage("")
        player.sendMessage("<gradient:#c061cb:#3584e4>✦━━━━━━━━━━━━━━━━✦</gradient>".toMini())


    }

    @Command("stats <target>")
    fun statsCommand(player: Player, target: Player){
        val playerData = jsonManager.getPlayerData(target)
        val kills = playerData?.kills ?: "error"
        val death = playerData?.deaths ?: "error"
        val wins = playerData?.wins ?: "error"
        val defeats = playerData?.defeats ?: "error"
        val gamesPlayed = playerData?.gamesPlayed ?: "error"
        val xp = playerData?.xp ?: "error"
        val level = playerData?.level ?: "error"

        val mm = MiniMessage.miniMessage()

        val message = mm.deserialize("""
        <gradient:#ffcc00:#ff6699>✦━━━━━━━━━━━━━━━━━━━━━━━✦</gradient>
        <bold><gradient:#00ff99:#00ccff>📊 Statistiche Giocatore 📊</gradient></bold>
        
        <yellow>👤 Player</yellow> <gray>»</gray> <green>${target.name}</green>
        <yellow>💀 Deaths</yellow> <gray>»</gray> <red>$death</red>
        <yellow>⚔ Defeats</yellow> <gray>»</gray> <red>$defeats</red>
        <yellow>🏹 Kills</yellow> <gray>»</gray> <green>$kills</green>
        <yellow>🏆 Wins</yellow> <gray>»</gray> <aqua>$wins</aqua>
        <yellow>🎮 Games Played</yellow> <gray>»</gray> <gold>$gamesPlayed</gold>
        <yellow>⭐ XP</yellow> <gray>»</gray> <light_purple>$xp</light_purple>
        <yellow>⬆ Level</yellow> <gray>»</gray> <blue>$level</blue>
        
        <gradient:#ff6699:#ffcc00>✦━━━━━━━━━━━━━━━━━━━━━━━✦</gradient>
        """)

        player.sendMessage(message)

    }

    @Command("levelset <level>")
    @CommandPermission("pillars.admin")
    fun levelSet(player: Player, level: Int){
        jsonManager.addXP(player, level)
        LevelBarManager.updateLevelInBar(player)
    }

}