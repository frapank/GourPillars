package org.gourmet.gourPillars.listener.lobby

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars

class ItemLobbyEvent : Listener{

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) return
        if (!isSpawnWorld(e.player.location.world)) return

        val item = e.item ?: return
        val meta = item.itemMeta ?: return
        val key = NamespacedKey(GourPillars.instance, "lobby_command")
        val command = meta.persistentDataContainer.get(key, PersistentDataType.STRING) ?: return

        e.isCancelled = true
        e.player.performCommand(command)
    }

    @EventHandler
    fun onDropItem(e: PlayerDropItemEvent) {
        if (isSpawnWorld(e.player.location.world)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (isSpawnWorld(e.whoClicked.location.world)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onSwapHands(e: PlayerSwapHandItemsEvent) {
        if (isSpawnWorld(e.player.location.world)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockDropItem(e: BlockDropItemEvent) {
        if (isSpawnWorld(e.player.location.world)) {
            e.isCancelled = true
        }
    }

    private fun isSpawnWorld(eventWorld: World): Boolean {
        return GourPillars.spawnManager.getConfiguredWorld() == eventWorld
    }

}
