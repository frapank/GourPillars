package org.gourmet.gourPillars.listener.lobby

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.commands.BuildCMD
import org.gourmet.gourPillars.managers.LobbyConfig

class WorldChangeListener : Listener {
    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (!LobbyConfig.worldProtection) return
        if (isSpawnWorld(e.block.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        if (!LobbyConfig.worldProtection) return
        if (isSpawnWorld(e.block.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onSignChange(e: SignChangeEvent) {
        if (!LobbyConfig.worldProtection) return
        if (isSpawnWorld(e.block.world) && (!BuildCMD.buildSessionPlayers.contains(e.player))) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (!LobbyConfig.worldProtection) return
        if (!isSpawnWorld(e.player.world)) return
        if (BuildCMD.buildSessionPlayers.contains(e.player)) return

        val item = e.item
        val isRightClick = e.action == Action.RIGHT_CLICK_BLOCK || e.action == Action.RIGHT_CLICK_AIR
        if (isRightClick && item != null && item.type.name.endsWith("_SPAWN_EGG")) {
            e.isCancelled = true
            return
        }

        val block = e.clickedBlock ?: return
        if (e.action.isLeftClick && PLANT_MATERIALS.contains(block.type)) {
            e.isCancelled = true
        }
    }

    // Remove damage
    @EventHandler
    fun onAnyDamage(e: EntityDamageEvent) {
        if (!LobbyConfig.damageProtection) return
        val entity = e.entity
        if (entity is Player && isSpawnWorld(e.entity.world)) {
            e.isCancelled = true
        }
    }

    // Remove damage
    @EventHandler
    fun onPlayerVsPlayerDamage(e: EntityDamageByEntityEvent) {
        if (!LobbyConfig.damageProtection) return
        val damager = e.damager
        val target = e.entity
        if (damager is Player && target is Player) {
            if (isSpawnWorld(target.world)) {
                e.isCancelled = true
            }
        }
    }

    private fun isSpawnWorld(eventWorld: World): Boolean = GourPillars.spawnManager.getConfiguredWorld() == eventWorld

    companion object {
        private val PLANT_MATERIALS =
            setOf(
                Material.TALL_GRASS,
                Material.FERN,
                Material.LARGE_FERN,
                Material.DANDELION,
                Material.POPPY,
                Material.BLUE_ORCHID,
                Material.ALLIUM,
                Material.AZURE_BLUET,
                Material.RED_TULIP,
                Material.ORANGE_TULIP,
                Material.WHITE_TULIP,
                Material.PINK_TULIP,
                Material.OXEYE_DAISY,
                Material.SUNFLOWER,
                Material.LILAC,
                Material.ROSE_BUSH,
                Material.PEONY,
            )
    }
}
