package org.gourmet.gourPillars.guis

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.arena.EventSelector
import org.gourmet.gourPillars.managers.game.arena.GameEvents
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.toMini
import java.util.*

object VoteInventory {
    private data class VoteItemConfig(
        val slot: Int,
        val material: Material,
        val headTexture: String?,
        val name: Component,
        val lore: List<Component>,
    )

    fun displayInventory(player: Player) {
        val config = GourPillars.instance.config

        val rawSize = config.getInt("gui.vote.size", 27)
        val size = rawSize.coerceIn(9, 54).let { it - it % 9 }.coerceAtLeast(9)
        val title = config.getString("gui.vote.title", "<light_purple>Vote")!!.toMini()

        val inventory: Inventory = Bukkit.createInventory(null, size, title)

        val fillerMaterialName = config.getString("gui.vote.filler.material", "GRAY_STAINED_GLASS_PANE")!!
        val fillerMaterial =
            Material.matchMaterial(fillerMaterialName) ?: run {
                Logger.warning("Invalid material '$fillerMaterialName' for gui.vote.filler.material, using GRAY_STAINED_GLASS_PANE")
                Material.GRAY_STAINED_GLASS_PANE
            }
        val fillerName = config.getString("gui.vote.filler.name", "<gray>────</gray>")!!.toMini()
        val fillerPane =
            ItemStack(fillerMaterial).apply {
                itemMeta = itemMeta?.apply { displayName(fillerName) }
            }
        for (i in 0 until size) inventory.setItem(i, fillerPane)

        val itemsSection = config.getConfigurationSection("gui.vote.items")
        if (itemsSection == null) {
            player.openInventory(inventory)
            return
        }

        for (id in itemsSection.getKeys(false)) {
            val requiredEvent = GameEvents.fromVoteItemId(id)
            if (requiredEvent != null && !EventSelector.isEnabled(requiredEvent)) continue

            val itemConfig = loadItemConfig(itemsSection, id) ?: continue
            if (itemConfig.slot !in 0 until size) {
                Logger.warning("Skipping vote gui item '$id': slot ${itemConfig.slot} is out of bounds for a size $size inventory")
                continue
            }

            inventory.setItem(itemConfig.slot, createItemStack(itemConfig, id))
        }

        player.openInventory(inventory)
    }

    // Deliberately independent from loadItemConfig: a bad material/slot shouldn't stop
    // EventSelectionAnimationTask from showing this item's configured name.
    fun nameFor(id: String): Component? {
        val section = GourPillars.instance.config.getConfigurationSection("gui.vote.items") ?: return null
        val itemSection = section.getConfigurationSection(id) ?: return null
        return itemSection.getString("name", id)!!.toMini()
    }

    private fun loadItemConfig(
        itemsSection: ConfigurationSection,
        id: String,
    ): VoteItemConfig? {
        val itemSection = itemsSection.getConfigurationSection(id) ?: return null

        val slot = itemSection.getInt("slot", -1)
        if (slot < 0) {
            Logger.warning("Skipping vote gui item '$id': missing or invalid slot")
            return null
        }

        val materialName = itemSection.getString("material", "PLAYER_HEAD")!!
        val material =
            Material.matchMaterial(materialName) ?: run {
                Logger.warning("Skipping vote gui item '$id': unknown material '$materialName'")
                return null
            }

        val headTexture = itemSection.getString("head-texture")
        val name = itemSection.getString("name", id)!!.toMini()
        val lore = itemSection.getStringList("lore").map { it.toMini() }

        return VoteItemConfig(slot, material, headTexture, name, lore)
    }

    private fun createItemStack(
        itemConfig: VoteItemConfig,
        tag: String,
    ): ItemStack {
        val item = ItemStack(itemConfig.material, 1)
        val meta = item.itemMeta ?: return item

        meta.displayName(itemConfig.name)
        meta.lore(itemConfig.lore)

        if (meta is SkullMeta && !itemConfig.headTexture.isNullOrBlank()) {
            val profile = Bukkit.createProfile(UUID.randomUUID())
            profile.setProperty(ProfileProperty("textures", itemConfig.headTexture))
            meta.ownerProfile = profile
        }

        val key = NamespacedKey(GourPillars.instance, tag)
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, "true")

        item.itemMeta = meta
        return item
    }
}
