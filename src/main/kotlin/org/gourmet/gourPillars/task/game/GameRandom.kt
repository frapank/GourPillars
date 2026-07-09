package org.gourmet.gourPillars.task.game

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars
import org.gourmet.gourPillars.other.Logger

object GameRandom {
    fun startRandomItemTask(
        alivePlayer: MutableSet<Player>,
        isRunning: () -> Boolean,
    ) {
        val intervalSeconds = GourPillars.instance.config.getDouble("game.random-item-interval-seconds", 3.5)
        val intervalTicks = (intervalSeconds * 20).toLong().coerceAtLeast(1L)

        object : BukkitRunnable() {
            override fun run() {
                if (!isRunning()) {
                    cancel()
                    return
                }

                alivePlayer.forEach { player ->
                    giveRandomItem(player)
                }
            }
        }.runTaskTimer(GourPillars.instance, 0L, intervalTicks)
    }

    fun giveRandomItem(player: Player) {
        val randomMaterial = getRandomMaterial()
        val itemStack = ItemStack(randomMaterial)

        player.inventory.addItem(itemStack)
    }

    fun getRandomMaterial(): Material {
        val nonPlacableMaterials = getExcludedMaterials()

        val armorTrimMaterials =
            Material.entries
                .filter { it.name.endsWith("_ARMOR_TRIM_SMITHING_TEMPLATE") }
                .toSet()

        val world = GourPillars.spawnManager.getConfiguredWorld() ?: Bukkit.getWorlds().first()
        val materials =
            Material.entries
                .filter { it.isItem }
                .filter { m: Material ->
                    !m.isEmpty && !m.isLegacy && m.isItem && m.asItemType()?.let {
                        world.isEnabled(
                            it,
                        )
                    } == true && m !in nonPlacableMaterials &&
                        m !in armorTrimMaterials
                }

        return materials.random()
    }

    private fun getExcludedMaterials(): Set<Material> =
        GourPillars.instance.config
            .getStringList("game.excluded-random-items")
            .mapNotNull { name ->
                Material.matchMaterial(name) ?: run {
                    Logger.warning("Invalid material '$name' in game.excluded-random-items, ignoring it")
                    null
                }
            }.toSet()
}
