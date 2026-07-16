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
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.managers.game.GameEventRegistry
import org.gourmet.gourPillars.other.Logger
import org.gourmet.gourPillars.other.toMini
import java.util.*

object VoteInventory {
    // PDC key on clickable vote items; its value is the vote option id ("no-event",
    // "day-vote", "night-vote" or a registered game event id).
    const val VOTE_OPTION_TAG = "vote-option"

    private const val NO_EVENT_HEAD =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGEyZmUwMWExZjdkNzZmM2NkNmRkYjUzZDUzMjVhMzk4YWQ3NDhkNzE4YWU3MjBhNmJjMjMzODI4NjdkNjUzMSJ9fX0="
    private const val DAY_HEAD =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTUxZTUyYjFkZTk3YjUzNDM1OGI1M2NkMmM5YzQ1NTI2MDg2ZDJhNmYwZTBhZTY1ZTRiYTJmZjVjNjI5MGVjIn19fQ=="
    private const val NIGHT_HEAD =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzZiMzIxNDRlNzAzMjcyNjE0MTdhYTEwN2U0Y2YyYjMyNTMxNWEzMmYxZDgxYzIxZDE5ZjBmNzhjNjEwMzhhOSJ9fX0="

    private data class BuiltInDefaults(
        val slot: Int,
        val headTexture: String,
        val name: String,
        val lore: List<String>,
    )

    // Fallbacks for the three built-in options: every field can be overridden from
    // gui.vote.items, but a missing or broken entry never removes the option from the
    // GUI (set "enabled: false" on the entry to hide it on purpose).
    private val BUILT_IN_ITEMS =
        mapOf(
            GameEventRegistry.NO_EVENT_ID to
                BuiltInDefaults(10, NO_EVENT_HEAD, "<red>No event", listOf("<gray>Vote for a classic game!")),
            "day-vote" to BuiltInDefaults(15, DAY_HEAD, "<aqua>Day time", listOf("<gray>A bright game!")),
            "night-vote" to BuiltInDefaults(16, NIGHT_HEAD, "<yellow>Night event", listOf("<gray>A nighttime game!")),
        )

    // The GUI is rebuilt on every open; each config problem is only logged once.
    private val warnedConfigIssues = mutableSetOf<String>()

    private class ResolvedItem(
        val preferredSlot: Int?,
        val stack: ItemStack,
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
                warnOnce("filler", "Invalid material '$fillerMaterialName' for gui.vote.filler.material, using GRAY_STAINED_GLASS_PANE")
                Material.GRAY_STAINED_GLASS_PANE
            }
        val fillerName = config.getString("gui.vote.filler.name", "<gray>────</gray>")!!.toMini()
        val fillerPane =
            ItemStack(fillerMaterial).apply {
                itemMeta = itemMeta?.apply { displayName(fillerName) }
            }
        for (i in 0 until size) inventory.setItem(i, fillerPane)

        val itemsSection = config.getConfigurationSection("gui.vote.items")
        warnUnknownEntries(itemsSection)

        val items = mutableListOf<ResolvedItem>()
        for ((id, defaults) in BUILT_IN_ITEMS) {
            resolveBuiltIn(itemsSection?.getConfigurationSection(id), id, defaults)?.let(items::add)
        }
        for (entry in GourPillars.gameEventRegistry.voteEntries()) {
            items.add(ResolvedItem(entry.voteItem.preferredSlot, createEventItemStack(entry)))
        }

        val usedSlots = mutableSetOf<Int>()
        for (item in items) {
            val slot = pickSlot(item.preferredSlot, usedSlots, size)
            if (slot == null) {
                warnOnce("full", "The vote gui (size $size) has no free slot left for every vote option, enlarge gui.vote.size")
                break
            }
            usedSlots.add(slot)
            inventory.setItem(slot, item.stack)
        }

        player.openInventory(inventory)
    }

    // Name shown by EventSelectionAnimationTask for the built-in options.
    fun nameFor(id: String): Component? {
        val defaults = BUILT_IN_ITEMS[id] ?: return null
        val itemSection = GourPillars.instance.config.getConfigurationSection("gui.vote.items.$id")
        return (itemSection?.getString("name", defaults.name) ?: defaults.name).toMini()
    }

    private fun resolveBuiltIn(
        section: ConfigurationSection?,
        id: String,
        defaults: BuiltInDefaults,
    ): ResolvedItem? {
        if (section?.getBoolean("enabled", true) == false) return null

        val slot = section?.getInt("slot", defaults.slot) ?: defaults.slot
        val materialName = section?.getString("material", "PLAYER_HEAD") ?: "PLAYER_HEAD"
        val material =
            Material.matchMaterial(materialName) ?: run {
                warnOnce("material-$id", "Invalid material '$materialName' for gui.vote.items.$id, using PLAYER_HEAD")
                Material.PLAYER_HEAD
            }
        val headTexture = section?.getString("head-texture", defaults.headTexture) ?: defaults.headTexture
        val name = (section?.getString("name", defaults.name) ?: defaults.name).toMini()
        val lore = (section?.getStringList("lore")?.takeIf { it.isNotEmpty() } ?: defaults.lore).map { it.toMini() }

        val item = ItemStack(material, 1)
        val meta = item.itemMeta ?: return ResolvedItem(slot, item)
        meta.displayName(name)
        meta.lore(lore)
        applyHeadTexture(meta, headTexture)
        tagVoteOption(meta, id)
        item.itemMeta = meta

        return ResolvedItem(slot, item)
    }

    private fun createEventItemStack(entry: GameEventRegistry.VoteEntry): ItemStack {
        val item = ItemStack(entry.voteItem.material, 1)
        val meta = item.itemMeta ?: return item

        meta.displayName(entry.displayName)
        meta.lore(entry.voteItem.lore)
        applyHeadTexture(meta, entry.voteItem.headTexture)
        tagVoteOption(meta, entry.id)

        item.itemMeta = meta
        return item
    }

    // Preferred slot when free and in bounds, otherwise the first free slot starting
    // from the second row (the first row usually stays filler). Null when the GUI is full.
    private fun pickSlot(
        preferred: Int?,
        usedSlots: Set<Int>,
        size: Int,
    ): Int? {
        if (preferred != null && preferred in 0 until size && preferred !in usedSlots) return preferred
        val scanOrder = (9 until size) + (0 until 9)
        return scanOrder.firstOrNull { it !in usedSlots }
    }

    private fun warnUnknownEntries(itemsSection: ConfigurationSection?) {
        if (itemsSection == null) return
        for (id in itemsSection.getKeys(false)) {
            if (id !in BUILT_IN_ITEMS && warnedConfigIssues.add("unknown-$id")) {
                Logger.warning("Ignoring unknown vote gui item '$id' in config.yml: game events add their own item automatically")
            }
        }
    }

    private fun warnOnce(
        key: String,
        message: String,
    ) {
        if (warnedConfigIssues.add(key)) Logger.warning(message)
    }

    private fun applyHeadTexture(
        meta: ItemMeta,
        headTexture: String?,
    ) {
        if (meta is SkullMeta && !headTexture.isNullOrBlank()) {
            val profile = Bukkit.createProfile(UUID.randomUUID())
            profile.setProperty(ProfileProperty("textures", headTexture))
            meta.ownerProfile = profile
        }
    }

    private fun tagVoteOption(
        meta: ItemMeta,
        optionId: String,
    ) {
        val key = NamespacedKey(GourPillars.instance, VOTE_OPTION_TAG)
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, optionId)
    }
}
