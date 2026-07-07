package org.gourmet.gourPillars.managers.party

import org.bukkit.entity.Player

data class PartyData(
    var partyAdmin: Player,
    val members: MutableSet<Player> = mutableSetOf(),
)
