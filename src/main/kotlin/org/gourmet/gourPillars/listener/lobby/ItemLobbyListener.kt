package org.gourmet.gourPillars.listener.lobby

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Sign
import org.bukkit.entity.GlowItemFrame
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.commands.BuildCMD
import org.gourmet.gourPillars.managers.LobbyConfig
import org.gourmet.gourPillars.managers.game.arena.State

class ItemLobbyListener : Listener {
    private val arenaManager = GourPillars.arenaManager

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) return
        if (!isSpawnWorld(e.player.location.world)) return

        if (LobbyConfig.lobbyItems) {
            val item = e.item
            val meta = item?.itemMeta
            val key = NamespacedKey(GourPillars.instance, "lobby_command")
            val command = meta?.persistentDataContainer?.get(key, PersistentDataType.STRING)
            if (command != null) {
                e.isCancelled = true
                e.player.performCommand(command)
            }
        }

        if (!LobbyConfig.worldProtection) return

        val action = e.action
        val block = e.clickedBlock ?: return

        val type = block.type
        val state = block.state

        if (state is Sign) {
            if (!BuildCMD.buildSessionPlayers.contains(e.player)) {
                e.isCancelled = true
            }
        }

        // incomplete - Pot interaction
        if (type == Material.FLOWER_POT || type.name.startsWith("POTTED_")) {
            if (isSpawnWorld(e.player.location.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
                e.isCancelled = true
            }
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            val item = e.item
            if (item != null && block.type == Material.FLOWER_POT &&
                (
                    item.type.name.contains("SAPLING", true) ||
                        item.type.name.contains("FLOWER", true) ||
                        item.type.name.contains("MUSHROOM", true) ||
                        item.type.name.contains("FERN", true) ||
                        item.type.name.contains("CACTUS", true) ||
                        item.type.name.contains("BAMBOO", true)
                )
            ) {
                if (isSpawnWorld(e.player.location.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
                    e.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onDropItem(e: PlayerDropItemEvent) {
        if (!LobbyConfig.itemProtection) return
        if (isSpawnWorld(e.player.location.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (!LobbyConfig.itemProtection) return
        if (isSpawnWorld(e.whoClicked.location.world) && (!BuildCMD.buildSessionPlayers.contains(e.whoClicked as Player))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onSwapHands(e: PlayerSwapHandItemsEvent) {
        if (LobbyConfig.itemProtection && isSpawnWorld(e.player.location.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
            e.isCancelled = true
        }

        val arena = arenaManager.getArenaByPlayer(e.player)

        if (arena?.gameState == State.INGAME || arena?.gameState == State.STARTING) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockDropItem(e: BlockDropItemEvent) {
        if (!LobbyConfig.worldProtection) return
        if (isSpawnWorld(e.player.location.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onItemFrameInteract(e: PlayerInteractEntityEvent) {
        if (!LobbyConfig.worldProtection) return
        if (e.rightClicked is ItemFrame && isSpawnWorld(e.player.location.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onItemFrameDamage(e: EntityDamageByEntityEvent) {
        if (!LobbyConfig.worldProtection) return
        if (e.entity is ItemFrame && e.damager is Player && isSpawnWorld(e.entity.location.world) &&
            (!BuildCMD.buildSessionPlayers.contains(e.damager as Player))
        ) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onHangingBreak(event: HangingBreakByEntityEvent) {
        if (!LobbyConfig.worldProtection) return
        val entity = event.entity
        if (entity is ItemFrame || entity is Painting || entity is GlowItemFrame) {
            if (event.remover is Player) {
                val player = event.remover as Player
                if (isSpawnWorld(event.remover.location.world) && !BuildCMD.buildSessionPlayers.contains(player)) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onHangingPlace(event: HangingPlaceEvent) {
        if (!LobbyConfig.worldProtection) return
        val entity = event.entity
        if ((entity is ItemFrame || entity is Painting || entity is GlowItemFrame) && isSpawnWorld(event.player!!.location.world) &&
            !BuildCMD.buildSessionPlayers.contains(event.player)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSleep(event: PlayerBedEnterEvent) {
        event.isCancelled = true
    }

    private fun isSpawnWorld(eventWorld: World): Boolean = GourPillars.spawnManager.getConfiguredWorld() == eventWorld
}
