package org.gourmet.gourPillars.other

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars

object Utils {

    fun setGlass(put: Boolean, location: Location) {
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


    fun giveLobbyItems(player: org.bukkit.entity.Player) {
        val inv = player.inventory
        inv.clear()

        inv.setItem(4, createNamedCompass("<gold>✩ <white><bold>ѕᴇʟᴇᴛᴛᴏʀᴇ ᴍᴏᴅᴀʟɪᴛᴀ' <gold>✩", "dm open serverselector", Material.COMPASS))
        inv.setItem(6, createNamedCompass("<gold>✩ <white><bold>ᴄᴏѕᴍᴇᴛɪᴄɪ <gold>✩", null, Material.EMERALD))
        inv.setItem(2, createNamedCompass("<gold>✩ <white><bold>ᴘᴀʀᴛɪᴛᴀ ᴄᴀѕᴜᴀʟᴇ <gold>✩", "joinrandom", Material.NETHER_STAR))
    }

    private fun createNamedCompass(name: String, command: String?, material: Material): ItemStack {
        val compass = ItemStack(material, 1)
        val meta: ItemMeta = compass.itemMeta!!
        meta.displayName(name.toMini())
        if (command != null) {
            val key = NamespacedKey(GourPillars.instance, "lobby_command")
            meta.persistentDataContainer.set(key, PersistentDataType.STRING, command)
        }
        compass.itemMeta = meta
        return compass
    }

}
val miniMessage = MiniMessage.builder().build()
fun String.toMini(): Component = miniMessage.deserialize(this)
