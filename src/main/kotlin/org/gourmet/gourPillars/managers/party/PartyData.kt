package org.gourmet.gourPillars.managers.party

import org.bukkit.entity.Player

// Not a data class: it's stored in a HashSet, and equals/hashCode based on the
// mutable members/partyAdmin fields would break removal from that set.
class PartyData(
    var partyAdmin: Player,
    val members: MutableSet<Player> = mutableSetOf(),
    var isPublic: Boolean = false,
)
