package org.gourmet.gourPillars.commands

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand

@Command("party", "p")
object PartyCMD {

    private val partyManager = GourPillars.partyManager
    private val invitedPlayers: MutableMap<Player, Player> = mutableMapOf() //Target, Owner
    private val chatToggle: MutableMap<Player, Boolean> = mutableMapOf() //Party member, enabled/disabled

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
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_NO_PARTY_REQUEST)
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
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
            //player.sendMessage("$prefix <hover:show_text:\"<white>/party create\"><click:run_command:/party create><green><bold>CREA UN PARTY\"")
            return
        }
        if(player == target){
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_CANT_INVITE_YOURSELF)
            return
        }
        if(partyManager.isInParty(target)) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_USER_ALREADY_IN_PARTY)
            return
        }
        if(party.partyAdmin != player) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }
        invitedPlayers[target] = player

        //target.sendMessage("$prefix<yellow>Sei stato invitato nel party da <white>${player.name}</white>, clicca per accettare</yellow>")
        //target.sendMessage("<hover:show_text:\"<white>/party accept\"><click:run_command:/party accept><green><bold>ACCETTA")
        target.sendDynamicMessage(MessageData.PARTY_INVITE_RECEIVE, "{player}" to player.name)
        //player.sendMessage("$prefix <yellow>Hai invitato <white>${target.name}</white> nel party</yellow>")
        player.sendDynamicMessage(MessageData.PARTY_INVITE, "{player}" to target.name)

        object : BukkitRunnable(){
            override fun run(){

                if(invitedPlayers.contains(target)){
                    //target.sendMessage("$prefix <red>L'invito al party e' scaduto".toMini())
                    target.sendDynamicMessage(MessageData.PARTY_ERRORS_INVITE_EXPIRED)
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

            if(membersList.isNotEmpty()){

                player.sendDynamicMessage(MessageData.PARTY_PARTY_INFO,
                    "{partyAdmin}" to party.partyAdmin.name,
                    "{members}" to membersList,
                    "{members_count}" to party.members.size.toString(),
                    "{members_max}" to "8")
            } else {
                player.sendDynamicMessage(MessageData.PARTY_PARTY_INFO_NO_MEMBERS,
                    "{partyAdmin}" to party.partyAdmin.name,
                    "{members_count}" to party.members.size.toString(),
                    "{members_max}" to "8")
            }

        } else {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_PLAYER_NOT_IN_PARTY)
        }

        Bukkit.getLogger().info("invites: $invitedPlayers")
    }

    //todo complete party chat
    //@Subcommand("chat")
    fun partyChat(player: Player) {
        val party = partyManager.getPartyByPlayer(player) ?: return
        if (partyManager.isInParty(player)) {
            val chatEnabled: Boolean = chatToggle.getOrDefault(player, false)
            chatToggle.put(player, !chatEnabled)
            player.sendDynamicMessage(MessageData.PARTY_CHAT_ENABLED_DISABLED, "{status}" to if (chatEnabled) "" else "")
        } else {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_PLAYER_NOT_IN_PARTY)
        }
    }

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

        //player.sendMessage(message)
        player.sendDynamicMessage(MessageData.PARTY_PARTY_COMMAND_HELP)
    }
}

