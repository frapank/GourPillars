# Features

## Arenas

Multi-arena system, each with its own region, spawns and player limit. Arenas live one-per-file under `arenas/<name>.yml`, built in-game with `/edit` — see [arenas.md](arenas.md). Pillars collapse dynamically during the match, and random items/blocks are handed out to alive players on an interval.

## Game events

Before each match, players vote for an event: Lava, Knockback, Border, or no event at all. Votes only make an option more likely, never guaranteed or excluded — after voting closes, one option is picked at random, weighted by its vote count. Each event can be disabled entirely from `config.yml`, in which case it disappears from voting. A slot-machine style animation plays once the vote closes, scrolling through the options before landing on the picked one.

See [config.md](config.md) for the full list of tunable values (weights, animation timing/sounds, event parameters).

## Party system

Create, invite, promote, disband and kick, with public/private parties and a server-wide broadcast invite. Every action has its own permission node. A party's max size automatically follows the biggest loaded arena, so it can never outgrow what's joinable. See [commands.md](commands.md) for the full list of subcommands and permissions.

## Player statistics

Kills, wins, defeats, XP, level and win streaks, persisted through an async database layer backed by either MySQL (via HikariCP) or SQLite. See [database.md](database.md).

## Scoreboards

Separate, independently configurable scoreboards for the lobby, an arena's waiting room, and an in-game match.

## Messages

All in-game text lives in `language.yml` using MiniMessage formatting, so colors/styling can be customized without touching code.

## PlaceholderAPI support

Optional expansion registered under the `pillars` identifier, exposing arena, player and global statistics. See [placeholders.md](placeholders.md).

## Config resilience

On startup, `config.yml` is compared against the plugin's bundled defaults: any missing option (e.g. after updating to a version that added new settings) is added back automatically with its default value, without touching existing customizations. Invalid entries (bad slot, unknown material, etc.) are never edited — they're logged as a warning and skipped in favor of a safe fallback at runtime, instead of blocking startup.
