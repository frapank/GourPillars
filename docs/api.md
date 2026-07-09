# API for other plugins

GourPillars exposes a small Bukkit-style API so other plugins can read arena/match state and react to matches. Everything lives under `org.gourmet.gourPillars.api`.

### Gradle

```kotlin
dependencies {
    compileOnly(files("/path/to/GourPillars-1.1.jar"))
}
```

### Maven

Install the jar into your local repository once:

```
mvn install:install-file -Dfile=GourPillars-1.1.jar -DgroupId=org.gourmet -DartifactId=GourPillars -Dversion=1.1 -Dpackaging=jar
```

Then reference it:

```xml
<dependency>
    <groupId>org.gourmet</groupId>
    <artifactId>GourPillars</artifactId>
    <version>1.1</version>
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
| `getCurrentEvent(arenaName)` | Active game event, or `null` if none/not loaded. |
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
    val alivePlayers: Int?,        // set only while INGAME
    val secondsRemaining: Int?,    // set only while INGAME
    val currentEvent: GameEvents?, // set only while INGAME and an event was picked
) {
    val isFull: Boolean
    val isJoinable: Boolean
}
```

`GameEvents` is `LAVA`, `BORDER` or `KNOCKBACK`.

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
| `GourPillarsEventSelectedEvent` | `arenaName`, `event` (nullable `GameEvents`) | The vote closes and the match's event is picked; `null` means no event. |
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
