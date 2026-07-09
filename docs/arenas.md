# Setting Up an Arena

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

## `arenas/<name>.yml`

One file per arena (world, height/player limits, main spawn, region, in-game spawns), created by `/edit save` or dropped in manually. Requires a server restart to be picked up.

`spawn-height` (default `0`) raises the glass-cage spawn point by that many blocks above the configured spawn location; `spawn-height: 2` spawns players 2 blocks higher than the pillar itself.

A corrupted or incomplete arena file is skipped with a warning in console instead of blocking server startup. Any of `private-arena`, `min-height`, `min-players`, `slow-falling-time` or `spawn-height` missing from a file gets filled in with its default value (and a warning in console) the next time the arena loads.

Arenas previously stored under `config.yml`'s `Arenas` section are migrated here automatically on first startup after updating.
