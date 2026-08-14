# Commands & Permissions

## Main commands

| Command                  | Permission          | Description                          |
|---------------------------|----------------------|---------------------------------------|
| `/join <arena>`            | -                    | Join the specified arena              |
| `/joinrandom`               | -                    | Join the best available arena         |
| `/leave`                   | -                    | Leave the current arena               |
| `/stats`                   | -                    | Show your own player statistics       |
| `/stats <target>`          | `gpillars.stats.other` | Show another player's statistics    |
| `/spec <player\|arena>`    | `gpillars.spectate` | Spectate an in-game player or arena (see [features.md](features.md)) |
| `/party`, `/p`              | see below            | Party management (see `/party help`)  |
| `/edit`                    | `gpillars.admin`     | Arena editing session                 |
| `/build`                   | `gpillars.build`     | Toggle a build session in the lobby   |
| `/setspawn`                 | `gpillars.admin`     | Set the lobby spawn to your location  |

`/leave` also exits spectating, in addition to leaving an arena.

## Party commands & permissions

Every party action is gated behind its own permission node, so it can be restricted independently through a permissions plugin. `create`/`invite`/`accept`/`remove`/`leave`/`disband`/`promote`/`info`/`join` default to **everyone**; `public` and `broadcast` default to **operators only**.

| Subcommand                    | Permission                 | Description                                                                   |
|--------------------------------|------------------------------|---------------------------------------------------------------------------------|
| `/party create [--public]`     | `gpillars.party.create`      | Create a party. Private by default; `--public` requires `gpillars.party.public` |
| `/party invite <target>`       | `gpillars.party.invite`      | Invite a player (party leader only)                                            |
| `/party accept`                | `gpillars.party.accept`      | Accept a pending invite                                                        |
| `/party remove <target>`       | `gpillars.party.remove`      | Kick a member (party leader only)                                              |
| `/party leave`                 | `gpillars.party.leave`       | Leave your party                                                               |
| `/party disband`               | `gpillars.party.disband`     | Disband the party (party leader only)                                          |
| `/party promote <target>`      | `gpillars.party.promote`     | Transfer party leadership (party leader only)                                  |
| `/party info`, `/party list`   | `gpillars.party.info`        | Show party members                                                             |
| `/party join <target>`         | `gpillars.party.join`        | Join a **public** party by naming any of its members                          |
| `/party public`                | `gpillars.party.public`      | Make your party public (party leader only)                                     |
| `/party private`               | `gpillars.party.public`      | Make your party private again (party leader only)                              |
| `/party broadcast`             | `gpillars.party.broadcast`   | Send a clickable server-wide invite for your (public) party, reaching every online player, including those in a match |

A party's max size is not fixed: it always equals the largest max-player count among every loaded arena (in any state, including ones currently in-game), so a party can never grow too big to fit in any arena. If no arena is loaded at all, `party.fallback-max-size` in `config.yml` is used instead. Trying to `/party join`, or an admin `/party invite`-ing, past that size is rejected.

When a party leader runs `/join <arena>` or `/joinrandom`, the whole party is only let in if the arena has enough free slots for every member; otherwise nothing happens and the leader is told how many slots are available.

## Edit commands

Requires `gpillars.admin`. All commands below are `/edit <subcommand>`, used to build an arena — see [arenas.md](arenas.md) for the full walkthrough. `/edit` alone (or `/edit help`) lists them in game, and an action bar shows the state of the session while you build.

Each subcommand says what it does; the short form in the last column is the same command, kept so older habits and guides keep working. Names are case-insensitive (`setspawn` works as well as `setSpawn`).

| Subcommand | Description | Short form |
|---|---|---|
| `startEditing` | Start a new arena (switches you to creative) | `start` |
| `loadArena <name>` | Open an existing arena for editing, values included | `load` |
| `setName <name>` | Set the arena name (and its file name) | `name` |
| `setSpawn <number>` | Add/move a player spawn where you stand | `spawn` |
| `removeSpawn <number>` | Remove a spawn | `delSpawn` |
| `listSpawns` | List every spawn with its coordinates | `spawns` |
| `teleportToArena` | Teleport to the arena you are editing | `tp` |
| `teleportToSpawn <number>` | Teleport to one of its spawns | `tp <number>` |
| `setRegionOne`, `setRegionTwo` | The two opposite corners: they define the height limits too | `pos1`, `pos2` |
| `setDeathSpawn` | Where eliminated players and spectators are sent | — |
| `setMinPlayers <min>` | Players needed to start the countdown | `minplayers` |
| `setSlowFalling <seconds>` | Slow falling given at the start of the match | `setFallingTime` |
| `setSpawnHeight <blocks>` | Raise the glass cages above the spawns | — |
| `setPrivate <true\|false>` | Hide the arena from joining players | `private` |
| `showStatus` | Show what is set, what is defaulted and what is missing | `check` |
| `toggleMarkers` | Turn the particle markers on and off | `view` |
| `toggleAlignment` | Turn the block-centring / view-alignment assist on and off | `snap` |
| `saveArena` | Write the file and load the arena, no restart needed | `save` |
| `stopEditing` | Close the session without saving, restoring your gamemode and position | `stop` |
