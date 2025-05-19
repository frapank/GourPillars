package org.gourmet.gourPillars.managers.party

import org.bukkit.entity.Player
import org.gourmet.gourPillars.managers.party.PartyData
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage

class PartyManager {

    //todo stringa identificativa party
    //private val prefix = "<bold><green>Party </bold><green>|"
    private val parties: MutableSet<PartyData> = mutableSetOf()

    fun createParty(creator: Player){
        if(isInParty(creator)){
            creator.sendDynamicMessage(MessageData.PARTY_ERRORS_USER_ALREADY_IN_PARTY)
            return
        }
        creator.sendDynamicMessage(MessageData.PARTY_PARTY_CREATED)
        val newParty = PartyData(creator)
        newParty.members.add(creator)
        parties.add(newParty)

    }

    fun disbandParty(partyData: PartyData) {

        if (partyData.partyAdmin !in partyData.members) {
            partyData.partyAdmin.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }

        for (member in partyData.members) {
            member.sendDynamicMessage(MessageData.PARTY_PARTY_DISBAND)
        }

        partyData.members.clear()

        // 🔥 Metodo alternativo per rimuovere il party dalla lista
        parties.removeIf { it.partyAdmin == partyData.partyAdmin }

    }

    fun addMember(owner: Player, target: Player){
        val party = getPartyByPlayer(owner) ?: run {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
            return
        }
        if(isInParty(target)){
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_USER_ALREADY_IN_PARTY)
            return
        }
        if(party.partyAdmin != owner){
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return
        }
        if(party.members.size >= 8) {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_MAX_PARTY_MEMBER)
            return
        }
        party.members.forEach { player ->
            player.sendDynamicMessage(MessageData.PARTY_PLAYER_JOINED_BROADCAST, "{player}" to target.name)
        }
        target.sendDynamicMessage(MessageData.PARTY_PLAYER_JOINED, "{player}" to owner.name)
        party.members.add(target)
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
    fun kickPlayerFromParty(owner: Player, target: Player): Boolean {
        val party = getPartyByPlayer(owner) ?: run {
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_PLAYER_NOT_IN_PARTY)
            return false
        }
        if(party.partyAdmin != owner){
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_PARTY_ADMIN)
            return false
        }
        if(!party.members.contains(target)){
            //owner.sendMessage("$prefix ${target.name} non e' nel party".toMini())
            owner.sendDynamicMessage(MessageData.PARTY_ERRORS_TARGET_NOT_IN_PARTY, "{player}" to target.name)
            return false
        }
        party.members.forEach { player ->
            //player.sendMessage("$prefix <white>${target.name} <yellow>e' uscito dal party!".toMini())
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
    fun leaveParty(player: Player){
        val party = getPartyByPlayer(player) ?: run {
            player.sendDynamicMessage(MessageData.PARTY_ERRORS_NOT_IN_PARTY)
            return
        }
        if(player == party.partyAdmin){
            if(party.members.size <= 1){
                parties.remove(party)
                player.sendDynamicMessage(MessageData.PARTY_PARTY_DISBAND)
                return
            }
            party.members.remove(player)
            val first = party.members.first()
            party.partyAdmin = first
            first.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE)
            party.members.forEach { member ->
                if(member != first)
                    //member.sendMessage("$prefix <green>${first.name} e' il nuovo owner del party!".toMini())
                    member.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE_BROADCAST, "{player}" to first.name)
            }
        } else {
            party.members.remove(player)
            party.members.forEach { member ->
                //member.sendMessage("$prefix <red>${player.name} e' uscito dal party".toMini())
                member.sendDynamicMessage(MessageData.PARTY_USER_LEFT_PARTY, "{player}" to player.name)
            }
            player.sendDynamicMessage(MessageData.PARTY_PARTY_LEAVE)
        }

    }

    fun promote(owner: Player, target: Player): Boolean {
        val party = getPartyByPlayer(owner) ?: return false
        party.partyAdmin = target
        target.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE)
        party.members.forEach { member ->
            if(member != party.partyAdmin)
                //member.sendMessage("$prefix <white>${target.name} <yellow>e' il nuovo owner del party!".toMini())
                member.sendDynamicMessage(MessageData.PARTY_PARTY_PROMOTE_BROADCAST, "{player}" to target.name)
        }
        return true
    }

    fun getMembers(partyData: PartyData): MutableSet<Player> {
        return partyData.members
    }

    fun isOwner(player: Player): Boolean {
        val partyData = getPartyByPlayer(player) ?: return false
        return partyData.partyAdmin == player
    }

    fun isInParty(player: Player): Boolean{
        for(party in parties){
            if(party.members.contains(player)){
                return true
            }
        }
        return false
    }

    fun getPartyByPlayer(player: Player): PartyData? {
        for(party in parties){
            if(party.members.contains(player)){
                return party
            }
        }
        return null
    }


}