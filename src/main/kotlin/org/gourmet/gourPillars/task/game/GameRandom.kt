package org.gourmet.gourPillars.task.game

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.gourmet.gourPillars.GourPillars

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
        val nonPlacableMaterials =
            setOf(
                Material.COMMAND_BLOCK,
                Material.CHAIN_COMMAND_BLOCK,
                Material.REPEATING_COMMAND_BLOCK,
                Material.BARRIER,
                Material.STRUCTURE_BLOCK,
                Material.JIGSAW,
                Material.DEBUG_STICK,
                Material.KNOWLEDGE_BOOK,
                Material.LIGHT,
                Material.STRUCTURE_VOID,
                Material.END_PORTAL_FRAME,
                Material.END_PORTAL,
                Material.NETHER_PORTAL,
                Material.BEDROCK,
                Material.SPAWNER,
                Material.ENDER_DRAGON_SPAWN_EGG,
                Material.WITHER_SPAWN_EGG,
                Material.SNOWBALL,
                Material.SHIELD,
            )

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
}
