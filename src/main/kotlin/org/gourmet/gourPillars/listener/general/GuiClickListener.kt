package org.gourmet.gourPillars.listener.general

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.guis.VoteInventory
import org.gourmet.gourPillars.managers.game.GameEventRegistry
import org.gourmet.gourPillars.managers.game.arena.Arena
import org.gourmet.gourPillars.managers.game.arena.State
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage

class GuiClickListener : Listener {
    private val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (arena.gameState != State.WAITING && arena.gameState != State.STARTING) {
            return
        }

        val item = event.item ?: return

        if (event.hand != EquipmentSlot.HAND) return
        if (event.action.name.contains("RIGHT_CLICK")) {
            if (hasItemTag(item, "leave-item")) {
                player.inventory.clear()
                player.performCommand("leave")
                event.isCancelled = true
            }

            if (hasItemTag(item, "vote-item")) {
                VoteInventory.displayInventory(player)
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        // Votes are only open before the match starts.
        if (arena.gameState != State.WAITING && arena.gameState != State.STARTING) return
        event.isCancelled = true

        val item = event.currentItem ?: return
        when (val option = voteOptionOf(item) ?: return) {
            "day-vote" -> handleTimeVote(player, arena, day = true)
            "night-vote" -> handleTimeVote(player, arena, day = false)
            else -> handleEventVote(player, arena, option)
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (arena.gameState == State.WAITING || arena.gameState == State.STARTING) {
            event.isCancelled = true
        }
    }

    private fun handleTimeVote(
        player: Player,
        arena: Arena,
        day: Boolean,
    ) {
        if (arena.dayVote.contains(player) || arena.nightVote.contains(player)) {
            player.sendDynamicMessage(MessageData.ARENA_VOTE_ALREADY_VOTED_TIME)
            return
        }

        if (day) {
            arena.dayVote.add(player)
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_DAY_VOTED, "{player}" to player.name)
        } else {
            arena.nightVote.add(player)
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_NIGHT_VOTED, "{player}" to player.name)
        }
    }

    private fun handleEventVote(
        player: Player,
        arena: Arena,
        optionId: String,
    ) {
        val displayName =
            if (optionId == GameEventRegistry.NO_EVENT_ID) {
                null
            } else {
                // The event got unregistered after this GUI was opened.
                GourPillars.gameEventRegistry.displayNameOf(optionId) ?: run {
                    player.sendDynamicMessage(MessageData.ARENA_VOTE_EVENT_DISABLED)
                    return
                }
            }

        if (arena.eventVotes.containsKey(player.uniqueId)) {
            player.sendDynamicMessage(MessageData.ARENA_VOTE_ALREADY_VOTED_EVENT)
            return
        }

        arena.eventVotes[player.uniqueId] = optionId
        if (displayName == null) {
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_CLASSIC_VOTED, "{player}" to player.name)
        } else {
            arena.sendDynamicMessageToPlayerInGame(
                MessageData.ARENA_VOTE_EVENT_VOTED,
                "{player}" to player.name,
                "{event}" to PlainTextComponentSerializer.plainText().serialize(displayName),
            )
        }
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if (arena.gameState == State.WAITING || arena.gameState == State.STARTING) {
            event.isCancelled = true
            return
        }
    }

    private fun voteOptionOf(item: ItemStack): String? {
        if (!item.hasItemMeta()) return null
        val meta = item.itemMeta ?: return null
        val key = NamespacedKey(GourPillars.instance, VoteInventory.VOTE_OPTION_TAG)
        return meta.persistentDataContainer.get(key, PersistentDataType.STRING)
    }

    private fun hasItemTag(
        item: ItemStack,
        tag: String,
    ): Boolean {
        if (!item.hasItemMeta()) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(GourPillars.instance, tag)
        val hasTag = meta.persistentDataContainer.has(key, PersistentDataType.STRING)

        return hasTag
    }
}
