# Commands & Permissions

## Main commands

| Command                  | Permission          | Description                          |
|---------------------------|----------------------|---------------------------------------|
| `/join <arena>`            | -                    | Join the specified arena              |
| `/joinrandom`               | -                    | Join the best available arena         |
| `/leave`                   | -                    | Leave the current arena               |
| `/stats`                   | -                    | Show your own player statistics       |
| `/stats <target>`          | `gpillars.stats.other` | Show another player's statistics    |
| `/party`, `/p`              | see below            | Party management (see `/party help`)  |
| `/edit`                    | `gpillars.admin`     | Arena editing session                 |
| `/build`                   | `gpillars.build`     | Toggle a build session in the lobby   |
| `/setspawn`                 | `gpillars.admin`     | Set the lobby spawn to your location  |

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

Requires `gpillars.admin`. All commands below are `/edit <subcommand>`, used to build an arena — see [arenas.md](arenas.md) for the full walkthrough.

`start`, `save`, `stop`, `name <name>`, `minplayers <min>`, `setMaxHeight`, `setMinHeight`, `setFallingTime <number>`, `setDeathSpawn`, `setRegionOne`, `setRegionTwo`, `spawn <number>`, `check`.
