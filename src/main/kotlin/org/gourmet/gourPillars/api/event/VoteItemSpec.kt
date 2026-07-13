package org.gourmet.gourPillars.api.event

import net.kyori.adventure.text.Component
import org.bukkit.Material

// Look of a registered event's item in the vote GUI. The item name is the event's
// displayName; only the icon, lore and slot are configurable here.
data class VoteItemSpec(
    val material: Material = Material.PAPER,
    // Base64 skin value (e.g. from minecraft-heads.com), used when material is PLAYER_HEAD.
    val headTexture: String? = null,
    val lore: List<Component> = emptyList(),
    // Slot in the vote GUI; used when free and in bounds, otherwise the first free
    // slot after the built-in items is picked.
    val preferredSlot: Int? = null,
)
