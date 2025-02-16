package org.gourmet.gourPillars.managers

import org.bukkit.entity.Player
import org.gourmet.gourPillars.managers.arena.toMini

class PartyManager {

    //todo stringa identificativa party
    private val prefix = "<bold><green>Party </bold><green>|"
    private val parties: MutableSet<PartyData> = mutableSetOf()

    fun createParty(creator: Player){
        if(isInParty(creator)){
            creator.sendMessage("$prefix <red>Sei gia in un party".toMini())
            return
        }
        creator.sendMessage("$prefix <yellow>Hai creato un party!".toMini())
        val newParty = PartyData(creator)
        newParty.members.add(creator)
        parties.add(newParty)

    }

    fun disbandParty(partyData: PartyData) {



        if (partyData.partyAdmin !in partyData.members) {
            partyData.partyAdmin.sendMessage("$prefix <red>Non sei l'owner del party!".toMini())
            return
        }

        for (member in partyData.members) {
            member.sendMessage("$prefix <yellow>Il party è stato sciolto".toMini())
        }

        partyData.members.clear()

        // 🔥 Metodo alternativo per rimuovere il party dalla lista
        parties.removeIf { it.partyAdmin == partyData.partyAdmin }

    }

    fun addMember(owner: Player, target: Player){
        val party = getPartyByPlayer(owner) ?: run {
            owner.sendMessage("$prefix <red>Non sei in nessun party".toMini())
            return
        }
        if(isInParty(target)){
            owner.sendMessage("$prefix <red>Questo utente e' gia in un party".toMini())
            return
        }
        if(party.partyAdmin != owner){
            owner.sendMessage("$prefix <red>Non sei l'admin del party".toMini())
            return
        }
        if(party.members.size >= 8) {
            owner.sendMessage("$prefix<red>Puoi invitare fino a 7 persone nel tuo party".toMini())
            return
        }
        party.members.forEach { player ->
            player.sendMessage("$prefix <white>${target.name} <yellow>e' entrato nel party!".toMini())
        }
        target.sendMessage("$prefix <yellow>Sei entrato nel party di <white>${owner.name}!".toMini())
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
            owner.sendMessage("$prefix <red>Non sei in nessun party".toMini())
            return false
        }
        if(party.partyAdmin != owner){
            owner.sendMessage("$prefix<red>Non sei owner del party".toMini())
            return false
        }
        if(!party.members.contains(target)){
            owner.sendMessage("$prefix ${target.name} non e' nel party".toMini())
            return false
        }
        party.members.forEach { player ->
            player.sendMessage("$prefix <white>${target.name} <yellow>e' uscito dal party!".toMini())
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
            player.sendMessage("$prefix <red>Non sei in nessun party".toMini())
            return
        }
        if(player == party.partyAdmin){
            if(party.members.size <= 1){
                parties.remove(party)
                player.sendMessage("$prefix <green>Party vuoto eliminato".toMini())
                return
            }
            party.members.remove(player)
            val first = party.members.first()
            party.partyAdmin = first
            first.sendMessage("$prefix <green>Sei stato promosso ad owner del party!".toMini())
            party.members.forEach { member ->
                if(member != first)
                    member.sendMessage("$prefix <green>${first.name} e' il nuovo owner del party!".toMini())
            }
        } else {
            party.members.remove(player)
            party.members.forEach { member ->
                member.sendMessage("$prefix <red>${player.name} e' uscito dal party".toMini())
            }
            player.sendMessage("$prefix <green>Sei uscito dal party".toMini())
        }

    }

    fun promote(owner: Player, target: Player): Boolean {
        val party = getPartyByPlayer(owner) ?: return false
        party.partyAdmin = target
        target.sendMessage("$prefix <green>Sei il nuovo owner del party!".toMini())
        party.members.forEach { member ->
            if(member != party.partyAdmin)
                member.sendMessage("$prefix <white>${target.name} <yellow>e' il nuovo owner del party!".toMini())
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