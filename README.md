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

- `config.yml` — arenas, spawns, scoreboard layouts and match tuning (match/countdown length, random item interval, knockback multiplier, lava/border event timings).
- `language.yml` — all in-game text, using MiniMessage formatting.
- `database.yml` — MySQL connection settings (host, port, database, credentials, pool size). Generated with safe defaults on first run; invalid values fall back to defaults and log a warning instead of preventing startup. If the database is unreachable, server operators (and any player with `gpillars.admin`) are warned in chat on join, and statistics are disabled for that session.

## Building from Source

```
./gradlew build
```

The shaded plugin jar is produced at `build/libs/GourPillars-<version>-all.jar`.
