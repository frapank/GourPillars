package org.gourmet.gourPillars.guis

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import java.util.*

object VoteInventory {

    private val noEvent = createHeadItem(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGEyZmUwMWExZjdkNzZmM2NkNmRkYjUzZDUzMjVhMzk4YWQ3NDhkNzE4YWU3MjBhNmJjMjMzODI4NjdkNjUzMSJ9fX0=",
        "<red>No event",
        listOf("<gray>Voti per una partita classica!"),
        "no-event"
    )

    private val dayEvent = createHeadItem(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzZiMzIxNDRlNzAzMjcyNjE0MTdhYTEwN2U0Y2YyYjMyNTMxNWEzMmYxZDgxYzIxZDE5ZjBmNzhjNjEwMzhhOSJ9fX0=",
        "<aqua>Night time",
        listOf("<gray>Una partita illuminata!"),
        "day-vote"
    )

    private val nightEvent = createHeadItem(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTUxZTUyYjFkZTk3YjUzNDM1OGI1M2NkMmM5YzQ1NTI2MDg2ZDJhNmYwZTBhZTY1ZTRiYTJmZjVjNjI5MGVjIn19fQ==",
        "<yellow>Day event",
        listOf("<gray>Una partita notturna!"),
        "night-vote"
    )

    private val lavaEvent = createHeadItem(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjY5YTMyY2ZmZjAzMTU1YjlmODEwOTg1OGQ4MzAzYjA2ZmU3MGQwYjUzNWJhNjRiNTFkMDMwMmZmMzM5ZTBjYiJ9fX0=",
        "<yellow>Lava event",
        listOf("<gray>Tutti avranno un set di armatura!"),
        "lava-event"
    )

    private val knockbackEvent = createHeadItem(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjA2MWY5OGFhZmYxZTQwNmUwNmY2ZjEzZmZlMDYwMDU4NzNmM2QxZWFkZGIxYjU5ZTE5ZGRhMGVkOWZmYjI3MCJ9fX0=",
        "<green>Knockback Event",
        listOf("<gray>I giocatori avranno un", "<gray>knockback potenziato!"),
        "knockback-event"
    )

    fun displayInventory(player: Player) {
        val mm = MiniMessage.miniMessage()
        val inventory: Inventory = Bukkit.createInventory(null, 27, mm.deserialize("<light_purple>Vota 🌟"))

        val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { displayName(mm.deserialize("<gray>────</gray>")) }
        }
        for (i in 0 until 27) inventory.setItem(i, glassPane)


        inventory.setItem(10, noEvent)
        inventory.setItem(11, lavaEvent)
        inventory.setItem(12, knockbackEvent)
        inventory.setItem(15, dayEvent)
        inventory.setItem(16, nightEvent)

        player.openInventory(inventory)
    }

    private fun createHeadItem(base64: String, name: String, lore: List<String>, tag: String): ItemStack {
        val mm = MiniMessage.miniMessage()
        val item = ItemStack(Material.PLAYER_HEAD, 1)
        val meta = item.itemMeta as SkullMeta

        meta.displayName(mm.deserialize(name))
        meta.lore(lore.map { mm.deserialize(it) })

        val profile = Bukkit.createProfile(UUID.randomUUID())
        val textureProperty = ProfileProperty("textures", base64)
        profile.setProperty(textureProperty)

        meta.ownerProfile = profile

        val key = NamespacedKey(GourPillars.instance, tag)
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, "true")

        item.itemMeta = meta
        return item
    }

    private fun createItem(material: Material, name: String, lore: List<String>, tag: String): ItemStack {
        val mm = MiniMessage.miniMessage()
        return ItemStack(material).apply {
            itemMeta = itemMeta?.apply {
                displayName(mm.deserialize(name))
                lore(lore.map { mm.deserialize(it) })

                val key = NamespacedKey(GourPillars.instance, tag)

                persistentDataContainer.set(key, PersistentDataType.STRING, "true")

            }
        }
    }


}