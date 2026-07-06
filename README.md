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
| `/edit`                    | `gpillars.admim`  | Arena editing session                 |
| `/build`                   | `gpillars.build`  | Toggle a build session in the lobby   |

`/party` subcommands: `create`, `invite <target>`, `accept`, `remove <target>`, `leave`, `disband`, `promote <target>`, `info`/`list`.

`/edit` subcommands: `start`, `save`, `stop`, `name <name>`, `minplayers <min>`, `setMaxHeight`, `setMinHeight`, `setFallingTime <number>`, `setDeathSpawn`, `setRegionOne`, `setRegionTwo`, `spawn <number>`, `check`.

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

Arenas, spawns and scoreboard layouts are defined in `config.yml`; all in-game text is defined in `language.yml` using MiniMessage formatting. Database connection settings are currently defined in `DatabaseManager.kt` and must be adjusted there before building.

## Building from Source

```
./gradlew build
```

The shaded plugin jar is produced at `build/libs/GourPillars-<version>-all.jar`.
