package org.gourmet.gourPillars.commands

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.other.toMini
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand

@Command("party", "p")
object PartyCMD {
    private val partyManager = GourPillars.partyManager
    private val invitedPlayers: MutableMap<Player, Player> = mutableMapOf()

    @Subcommand()
    fun partyMain(player: Player) {
        sendCommandsPartyHelp(player)
    }

    @Subcommand("create")
    fun createParty(player: Player) {
        partyManager.createParty(player)
    }

    @Subcommand("accept")
    fun acceptParty(player: Player) {
        if (!invitedPlayers.contains(player)) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_NO_PARTY_REQUEST)
            return
        }
        val owner = invitedPlayers[player] ?: return
        partyManager.addMember(owner, player)
        invitedPlayers.remove(player)
    }

    @Subcommand("invite <target>")
    fun inviteToParty(
        player: Player,
        target: Player,
    ) {
        // todo automatic party creation & test party members size limit (test)
        val party =
            partyManager.getPartyByPlayer(player) ?: run {
                player.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
                return
            }
        if (player == target) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_CANT_INVITE_YOURSELF)
            return
        }
        if (partyManager.isInParty(target)) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_USER_ALREADY_IN_PARTY)
            return
        }
        if (party.partyAdmin != player) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }
        invitedPlayers[target] = player

        target.sendDynamicMessage(MessageData.PARTY_INVITE_RECEIVE, "{player}" to player.name)
        player.sendDynamicMessage(MessageData.PARTY_INVITE, "{player}" to target.name)

        object : BukkitRunnable() {
            override fun run() {
                if (invitedPlayers.contains(target)) {
                    target.sendDynamicMessage(MessageData.PARTY_ERRORS_INVITE_EXPIRED)
                    invitedPlayers.remove(target)
                }
            }
        }.runTaskLaterAsynchronously(GourPillars.instance, 20 * 20)
    }

    @Subcommand("remove <target>")
    fun removeMember(
        player: Player,
        target: Player,
    ) {
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
    fun partyPromote(
        player: Player,
        target: Player,
    ) {
        partyManager.promote(player, target)
    }

    @Subcommand("info", "list")
    fun partyInfo(player: Player) {
        val party = partyManager.getPartyByPlayer(player) ?: return

        if (partyManager.isInParty(player)) {
            val membersList =
                party.members
                    .filter { it != party.partyAdmin }
                    .joinToString(" <gray>|</gray> ") { "<yellow>${it.name}</yellow>" }

            if (membersList.isNotEmpty()) {
                player.sendDynamicMessage(
                    MessageData.PARTY_PARTY_INFO,
                    "{partyAdmin}" to party.partyAdmin.name,
                    "{members}" to membersList,
                )
            } else {
                player.sendDynamicMessage(
                    MessageData.PARTY_PARTY_INFO_NO_MEMBERS,
                    "{partyAdmin}" to party.partyAdmin.name,
                )
            }
        } else {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_PLAYER_NOT_IN_PARTY)
        }

        Bukkit.getLogger().info("invites: $invitedPlayers")
    }

    private fun sendCommandsPartyHelp(player: Player) {
        player.sendDynamicMessage(MessageData.PARTY_PARTY_COMMAND_HELP)
    }
}
