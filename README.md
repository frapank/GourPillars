# GourPillars

GourPillars is a Paper plugin implementing a "pillars" last-man-standing minigame: players fight on a set of collapsing pillars while random items drop periodically, until a single survivor remains.

## Requirements

- Paper (or a Paper fork) 1.21.11
- Java 21
- [Multiverse-Core](https://github.com/Multiverse/Multiverse-Core) (required, used for per-arena world handling)
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) (optional, enables the placeholders below)
- A reachable MySQL server (used for persistent player statistics)

## Features

- Multi-arena system with independently configurable regions, spawns, and player limits
- Dynamic pillar collapse mechanic
- Periodic random item/block distribution during a match
- Party system (create, invite, promote, disband, kick)
- Persistent player statistics (kills, wins, defeats, XP, level, win streaks) backed by MySQL through HikariCP
- Lobby, waiting-room and in-game scoreboards
- MiniMessage-based, fully configurable messages (`language.yml`)
- PlaceholderAPI expansion for arena and player statistics

## Commands & Permissions

| Command                  | Permission        | Description                          |
|---------------------------|-------------------|---------------------------------------|
| `/join <arena>`            | -                 | Join the specified arena              |
| `/leave`                   | -                 | Leave the current arena               |
| `/stats`                   | -                 | Show your player statistics           |
| `/party`, `/p`              | -                 | Party management (see `/party help`)  |
| `/edit`                    | `gpillars.admin`  | Arena editing session                 |
| `/build`                   | `gpillars.build`  | Toggle a build session in the lobby   |
| `/setspawn`                 | `gpillars.admin`  | Set the lobby spawn to your location  |

`/party` subcommands: `create`, `invite <target>`, `accept`, `remove <target>`, `leave`, `disband`, `promote <target>`, `info`/`list`.

`/edit` subcommands: `start`, `save`, `stop`, `name <name>`, `minplayers <min>`, `setMaxHeight`, `setMinHeight`, `setFallingTime <number>`, `setDeathSpawn`, `setRegionOne`, `setRegionTwo`, `spawn <number>`, `check`.

## Setting Up an Arena

Requires `gpillars.admin`. All commands below are `/edit <subcommand>`.

1. `start` — begins an editing session for you. Only one arena can be edited at a time per player.
2. `name <name>` — sets the arena's identifier (also its file name, `arenas/<name>.yml`). Required.
3. `minplayers <min>` — minimum players needed for the match to start. Required.
4. `setDeathSpawn` — stand where players should be sent back to when the match ends, then run this. Required (loaded as the arena's main spawn).
5. `setRegionOne` and `setRegionTwo` — stand at two opposite corners of the arena and run each, to define its bounding region (used for the build limit and the border-shrink event). Required.
6. `setMinHeight` / `setMaxHeight` — stand at the lowest/highest point of the arena and run each. `setMinHeight` also sets the void-kill and lava-rise level; `setMaxHeight` is currently stored but not enforced by any game logic.
7. `setFallingTime <number>` — slow falling potion duration (in ticks) applied to players on join. Optional, defaults to `1`.
8. `spawn <number>` — stand on a pillar and run this once per player slot (e.g. `spawn 1`, `spawn 2`, ...). The number of registered spawns becomes the arena's max player count. At least one is required, and all spawns must be in the same world.
9. `check` — prints the values collected so far, useful to verify before saving.
10. `save` — writes `arenas/<name>.yml` and disables daylight/weather cycles and advancement announcements in that world. Missing optional fields only print a warning; a missing name, spawn, or minplayers blocks the save. **The server must be restarted for the new/edited arena to be loaded and joinable.**
11. `stop` — ends the editing session without saving.

## Placeholders

Registered under the `pillars` identifier once PlaceholderAPI is installed.

```
Arena
%pillars_minplayers%      Minimum players required to start
%pillars_maxplayers%      Maximum players allowed in the arena
%pillars_waitingplayers%  Players currently waiting in the arena
%pillars_arenaname%       Name of the player's current arena
%pillars_aliveplayers%    Players still alive in the current match
%pillars_time%            Elapsed match time
%pillars_ingamekills%     Player's kills in the current match

Player
%pillars_kills%           Total kills
%pillars_wins%            Total wins
%pillars_defeats%         Total defeats (games played minus wins)
%pillars_xp%              Current XP
%pillars_level%           Current level

Global
%pillars_arenacount%      Number of active arenas
%pillars_playersinmatch%  Total players currently in a match
```

## Configuration

- `config.yml` — lobby spawn, scoreboard layouts and match tuning (match/countdown length, random item interval, knockback multiplier, lava/border event timings).
- `arenas/<name>.yml` — one file per arena (world, height/player limits, main spawn, region, in-game spawns), created by `/edit save` or dropped in manually. Requires a server restart to be picked up. Arenas previously stored under `config.yml`'s `Arenas` section are migrated here automatically on first startup after updating.
- `language.yml` — all in-game text, using MiniMessage formatting.
- `database.yml` — MySQL connection settings (host, port, database, credentials, pool size). Generated with safe defaults on first run; invalid values fall back to defaults and log a warning instead of preventing startup. If the database is unreachable, server operators (and any player with `gpillars.admin`) are warned in chat on join, and statistics are disabled for that session.

## Building from Source

```
./gradlew build
```

The shaded plugin jar is produced at `build/libs/GourPillars-<version>-all.jar`.
