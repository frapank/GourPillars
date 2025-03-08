package org.gourmet.gourPillars.listener

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.guis.VoteInventory
import org.gourmet.gourPillars.managers.arena.State
import org.gourmet.gourPillars.other.messages.MessageData
import org.gourmet.gourPillars.other.messages.sendDynamicMessage
import org.gourmet.gourPillars.other.toMini

class ClickItemEvent : Listener {

    private val arenaManager = GourPillars.arenaManager
    private val prefix = "<bold><aqua>Game </bold><gray>|"

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if(arena.gameState != State.WAITING && arena.gameState != State.STARTING){
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

        val player: Player = event.whoClicked as Player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        val item = event.currentItem ?: return

        if(arena.gameState == State.WAITING || arena.gameState == State.STARTING){
            event.isCancelled = true
        }

        if (hasItemTag(item, "knockback-event")) {
            if (arena.knockbackVote.contains(player) || arena.lavaEvent.contains(player) || arena.noEventVote.contains(player)) {
                //player.sendMessage("$prefix <red>Hai gia votato un evento!".toMini())
                player.sendDynamicMessage(MessageData.ARENA_VOTE_ALREADY_VOTED_EVENT)
                return
            }

            arena.knockbackVote.add(player)
            //arena.sendMessageToPlayerInGame("$prefix <green>${player.name} ha votato l'evento <yellow><bold>knockback")
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_KNOCKBACK_VOTED, "{player}" to player.name)
            return
        }

        if (hasItemTag(item, "lava-event")) {
            if (arena.knockbackVote.contains(player) || arena.lavaEvent.contains(player) || arena.noEventVote.contains(player)) {
                //player.sendMessage("$prefix <red>Hai gia votato un evento!".toMini())
                player.sendDynamicMessage(MessageData.ARENA_VOTE_ALREADY_VOTED_EVENT)
                return
            }

            arena.lavaEvent.add(player)
            //arena.sendMessageToPlayerInGame("$prefix <green>${player.name} ha votato l'evento <yellow><bold>lava")
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_LAVA_VOTED, "{player}" to player.name)
            return
        }
        if (hasItemTag(item, "no-event")) {
            if (arena.knockbackVote.contains(player) || arena.lavaEvent.contains(player) || arena.noEventVote.contains(player)) {
                //player.sendMessage("$prefix <red>Hai gia votato un evento!".toMini())
                player.sendDynamicMessage(MessageData.ARENA_VOTE_ALREADY_VOTED_EVENT)
                return
            }

            arena.noEventVote.add(player)
            //arena.sendMessageToPlayerInGame("$prefix <green>${player.name} ha votato per una partita <yellow><bold>classica")
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_CLASSIC_VOTED, "{player}" to player.name)
            return
        }

        if (hasItemTag(item, "day-vote")) {
            if (arena.dayVote.contains(player) || arena.nightVote.contains(player)) {
                //player.sendMessage("$prefix <red>Hai gia votato il tempo!".toMini())
                player.sendDynamicMessage(MessageData.ARENA_VOTE_ALREADY_VOTED_TIME)
                return
            }

            arena.dayVote.add(player)
            //arena.sendMessageToPlayerInGame("$prefix <green>${player.name} ha votato la <yellow><bold>notte")
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_NIGHT_VOTED, "{player}" to player.name)
            return
        }

        if (hasItemTag(item, "night-vote")) {
            if (arena.dayVote.contains(player) || arena.nightVote.contains(player)) {
                //player.sendMessage("$prefix <red>Hai gia votato il tempo!".toMini())
                player.sendDynamicMessage(MessageData.ARENA_VOTE_ALREADY_VOTED_TIME)
                return
            }

            arena.nightVote.add(player)
            //arena.sendMessageToPlayerInGame("$prefix <green>${player.name} ha votato il <yellow><bold>giorno")
            arena.sendDynamicMessageToPlayerInGame(MessageData.ARENA_VOTE_DAY_VOTED, "{player}" to player.name)
            return
        }

    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {

        val player = event.player
        val arena = arenaManager.getArenaByPlayer(player) ?: return

        if(arena.gameState == State.WAITING || arena.gameState == State.STARTING){
            event.isCancelled = true
            return
        }

    }

    private fun hasItemTag(item: ItemStack, tag: String): Boolean {
        if (!item.hasItemMeta()) return false

        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(GourPillars.instance, tag)
        val hasTag = meta.persistentDataContainer.has(key, PersistentDataType.STRING)

        return hasTag
    }

}