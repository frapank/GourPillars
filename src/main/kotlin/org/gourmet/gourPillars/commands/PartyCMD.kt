package org.gourmet.gourPillars.commands

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.annotation.Switch
import revxrsal.commands.bukkit.annotation.CommandPermission

@Command("party", "p")
object PartyCMD {
    private val partyManager = GourPillars.partyManager
    private val invitedPlayers: MutableMap<Player, Player> = mutableMapOf()

    @Subcommand()
    fun partyMain(player: Player) {
        player.sendDynamicMessage(MessageData.PARTY_PARTY_COMMAND_HELP)
    }

    @Subcommand("create")
    @CommandPermission("gpillars.party.create")
    fun createParty(
        player: Player,
        @Switch("public") isPublic: Boolean,
    ) {
        partyManager.createParty(player, isPublic)
    }

    @Subcommand("accept")
    @CommandPermission("gpillars.party.accept")
    fun acceptParty(player: Player) {
        val owner =
            invitedPlayers[player] ?: run {
                player.sendDynamicMessage(MessageData.PARTY_ERRORS_NO_PARTY_REQUEST)
                return
            }
        invitedPlayers.remove(player)
        partyManager.addMember(owner, player)
    }

    @Subcommand("invite <target>")
    @CommandPermission("gpillars.party.invite")
    fun inviteToParty(
        player: Player,
        target: Player,
    ) {
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
        val max = partyManager.maxPartySize()
        if (party.members.size >= max) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_MAX_PARTY_MEMBER, "{max}" to max.toString())
            return
        }

        invitedPlayers[target] = player

        target.sendDynamicMessage(MessageData.PARTY_INVITE_RECEIVE, "{player}" to player.name)
        player.sendDynamicMessage(MessageData.PARTY_INVITE, "{player}" to target.name)

        object : BukkitRunnable() {
            override fun run() {
                if (invitedPlayers[target] == player) {
                    target.sendDynamicMessage(MessageData.PARTY_ERRORS_INVITE_EXPIRED)
                    invitedPlayers.remove(target)
                }
            }
        }.runTaskLaterAsynchronously(GourPillars.instance, 20 * 20)
    }

    @Subcommand("remove <target>")
    @CommandPermission("gpillars.party.remove")
    fun removeMember(
        player: Player,
        target: Player,
    ) {
        partyManager.kickPlayerFromParty(player, target)
    }

    @Subcommand("leave")
    @CommandPermission("gpillars.party.leave")
    fun leaveParty(player: Player) {
        partyManager.leaveParty(player)
    }

    @Subcommand("disband")
    @CommandPermission("gpillars.party.disband")
    fun disbandParty(player: Player) {
        partyManager.disbandParty(player)
    }

    @Subcommand("promote <target>")
    @CommandPermission("gpillars.party.promote")
    fun partyPromote(
        player: Player,
        target: Player,
    ) {
        partyManager.promote(player, target)
    }

    @Subcommand("join <target>")
    @CommandPermission("gpillars.party.join")
    fun joinParty(
        player: Player,
        target: Player,
    ) {
        partyManager.joinPublicParty(player, target)
    }

    @Subcommand("public")
    @CommandPermission("gpillars.party.public")
    fun makePublic(player: Player) {
        partyManager.setPublic(player, true)
    }

    @Subcommand("private")
    @CommandPermission("gpillars.party.public")
    fun makePrivate(player: Player) {
        partyManager.setPublic(player, false)
    }

    @Subcommand("broadcast")
    @CommandPermission("gpillars.party.broadcast")
    fun broadcastParty(player: Player) {
        partyManager.broadcast(player)
    }

    @Subcommand("info", "list")
    @CommandPermission("gpillars.party.info")
    fun partyInfo(player: Player) {
        val party =
            partyManager.getPartyByPlayer(player) ?: run {
                player.sendDynamicMessage(MessageData.PARTY_ERRORS_PLAYER_NOT_IN_PARTY)
                return
            }

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
    }
}
