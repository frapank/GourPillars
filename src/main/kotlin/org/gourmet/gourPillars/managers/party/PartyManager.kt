package org.gourmet.gourPillars.managers.party

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage

class PartyManager {
    private val parties: MutableSet<PartyData> = mutableSetOf()

    fun maxPartySize(): Int {
        val arenaMax = GourPillars.arenaManager.maxArenaCapacity()
        if (arenaMax > 0) return arenaMax
        return GourPillars.instance.config.getInt("party.fallback-max-size", 8)
    }

    fun createParty(
        creator: Player,
        public: Boolean,
    ) {
        if (isInParty(creator)) {
            creator.sendDynamicMessage(MessageData.PARTY_ERRORS_USER_ALREADY_IN_PARTY)
            return
        }
        if (public && !creator.hasPermission("gpillars.party.public")) {
            creator.sendDynamicMessage(MessageData.PARTY_ERRORS_NO_PUBLIC_PERMISSION)
            return
        }
        val newParty = PartyData(creator, isPublic = public)
        newParty.members.add(creator)
        parties.add(newParty)
        creator.sendDynamicMessage(MessageData.PARTY_PARTY_CREATED)
    }

    fun disbandParty(caller: Player) {
        val party =
            getPartyByPlayer(caller) ?: run {
                caller.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
                return
            }
        if (party.partyAdmin != caller) {
            caller.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }

        for (member in party.members) {
            member.sendDynamicMessage(MessageData.PARTY_PARTY_DISBAND)
        }

        party.members.clear()
        parties.remove(party)
    }

    fun addMember(
        owner: Player,
        target: Player,
    ) {
        val party =
            getPartyByPlayer(owner) ?: run {
                owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
                return
            }
        if (isInParty(target)) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_USER_ALREADY_IN_PARTY)
            return
        }
        if (party.partyAdmin != owner) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }
        val max = maxPartySize()
        if (party.members.size >= max) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_MAX_PARTY_MEMBER, "{max}" to max.toString())
            return
        }
        party.members.forEach { player ->
            player.sendDynamicMessage(MessageData.PARTY_PLAYER_JOINED_BROADCAST, "{player}" to target.name)
        }
        target.sendDynamicMessage(MessageData.PARTY_PLAYER_JOINED, "{player}" to owner.name)
        party.members.add(target)
    }

    fun joinPublicParty(
        player: Player,
        target: Player,
    ) {
        if (isInParty(player)) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_USER_ALREADY_IN_PARTY)
            return
        }
        val party =
            getPartyByPlayer(target) ?: run {
                player.sendDynamicMessage(MessageData.PARTY_ERRORS_TARGET_HAS_NO_PARTY, "{player}" to target.name)
                return
            }
        if (!party.isPublic) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_PARTY_NOT_PUBLIC)
            return
        }
        val max = maxPartySize()
        if (party.members.size >= max) {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_MAX_PARTY_MEMBER, "{max}" to max.toString())
            return
        }
        party.members.forEach { member ->
            member.sendDynamicMessage(MessageData.PARTY_PLAYER_JOINED_BROADCAST, "{player}" to player.name)
        }
        player.sendDynamicMessage(MessageData.PARTY_PLAYER_JOINED, "{player}" to party.partyAdmin.name)
        party.members.add(player)
    }

    fun setPublic(
        caller: Player,
        public: Boolean,
    ) {
        val party =
            getPartyByPlayer(caller) ?: run {
                caller.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
                return
            }
        if (party.partyAdmin != caller) {
            caller.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }
        if (party.isPublic == public) {
            caller.sendDynamicMessage(
                if (public) MessageData.PARTY_ERRORS_ALREADY_PUBLIC else MessageData.PARTY_ERRORS_ALREADY_PRIVATE,
            )
            return
        }
        party.isPublic = public
        val message = if (public) MessageData.PARTY_PARTY_PUBLIC else MessageData.PARTY_PARTY_PRIVATE
        party.members.forEach { it.sendDynamicMessage(message, "{player}" to party.partyAdmin.name) }
    }

    fun broadcast(caller: Player) {
        val party =
            getPartyByPlayer(caller) ?: run {
                caller.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
                return
            }
        if (party.partyAdmin != caller) {
            caller.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }
        if (!party.isPublic) {
            caller.sendDynamicMessage(MessageData.PARTY_ERRORS_PARTY_NOT_PUBLIC)
            return
        }
        Bukkit.getOnlinePlayers().forEach { online ->
            online.sendDynamicMessage(MessageData.PARTY_PARTY_BROADCAST, "{player}" to caller.name)
        }
    }

    /**
     *
     * Kick a player from the party
     *
     * @param owner the party owner
     * @param target the party target
     * @return if player got removed successfully
     *
     */
    fun kickPlayerFromParty(
        owner: Player,
        target: Player,
    ): Boolean {
        val party =
            getPartyByPlayer(owner) ?: run {
                owner.sendDynamicMessage(MessageData.PARTY_ERRORS_PLAYER_NOT_IN_PARTY)
                return false
            }
        if (party.partyAdmin != owner) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return false
        }
        if (!party.members.contains(target)) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_TARGET_NOT_IN_PARTY, "{player}" to target.name)
            return false
        }
        party.members.forEach { player ->
            player.sendDynamicMessage(MessageData.PARTY_USER_LEFT_PARTY, "{player}" to target.name)
        }
        party.members.remove(target)
        return true
    }

    /**
     * leave party
     * @param player the player that leaves the party
     *
     */
    fun leaveParty(player: Player) {
        val party =
            getPartyByPlayer(player) ?: run {
                player.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
                return
            }
        if (player == party.partyAdmin) {
            if (party.members.size <= 1) {
                parties.remove(party)
                player.sendDynamicMessage(MessageData.PARTY_PARTY_DISBAND)
                return
            }
            party.members.remove(player)
            val first = party.members.first()
            party.partyAdmin = first
            first.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE)
            party.members.forEach { member ->
                if (member != first) {
                    member.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE_BROADCAST, "{player}" to first.name)
                }
            }
        } else {
            party.members.remove(player)
            party.members.forEach { member ->
                member.sendDynamicMessage(MessageData.PARTY_USER_LEFT_PARTY, "{player}" to player.name)
            }
            player.sendDynamicMessage(MessageData.PARTY_PARTY_LEAVE)
        }
    }

    fun promote(
        owner: Player,
        target: Player,
    ): Boolean {
        val party =
            getPartyByPlayer(owner) ?: run {
                owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
                return false
            }
        if (party.partyAdmin != owner) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return false
        }
        if (!party.members.contains(target)) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_TARGET_NOT_IN_PARTY, "{player}" to target.name)
            return false
        }
        party.partyAdmin = target
        target.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE)
        party.members.forEach { member ->
            if (member != party.partyAdmin) {
                member.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE_BROADCAST, "{player}" to target.name)
            }
        }
        return true
    }

    fun getMembers(partyData: PartyData): MutableSet<Player> = partyData.members

    fun isOwner(player: Player): Boolean {
        val partyData = getPartyByPlayer(player) ?: return false
        return partyData.partyAdmin == player
    }

    fun isInParty(player: Player): Boolean {
        for (party in parties) {
            if (party.members.contains(player)) {
                return true
            }
        }
        return false
    }

    fun getPartyByPlayer(player: Player): PartyData? {
        for (party in parties) {
            if (party.members.contains(player)) {
                return party
            }
        }
        return null
    }
}
