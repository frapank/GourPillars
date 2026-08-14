# Setting Up an Arena

Requires `gpillars.admin`. All commands below are `/edit <subcommand>`; `/edit` on its own (or `/edit help`) prints the same list in game.

Every subcommand is named after what it does. The short forms listed by `/edit help` (`start`, `save`, `pos1`, `check`, ...) are aliases for the same commands, and names are case-insensitive.

While a session is open, an action bar keeps showing what is already set, what is still missing and whether you have unsaved changes, so you never have to guess where you are in the process. `/edit showStatus` prints the same status in full, with the exact command to run for anything that is missing.

## Walkthrough

1. `startEditing` — begins an editing session for you and puts you in creative. Refused if you are in a match or spectating; one session per player.
2. `setName <name>` — sets the arena's identifier (also its file name, `arenas/<name>.yml`). Letters, digits, `-` and `_`, up to 32 characters. Required.
3. `setSpawn <number>` — stand on a pillar and run this once per player slot (e.g. `setSpawn 1`, `setSpawn 2`, ...). The number of registered spawns becomes the arena's max player count. At least one is required. Running it again with the same number moves that spawn.
4. `setRegionOne` and `setRegionTwo` (aliases `pos1` / `pos2`) — stand at two opposite corners of the arena and run each. They define the build limit, the border-shrink event **and both height limits**: the lower corner is where players are eliminated, the upper one is the ceiling. Required.
5. `setDeathSpawn` — stand where eliminated players and spectators should be sent, then run this. Required (stored as the arena's `main-spawn`).
6. `showStatus` — prints everything collected so far, the values that will be filled in automatically, and what still blocks the save.
7. `saveArena` — writes `arenas/<name>.yml`, disables daylight/weather cycles and advancement announcements in that world, snapshots the world for the between-matches reset, and **loads the arena immediately: no restart needed** unless the previous version of that arena is still busy with a match. The session stays open afterwards, so you can keep tweaking and save again.
8. `stopEditing` — closes the session, gives you back the gamemode you had before it and teleports you where you were standing when it started (the lobby spawn, if that spot's world is gone by then). With unsaved changes it asks for a confirmation first (run it twice within 15 seconds).

Everything else is optional:

| Command | Description | Left unset |
|---|---|---|
| `setMinPlayers <min>` | players needed for the countdown to start | `2` |
| `setSlowFalling <seconds>` | slow falling applied when the match starts | `1` |
| `setSpawnHeight <blocks>` | raises the glass cages above the spawns | `0` |
| `setPrivate <true\|false>` | hides the arena: nobody can join it | `false` |

**The heights are the region corners, there is no command to set them**: `setRegionOne`/`setRegionTwo` (`pos1`/`pos2`) already say where the arena starts and ends vertically, so the lower corner is the void-kill level and the upper one is the ceiling. Move a corner and they move with it, in this session and after reopening the arena with `load`. The values that end up in the file are named back to you when you save.

A file edited by hand can still carry `min-height`/`max-height` of its own — GourPillars loads them as they are — but the editor only ever writes the region ones, and `load` warns you when the file's values differ, before you save over them.

## A full arena, start to finish

```
/edit startEditing
/edit setName skyfall
              (stand on the first pillar)    /edit setSpawn 1
              (stand on the second pillar)   /edit setSpawn 2
              (stand at one corner)          /edit setRegionOne
              (stand at the opposite corner) /edit setRegionTwo
              (stand where the dead go)      /edit setDeathSpawn
/edit showStatus
/edit saveArena
```

Eight commands, and the arena is joinable right away: the heights are the two corners, and min players, slow falling and spawn height take their defaults.

## Visual markers

While a session is open, coloured particles draw what you have set:

| Colour | What it marks |
|---|---|
| light blue | the four vertical edges of the region |
| red | the `min-height` outline — below it players are eliminated |
| amber | the `max-height` outline |
| green | each player spawn |
| purple | the death spawn |
| white | a region corner placed on its own, waiting for the second one |

The red and amber outlines sit exactly where the two height limits will be saved, so the red one shows you the line players die below before a single match is played.

`toggleMarkers` toggles them. They are sent to the editor only — no other player sees them, nothing is placed in the world and no entity is created, so a session that ends (or an admin who disconnects) leaves nothing to clean up. The drawing cost is capped: a 5000-block region uses the same number of particles as a 20-block one, just spaced further apart, and anything more than 48 blocks away isn't drawn at all.

## Alignment assist

The spot you are standing at is tidied up slightly before it is stored, for `spawn` and `setDeathSpawn`:

- the position is **centred on the block** you stand on, so players spawn in the middle of the pillar instead of on its edge;
- the height is rounded to the block when it is a hair off one;
- the view is **squared up to the nearest 45°**, horizontally *and* vertically — a look that is nearly due west becomes exactly west, a nearly level view becomes exactly level.

The angles are only touched when they are already within 12° of a 45° mark: aim at a deliberate odd angle, or stand at a deliberate half-block height, and it is kept exactly as you set it. Whatever gets nudged is named back to you in chat, and `toggleAlignment` turns the whole assist off for the session, storing every spot precisely where and how you stand.

## Editing an arena that already exists

`loadArena <name>` opens an existing `arenas/<name>.yml` in an editing session with all of its values (including the ones you never set by hand, like `spawn-height`) already filled in, and **teleports you to that arena** so you can see what you are editing. Change what you need and `save` — options you didn't touch are written back unchanged, and the arena is reloaded in place.

You are switched to creative for the session either way, and disconnecting mid-session gives your gamemode back too, so nobody comes back logged in as a creative admin.

The teleport falls back sensibly: it aims at the death spawn, then the first spawn, then a region corner, then the world spawn. If you are already in that world you are left exactly where you stand (`teleportToArena` jumps to the arena on demand), and if the arena's world isn't loaded at all nothing is opened — you get told which world it is and how to load it (`/mv load <world>` with Multiverse).

Without `loadArena`, `/edit startEditing` + `setName <existing>` overwrites that file from scratch on save; the editor warns you when the name you pick already belongs to a file.

## Other helpers

- `listSpawns` — lists every spawn with its coordinates, flagging the ones outside the region.
- `teleportToArena` — teleports you to the arena you are editing (its death spawn, or the first thing it has); `teleportToSpawn <number>` goes to a specific spawn.
- `removeSpawn <number>` — removes a spawn.

## World snapshot and reset

Saving takes a zip snapshot of the arena's world under `backups/<world>-backup.zip`; after every match the world is unloaded, wiped and unpacked from that snapshot, so each game starts from the same map.

Files the server keeps locked (`session.lock`) are left out of the snapshot instead of aborting it — on Windows they cannot be read at all while the world is loaded, which is why a `/edit saveArena` there used to end without a usable backup while the same thing worked on Linux. Any other file that can't be read is named in the console and skipped, the new zip only replaces the previous one once it is written in full, and the folder deletion done before restoring is retried a few times, since Windows releases the handles of a just-unloaded world lazily.

## Rules the editor enforces

- An arena lives in exactly **one** world: any location set in a different world than the first one is refused, instead of being silently rewritten into the arena's world on load.
- Arenas can't be built in the lobby world.
- `min-players` can't exceed the number of spawns (the match could never start), and `min-height` has to stay below `max-height`.
- While anybody is editing, `/join` and `/joinrandom` are disabled server-wide; disconnecting closes the session and lifts the block.

## `arenas/<name>.yml`

One file per arena (world, height/player limits, main spawn, region, in-game spawns), created by `/edit saveArena` or dropped in manually. Files written by hand are picked up on the next server start, or with `/edit loadArena <name>` + `/edit saveArena`.

`spawn-height` (default `0`) raises the glass-cage spawn point by that many blocks above the configured spawn location; `spawn-height: 2` spawns players 2 blocks higher than the pillar itself.

A corrupted or incomplete arena file is skipped with a warning in console instead of blocking server startup. Any of `private-arena`, `min-players`, `slow-falling-time` or `spawn-height` missing from a file gets filled in with its default value (and a warning in console) the next time the arena loads. `min-height` and `max-height` are not written blindly: when missing they fall back to the world's own build limits, since a hardcoded `0` would kill every player in a world built below `Y=0`.

Arenas previously stored under `config.yml`'s `Arenas` section are migrated here automatically on first startup after updating.
