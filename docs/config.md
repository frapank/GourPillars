# Configuration (`config.yml`)

On startup, `config.yml` is compared against the bundled default: any option missing (e.g. after updating to a version that added new settings) is added back with its default value, logging a warning naming the option. Existing invalid entries (bad slot, unknown material, etc.) are never edited — they're logged as a warning and skipped in favor of a safe fallback at runtime.

## Scoreboards

`scoreboards` defines the title and line layout for the three scoreboards (`lobby`, `waiting`, `in-game`), using MiniMessage formatting and the [placeholders](placeholders.md).

## Lobby spawn

`spawn` sets the world/coordinates/yaw/pitch players are teleported to when they're in the lobby. Also settable in-game with `/setspawn`.

## Lobby checks (`lobby`)

`lobby.enabled` is the master switch for GourPillars' own lobby handling — set it to `false` if another plugin (e.g. a dedicated hub plugin) manages the lobby world instead; arenas/matches are unaffected either way.

Each `lobby.checks` entry only runs while `lobby.enabled` is `true`, so individual pieces can also be handed over on their own:

| Key                          | Description                                                                                   |
|-------------------------------|-----------------------------------------------------------------------------------------------|
| `teleport-to-spawn-on-join`    | Teleport players to the configured spawn on join.                                              |
| `reset-state-on-join`          | Reset health/hunger/fire/potion effects on join.                                               |
| `lobby-items`                  | Give the `lobby-items` on join and handle clicking them.                                       |
| `scoreboard`                   | Show the lobby scoreboard.                                                                     |
| `unlimited-food`               | Keep hunger at full while in the lobby world.                                                  |
| `world-protection`             | Block building/breaking/signs/interactions in the lobby world (build sessions are exempt).     |
| `damage-protection`            | Cancel all damage, including PvP, in the lobby world.                                          |
| `item-protection`              | Block dropping items, inventory clicks and hand swapping in the lobby world.                   |
| `void-teleport-to-spawn`       | Teleport a player back to spawn if they fall into the void outside an active match.            |

## Leveling (`level`)

`level.enabled` is the master switch for the whole leveling system — when `false`, no XP is granted, no level-ups happen, the XP bar isn't touched, and vanilla XP orbs behave normally again.

| Key                             | Description                                                                                   |
|-----------------------------------|-----------------------------------------------------------------------------------------------|
| `xp-per-level`                     | XP needed to go from one level to the next.                                                   |
| `xp-rewards.kill`                  | XP granted for eliminating another player.                                                    |
| `xp-rewards.void-kill`             | XP granted for knocking another player into the void.                                         |
| `xp-rewards.win`                   | XP granted for winning a match.                                                                |
| `xp-rewards.game-played`           | XP granted just for playing a match, win or lose.                                              |
| `level-up.sound` / `-volume` / `-pitch` | Sound played to a player when they level up.                                             |

Set any `xp-rewards` entry to `0` to turn off that specific source without touching the others. The title/subtitle shown on level-up come from `language.yml` (`level.title` / `level.subtitle`), with `{level}` as a placeholder.

## Party

`party.fallback-max-size` is only used when no arena is loaded at all — normally a party's max size follows the biggest loaded arena instead. See [commands.md](commands.md) for the full party permission list.

## Match tuning (`game`)

| Key                                   | Description                                                              |
|-----------------------------------------|-----------------------------------------------------------------------------|
| `match-duration-seconds`                 | Match length.                                                              |
| `countdown-seconds`                       | Pre-match countdown.                                                       |
| `random-item-interval-seconds`            | How often each alive player receives a random item.                       |
| `excluded-random-items`                   | Material names never handed out by the random item task (creative-only/dev blocks, unobtainable-in-survival blocks, etc.). |
| `knockback-multiplier`                    | Velocity multiplier applied to the victim when the Knockback event is active. |
| `lava-rise-interval-seconds`              | How often the lava rises when the Lava event is active.                    |
| `border.final-size`                       | Border size (in blocks) at which the shrink stops.                        |
| `border.shrink-interval-seconds`          | How often the border shrinks when the Border event is active.             |
| `border.damage-amount`                    | Damage dealt per second to players outside the border.                    |

### Event voting (`game.events`)

- `lava.enabled` / `knockback.enabled` / `border.enabled` — disabling an event removes it from voting entirely, even with votes.
- `base-weight` — weight every option (including "no event") starts with before votes are counted, so an option can still be picked with zero votes. Set to `0` to require at least one vote.
- `vote-weight` — extra weight added per vote received.

After voting closes, one option is picked at random using these weights — votes make an outcome more likely, never guaranteed.

### Event-selection animation (`game.event-selection-animation`)

Slot-machine style animation played once the vote closes, right before the match starts.

| Key                             | Description                                                                 |
|-----------------------------------|---------------------------------------------------------------------------------|
| `enabled`                          | Turn the animation on/off entirely.                                            |
| `scroll-count`                     | How many names scroll by before landing on the picked event.                   |
| `scroll-interval-start-ticks` / `scroll-interval-end-ticks` | Delay between scrolled names; gradually slows from start to end for a "landing" effect. |
| `scroll-sound`, `scroll-sound-volume`, `scroll-sound-pitch` | Sound played for each scrolled name. |
| `reveal-sound`, `reveal-sound-volume`, `reveal-sound-pitch` | Sound played once when the picked event is revealed. |
| `reveal-hold-ticks`                | How long the reveal stays on screen before the match actually starts.          |

The animation reuses the item names configured under `gui.vote.items` below, so renaming an event there also renames it in the animation.

## Lobby items (`lobby-items`)

Items given to players standing in the lobby (the mode selector, cosmetics, casual match by default). Each entry is a freely-named key with:

- `slot` — hotbar/inventory slot (0-35).
- `material` — a valid [Bukkit `Material`](https://jd.papermc.io/paper/1.21.11/org/bukkit/Material.html) name, e.g. `COMPASS`.
- `name` — MiniMessage-formatted display name.
- `lore` — list of MiniMessage-formatted lore lines (optional).
- `command` — command run (without the leading `/`) when the item is right-clicked; leave empty (`""`) for a purely decorative item.

Add, remove or reorder entries freely — invalid ones (bad slot/material) are skipped with a warning instead of blocking startup.

## Waiting room items (`waiting-items`)

Only configures `slot`/`material` for the vote and leave items given in an arena's waiting room, since what they do is fixed; their name/lore stay in `language.yml` under `items.waiting`.

## Vote GUI (`gui.vote`)

The GUI opened by the "vote" waiting item, used to vote for an event and for day/night.

- `title` — inventory title.
- `size` — inventory size, must be a multiple of 9 between 9 and 54.
- `filler.material` / `filler.name` — the item used to pad empty slots.
- `items` — one entry per vote option: `no-event`, `lava-event`, `knockback-event`, `border-event`, `day-vote`, `night-vote`. Each key is a fixed internal id used to decide what the item does when clicked, so don't rename them — but `slot`, `material`, `name` and `lore` are all free to change, and removing an entry hides it. `lava-event`/`knockback-event`/`border-event` also auto-hide when their event is disabled under `game.events`.
  - `material` accepts any Bukkit material, or `PLAYER_HEAD` with a `head-texture` (base64 skin value, e.g. from [minecraft-heads.com](https://minecraft-heads.com)) for a custom head icon.

## Other files

- `arenas/<name>.yml` — one file per arena, see [arenas.md](arenas.md).
- `language.yml` — all in-game text, using MiniMessage formatting.
- `database.yml` — see [database.md](database.md).
