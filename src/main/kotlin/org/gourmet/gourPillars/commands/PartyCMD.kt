package org.gourmet.gourPillars.commands

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand

@Command("party", "p")
object PartyCMD {

    private val partyManager = GourPillars.partyManager
    private val invitedPlayers: MutableMap<Player, Player> = mutableMapOf() //Target, Owner
    private val prefix = "<bold><green>Party <bold><gray>|"

    @Subcommand()
    fun partyMain(player: Player){
        sendCommandsPartyHelp(player)
    }

    @Subcommand("create")
    fun createParty(player: Player){
        partyManager.createParty(player)
    }

    @Subcommand("accept")
    fun acceptParty(player: Player){
        if(!invitedPlayers.contains(player)){
            player.sendMessage("$prefix Non hai nessuna richiesta".toMini())
            return
        }
        val owner = invitedPlayers[player] ?: return
        partyManager.addMember(owner, player)
        invitedPlayers.remove(player)
    }

    @Subcommand("invite <target>")
    fun inviteToParty(player: Player, target: Player) {
        //todo automatic party creation & test party members size limit (test)
        val party = partyManager.getPartyByPlayer(player) ?: run {
            player.sendMessage("$prefix <red>Non sei in nessun party".toMini())
            player.sendMessage("$prefix <hover:show_text:\"<white>/party create\"><click:run_command:/party create><green><bold>CREA UN PARTY\"".toMini())
            return
        }
        if(player == target){
            player.sendMessage("$prefix <red>Non puoi invitare te stesso".toMini())
            return
        }
        if(partyManager.isInParty(target)) {
            player.sendMessage("$prefix <red>Questo utente e' gia in un party".toMini())
            return
        }
        if(party.partyAdmin != player) {
            player.sendMessage("$prefix <red>Non sei l'admin del party".toMini())
            return
        }
        invitedPlayers[target] = player

        target.sendMessage("$prefix<yellow>Sei stato invitato nel party da <white>${player.name}</white>, clicca per accettare</yellow>".toMini())
        target.sendMessage("<hover:show_text:\"<white>/party accept\"><click:run_command:/party accept><green><bold>ACCETTA".toMini())
        player.sendMessage("$prefix <yellow>Hai invitato <white>${target.name}</white> nel party</yellow>".toMini())

        object : BukkitRunnable(){
            override fun run(){

                if(invitedPlayers.contains(target)){
                    target.sendMessage("$prefix <red>L'invito al party e' scaduto".toMini())
                    invitedPlayers.remove(target)
                }

            }
        }.runTaskLaterAsynchronously(GourPillars.instance, 20 * 20)
    }

    //todo make a admin party add member for tests
    //@Subcommand("add <target>")
    //@CommandPermission("pillar.admin")
    //fun addMember(player: Player, target: Player) {
    //
    //    partyManager.addMember(player, player)
    //}


    @Subcommand("remove <target>")
    fun removeMember(player: Player, target: Player) {
        partyManager.kickPlayerFromParty(player, target)
    }

    @Subcommand("leave")
    fun leaveParty(player: Player) {
        partyManager.leaveParty(player)
    }

    @Subcommand("disband")
    fun disbandParty(player: Player) {
        partyManager.disbandParty(partyManager.getPartyByPlayer(player) ?: return)
    }

    @Subcommand("promote <target>")
    fun partyPromote(player: Player, target: Player) {
        partyManager.promote(player, target)
    }

    @Subcommand("info", "list")
    fun partyInfo(player: Player) {
        val mm = MiniMessage.miniMessage()
        val party = partyManager.getPartyByPlayer(player) ?: return

        if (partyManager.isInParty(player)) {
            val membersList = party.members.filter { it != party.partyAdmin }
                .joinToString(" <gray>|</gray> ") { "<yellow>${it.name}</yellow>" }


            player.sendMessage("<gradient:#c061cb:#3584e4>✦━━━ INFO PARTY ━━━✦</gradient>".toMini())
            player.sendMessage("<gray>👑 Admin:</gray> <yellow>${party.partyAdmin.name}</yellow>".toMini())
            if(membersList.isNotEmpty()){
                player.sendMessage("<gray>👥 Membri:</gray> $membersList".toMini())
            }
            player.sendMessage("<gradient:#c061cb:#3584e4>✦━━━━━━━━━━━━━━━━━━✦</gradient>".toMini())


        } else {
            player.sendMessage(mm.deserialize("<red>❌ Non sei in un party!"))
        }

        Bukkit.getLogger().info("invites: $invitedPlayers")
    }

    //@Subcommand("info", "list")
    //fun partyInfo(player: Player) {
    //    val mm = MiniMessage.miniMessage()
    //    val party = partyManager.getPartyByPlayer(player) ?: return
    //
    //    if (partyManager.isInParty(player)) {
    //        val message = mm.deserialize("""
    //        <gradient:#ffcc00:#ff6699>✦━━━━━━━━━━━━━━━━━━━━━✦</gradient>
    //        <bold><gradient:#00ff99:#00ccff>🎉 Info del Party 🎉</gradient></bold>
    //
    //        <gray>👑 Admin:</gray> <yellow>${party.partyAdmin.name}</yellow>
    //
    //        ${if (party.members.count() > 1) "<gray>👥 Membri:</gray>" else ""}
    //        ${party.members.filter { it != party.partyAdmin }.joinToString("\n") { "<white>▪ <yellow>${it.name}</yellow>" }}
    //
    //        <gradient:#ff6699:#ffcc00>✦━━━━━━━━━━━━━━━━━━━━━✦</gradient>
    //    """)
    //
    //        player.sendMessage(message)
    //    } else {
    //        player.sendMessage(mm.deserialize("<red>❌ Non sei in un party!"))
    //    }
    //
    //    Bukkit.getLogger().info("invites: $invitedPlayers")
    //}

    //@Subcommand("info", "list")
    //fun partyInfo(player: Player) {
    //    val party = partyManager.getPartyByPlayer(player) ?: return
    //    if(partyManager.isInParty(player)) {
    //        player.sendMessage("---------------------------")
    //        player.sendMessage("<yellow>Info del party\n".toMini())
    //        player.sendMessage("<gray>Admin: <yellow>${party.partyAdmin.name}\n".toMini())
    //        if (party.members.count() > 1) {
    //            player.sendMessage("<gray>Membri:".toMini())
    //            party.members.forEach { member ->
    //                if (member != party.partyAdmin)
    //                    player.sendMessage("<white>▪ <yellow>${member.name}".toMini())
    //            }
    //
    //        }
    //        player.sendMessage("---------------------------")
    //    } else {
    //        player.sendMessage("<red>Non sei in un party!".toMini())
    //    }
    //    Bukkit.getLogger().info("invites: $invitedPlayers")
    //}

    private fun sendCommandsPartyHelp(player: Player) {
        val mm = MiniMessage.miniMessage()

        val message = mm.deserialize("""
        <gradient:#c061cb:#3584e4>━━━━━━━━━━━━━━━━━━━━━━━</gradient>
        <bold><gradient:#00ff99:#00ccff>☘ Comandi Party ☘</gradient></bold>
        
        <gold>/party create</gold> <gray>- <yellow>Crea il party</yellow>
        <gold>/party invite <player></gold> <gray>- <yellow>Invita un giocatore</yellow>
        <gold>/party accept</gold> <gray>- <yellow>Accetta un invito</yellow>
        <gold>/party leave</gold> <gray>- <yellow>Lascia il party</yellow>
        <gold>/party disband</gold> <gray>- <yellow>Sciogli il party</yellow>
        <gold>/party remove <player></gold> <gray>- <yellow>Rimuovi un membro</yellow>
        <gold>/party promote <player></gold> <gray>- <yellow>Promuovi un admin</yellow>
        <gold>/party info</gold> <gray>- <yellow>Info sul party</yellow>
        
        <gradient:#c061cb:#3584e4>━━━━━━━━━━━━━━━━━━━━━━━</gradient>
        """.trimIndent()
        )

        player.sendMessage(message)
    }
}

