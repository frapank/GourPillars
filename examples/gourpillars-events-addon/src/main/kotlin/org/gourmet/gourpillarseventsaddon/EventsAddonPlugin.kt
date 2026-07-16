package org.gourmet.gourpillarseventsaddon

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.gourmet.gourPillars.api.GourPillarsAPI
import org.gourmet.gourPillars.api.event.GameEventDefinition

/**
 * Game events for GourPillars: lava, knockback, border, meteors and low gravity,
 * registered through the GourPillars event API. Each event can be disabled and
 * tuned from this plugin's config.yml.
 */
class EventsAddonPlugin : JavaPlugin() {
    private val miniMessage = MiniMessage.miniMessage()

    override fun onEnable() {
        saveDefaultConfig()

        val api = Bukkit.getServicesManager().load(GourPillarsAPI::class.java)
        if (api == null) {
            logger.warning("GourPillarsAPI is not registered, disabling.")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        if (config.getBoolean("lava.enabled", true)) {
            registerOrWarn(
                api,
                LavaGameEvent(
                    plugin = this,
                    displayName = displayName("lava.name", "<yellow>Lava event"),
                    lore = lore("lava.lore"),
                    slot = config.getInt("lava.slot", 11),
                    riseIntervalSeconds = config.getLong("lava.rise-interval-seconds", 4L).coerceAtLeast(1L),
                ),
            )
        }

        if (config.getBoolean("knockback.enabled", true)) {
            val registered =
                registerOrWarn(
                    api,
                    KnockbackGameEvent(
                        displayName = displayName("knockback.name", "<green>Knockback Event"),
                        lore = lore("knockback.lore"),
                        slot = config.getInt("knockback.slot", 12),
                    ),
                )
            if (registered) {
                server.pluginManager.registerEvents(
                    KnockbackListener(api, config.getDouble("knockback.multiplier", 2.0)),
                    this,
                )
            }
        }

        if (config.getBoolean("border.enabled", true)) {
            registerOrWarn(
                api,
                BorderGameEvent(
                    plugin = this,
                    displayName = displayName("border.name", "<green>Border Event"),
                    lore = lore("border.lore"),
                    slot = config.getInt("border.slot", 13),
                    finalSize = config.getDouble("border.final-size", 10.0),
                    shrinkIntervalSeconds = config.getLong("border.shrink-interval-seconds", 7L).coerceAtLeast(1L),
                    damageAmount = config.getDouble("border.damage-amount", 3.0),
                ),
            )
        }

        if (config.getBoolean("meteors.enabled", true)) {
            registerOrWarn(
                api,
                MeteorsGameEvent(
                    plugin = this,
                    displayName = displayName("meteors.name", "<red>Meteors"),
                    lore = lore("meteors.lore"),
                    slot = config.getInt("meteors.slot", 14),
                    intervalSeconds = config.getLong("meteors.interval-seconds", 8L).coerceAtLeast(1L),
                    meteorsPerWave = config.getInt("meteors.meteors-per-wave", 2).coerceAtLeast(1),
                    explosionPower = config.getDouble("meteors.explosion-power", 1.5).toFloat().coerceIn(0f, 8f),
                    setFire = config.getBoolean("meteors.set-fire", false),
                ),
            )
        }

        if (config.getBoolean("low-gravity.enabled", true)) {
            registerOrWarn(
                api,
                LowGravityGameEvent(
                    displayName = displayName("low-gravity.name", "<aqua>Low gravity"),
                    lore = lore("low-gravity.lore"),
                    slot = config.getInt("low-gravity.slot", 9),
                    jumpBoostLevel = config.getInt("low-gravity.jump-boost-level", 3).coerceIn(0, 10),
                ),
            )
        }

        logger.info("GourPillars events registered: ${api.getRegisteredEvents().joinToString { it.id }}")
    }

    // GourPillars unregisters this plugin's events automatically when it is disabled.

    private fun registerOrWarn(
        api: GourPillarsAPI,
        event: GameEventDefinition,
    ): Boolean {
        val registered = api.registerEvent(this, event)
        if (!registered) {
            logger.warning("Game event id '${event.id}' is already registered by another plugin, skipping it.")
        }
        return registered
    }

    private fun displayName(
        path: String,
        default: String,
    ): Component = miniMessage.deserialize(config.getString(path, default)!!)

    private fun lore(path: String): List<Component> = config.getStringList(path).map { miniMessage.deserialize(it) }
}
