# API for other plugins

GourPillars exposes a small Bukkit-style API so other plugins can read arena/match state and react to matches. Everything lives under `org.gourmet.gourPillars.api`.

### Gradle

```kotlin
dependencies {
    compileOnly(files("/path/to/GourPillars-2.0.jar"))
}
```

### Maven

Install the jar into your local repository once:

```
mvn install:install-file -Dfile=GourPillars-2.0.jar -DgroupId=org.gourmet -DartifactId=GourPillars -Dversion=2.0 -Dpackaging=jar
```

Then reference it:

```xml
<dependency>
    <groupId>org.gourmet</groupId>
    <artifactId>GourPillars</artifactId>
    <version>2.0</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml

```yaml
depend: [GourPillars]
```

## Getting an instance

Either read the static field, or go through Bukkit's `ServicesManager` (no compile-time dependency on the `GourPillars` main class needed that way):

```kotlin
val api = GourPillars.api
// or
val api = Bukkit.getServicesManager().load(GourPillarsAPI::class.java) ?: return
```

## Threading

Every `GourPillarsAPI` method runs on the main server thread and must be called from it, same as the rest of the Bukkit API.

The one exception is `getPlayerStatistics`: its `CompletableFuture` completes on a background database thread, so hop back with `Bukkit.getScheduler().runTask(...)` before touching Bukkit API in the callback.

## Methods

| Method | Description |
|---|---|
| `getArenas()` | Every loaded arena, including private/full ones. |
| `getAvailableArenas()` | Arenas joinable right now (public, not full, not mid-match). |
| `getArena(name)` | Snapshot for one arena, or `null` if not loaded. |
| `isArenaFull(name)` | `null` if the arena doesn't exist. |
| `getArenaOfPlayer(player)` | The arena a player is currently playing in, if any. |
| `sendPlayerToArena(player, arenaName)` | Attempts to join, same rules as `/join`. Returns an `ArenaJoinResult`. |
| `removePlayerFromArena(player)` | Same as `/leave` (eliminates them first if the match is running). `false` if they weren't in an arena. |
| `isPlayerCaged(player)` | `true` while the player is still in the glass spawn cage, before the match starts. |
| `getPlayerLocation(player)` | Wraps `player.location`. |
| `isSpectating(player)` | Whether the player is spectating any arena. |
| `getSpectatedArena(player)` | The arena the player is spectating, if any. |
| `getSpectators(arenaName)` | Snapshot list of an arena's spectators, or `null` if it doesn't exist. |
| `getPlayersInArena(arenaName)` | Snapshot list of players currently in an arena, or `null` if it doesn't exist. |
| `getAlivePlayers(arenaName)` | Snapshot list of players still alive, or `null` if the arena doesn't exist or isn't in-game. |
| `getMatchKills(player)` | Kill count in the player's current match, or `null` if they're not in one. |
| `getTimeRemainingSeconds(arenaName)` | Seconds left in the match, or `null` if not in-game. |
| `getCurrentEvent(arenaName)` | Id of the game event active in the arena's match, or `null` if none/not loaded. |
| `getCurrentEventOfPlayer(player)` | Id of the game event active in the player's match, or `null`. Cheap enough for hot paths (damage listeners). |
| `registerEvent(owner, event)` | Adds a [custom game event](#custom-game-events) to the vote and the pre-match selection. `false` if the id is taken. |
| `unregisterEvent(eventId)` | Removes a registered event (pending votes are dropped, an active handler is stopped). `false` if unknown. |
| `unregisterEvents(owner)` | Unregisters every event a plugin registered, returns how many were removed. |
| `getRegisteredEvents()` | Snapshot of every registered event definition, in registration order. |
| `getRegisteredEvent(eventId)` | One registered event definition, or `null`. |
| `getPlayerStatistics(playerName)` | Persisted stats from the database, or `null` if never recorded. |
| `getCachedPlayerStatistics(player)` | In-memory stats cache, no database round-trip. |

Methods that return a collection (`getSpectators`, `getPlayersInArena`, `getAlivePlayers`, `getArenas`, ...) always return a copy, never a live reference into arena state — mutating the returned list has no effect on the match.

### ArenaInfo

```kotlin
data class ArenaInfo(
    val name: String,
    val state: State, // WAITING, STARTING, INGAME, STOPPED
    val currentPlayers: Int,
    val maxPlayers: Int,
    val minPlayers: Int,
    val isPrivate: Boolean,
    val alivePlayers: Int?,     // set only while INGAME
    val secondsRemaining: Int?, // set only while INGAME
    val currentEvent: String?,  // set only while INGAME and an event was picked
) {
    val isFull: Boolean
    val isJoinable: Boolean
}
```

`currentEvent` is the id of the registered game event active in the match (e.g. `"lava"` with the bundled events addon), see [Custom game events](#custom-game-events).

### ArenaJoinResult

`SUCCESS`, `ARENA_NOT_FOUND`, `ALREADY_SPECTATING`, `ARENA_PRIVATE`, `ALREADY_IN_GAME`, `ARENA_NOT_READY`, `ARENA_FULL`.

### EliminationCause

`KILL`, `VOID`, `VOID_KILL`, `FALL`, `MOB`, `OTHER` — see `GourPillarsPlayerEliminatedEvent` below.

### PlayerStats

```kotlin
data class PlayerStats(
    val name: String,
    var kills: Int,
    var wins: Int,
    var xp: Int,
    var level: Int,
    var playedGame: Int,
    var bestWinStreak: Int,
    var currentWinStreak: Int,
)
```

## Events

Standard Bukkit events, fire-and-forget (not cancellable), all under `org.gourmet.gourPillars.api.events`.

| Event | Fields | Fired when |
|---|---|---|
| `GourPillarsPlayerJoinArenaEvent` | `arenaName`, `player` | A player joins an arena's waiting room. |
| `GourPillarsPlayerLeaveArenaEvent` | `arenaName`, `player` | A player leaves an arena (quit, `/leave`, kicked out). |
| `GourPillarsArenaStateChangeEvent` | `arenaName`, `oldState`, `newState` | An arena's state changes. Only fires on an actual transition. |
| `GourPillarsGameStartEvent` | `arenaName` | A match starts. |
| `GourPillarsGameEndEvent` | `arenaName`, `winner` (nullable) | A match ends. |
| `GourPillarsPlayerFinishEvent` | `arenaName`, `player`, `kills`, `won` | A player's match is over, win or lose. Fired once per player. |
| `GourPillarsPlayerKillEvent` | `arenaName`, `killer`, `victim` | A player eliminates another. |
| `GourPillarsPlayerEliminatedEvent` | `arenaName`, `player`, `cause` (`EliminationCause`), `source` (nullable `Entity`) | A player is eliminated, for any reason. Fired once per elimination, before `GourPillarsPlayerFinishEvent`. `source` is the killer/damager when there is one. |
| `GourPillarsEventSelectedEvent` | `arenaName`, `eventId` (nullable `String`) | The vote closes and the match's event is picked; `null` means no event. |
| `GourPillarsSpectateStartEvent` | `arenaName`, `player` | A player starts spectating. |
| `GourPillarsSpectateStopEvent` | `arenaName`, `player` | A player stops spectating. |

```kotlin
class MyListener : Listener {
    @EventHandler
    fun onKill(event: GourPillarsPlayerKillEvent) {
        Bukkit.broadcast(Component.text("${event.killer.name} eliminated ${event.victim.name}!"))
    }
}
```

## Custom game events

Match events (like the lava, knockback and border events) are not hardcoded in GourPillars: they're provided by other plugins through the API. One plugin can register any number of events; each registered event automatically:

- shows up in the pre-match vote GUI (with the icon/lore you define),
- takes part in the weighted selection and the slot-machine animation,
- gets a fresh handler per match (multiple arenas can run the same event at once),
- is unregistered when your plugin is disabled (pending votes are dropped and, if the event is active in a running match, its handler is stopped with `onStop(null)`).

Everything lives under `org.gourmet.gourPillars.api.event`. All callbacks run on the main server thread, and `registerEvent`/`unregisterEvent` must be called from it too (register in `onEnable`).

```kotlin
class MeteorsEvent(private val plugin: JavaPlugin) : GameEventDefinition {
    override val id = "meteors" // unique, 1-32 chars of [a-z0-9_-]
    override val displayName = MiniMessage.miniMessage().deserialize("<red>Meteors")

    override val voteItem =
        VoteItemSpec(
            material = Material.FIRE_CHARGE,
            lore = listOf(Component.text("Meteors rain on the arena!")),
            preferredSlot = 14, // optional; falls back to the first free slot
        )

    // Called once per match where this event wins the vote.
    override fun createHandler(context: GameEventContext): GameEventHandler =
        object : GameEventHandler {
            private var task: BukkitTask? = null

            override fun onStart() {
                task =
                    object : BukkitRunnable() {
                        override fun run() {
                            if (!context.isMatchRunning) {
                                cancel()
                                return
                            }
                            val target = context.alivePlayers.randomOrNull() ?: return
                            val sky = target.location.add(0.0, 20.0, 0.0)
                            context.world.spawn(sky, Fireball::class.java)
                        }
                    }.runTaskTimer(plugin, 100L, 100L)
            }

            override fun onStop(winner: Player?) {
                task?.cancel()
                task = null
            }
        }
}

// in onEnable:
val api = Bukkit.getServicesManager().load(GourPillarsAPI::class.java) ?: return
api.registerEvent(this, MeteorsEvent(this))
```

`GameEventContext` is a read-only view of the arena the handler runs in: `arenaName`, `world`, `bounds` (the build region as block coordinates), `minHeight`/`maxHeight`, `spawnLocation`, `alivePlayers`/`playersInArena` (snapshots), `isMatchRunning`, and `broadcast(component)`.

Notes:

- `createHandler` is only called for matches where your event was picked. For a passive event that just reacts to Bukkit events, return `GameEventHandler.EMPTY` and check `getCurrentEvent(arena)`/`ArenaInfo.currentEvent` in your listener.
- `onStop` receives the winner (or `null`) and is guaranteed to run at most once, only after a successful `onStart`. Undo world-border/time/etc. changes there; arena *blocks* are reset by GourPillars after every match.
- A handler that throws in `createHandler`/`onStart` is detached and the match continues without the event, so one broken addon can't kill a match.
- `registerEvent` returns `false` when another plugin already took the id, and throws `IllegalArgumentException` for malformed or reserved ids (`no-event`, `day-vote`, `night-vote`).
- `displayName` and `voteItem` are read once, at registration. Changing them afterwards has no effect; unregister and re-register to update an event's look.

A complete working addon providing five events (lava, knockback, border, meteors, low gravity), each tunable from its own `config.yml`, lives in [`examples/gourpillars-events-addon`](../examples/gourpillars-events-addon). Build it with `./gradlew :examples:gourpillars-events-addon:build` and drop the `-all.jar` next to GourPillars.
