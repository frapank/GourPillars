package org.gourmet.gourPillars.other

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars

object Utils {
    fun setGlass(
        put: Boolean,
        location: Location,
    ) {
        val world: World = location.world ?: return
        val x = location.blockX
        val y = location.blockY - 1
        val z = location.blockZ
        val material = if (put) Material.GLASS else Material.AIR

        for (dx in -1..1) {
            for (dy in 0..3) {
                for (dz in -1..1) {
                    val block: Block = world.getBlockAt(x + dx, y + dy, z + dz)

                    if (dy == 0 || dy == 3) {
                        block.type = material
                    } else if (dx == -1 || dx == 1 || dz == -1 || dz == 1) {
                        block.type = material
                    }
                }
            }
        }
    }

    // Clears invulnerability, potion effects and fire so no state carries over between games/lobby
    fun resetPlayerState(player: Player) {
        player.isInvulnerable = false
        player.fireTicks = 0
        player.clearActivePotionEffects()
        player.health = 20.0
        player.foodLevel = 20
    }

    fun giveLobbyItems(player: Player) {
        val inv = player.inventory
        inv.clear()

        val section = GourPillars.instance.config.getConfigurationSection("lobby-items") ?: return
        for (key in section.getKeys(false)) {
            val itemSection = section.getConfigurationSection(key) ?: continue

            val slot = itemSection.getInt("slot", -1)
            if (slot !in 0..35) {
                Logger.warning("Skipping lobby item '$key': invalid or missing slot")
                continue
            }

            val materialName = itemSection.getString("material")
            val material = materialName?.let { Material.matchMaterial(it) }
            if (material == null) {
                Logger.warning("Skipping lobby item '$key': unknown material '$materialName'")
                continue
            }

            val name = itemSection.getString("name", "")!!
            val lore = itemSection.getStringList("lore")
            val command = itemSection.getString("command")

            inv.setItem(slot, createLobbyItem(material, name, lore, command))
        }
    }

    private fun createLobbyItem(
        material: Material,
        name: String,
        lore: List<String>,
        command: String?,
    ): ItemStack {
        val item = ItemStack(material, 1)
        val meta: ItemMeta = item.itemMeta!!

        meta.displayName(name.toMini())
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { it.toMini() })
        }
        if (!command.isNullOrBlank()) {
            val key = NamespacedKey(GourPillars.instance, "lobby_command")
            meta.persistentDataContainer.set(key, PersistentDataType.STRING, command)
        }

        item.itemMeta = meta
        return item
    }
}

val miniMessage = MiniMessage.builder().build()

fun String.toMini(): Component = miniMessage.deserialize(this)
