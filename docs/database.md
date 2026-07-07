# Database (`database.yml`)

Player statistics (kills, wins, defeats, XP, level, win streaks) are persisted through a single async database interface, so queries never block the main thread. Two backends are available, picked with `storage-type`:

- `mysql` (default, and the fallback if `storage-type` is missing or unrecognized)
- `sqlite`

`database.yml` is generated with safe defaults on first run.

## MySQL settings

Used when `storage-type: mysql`.

| Key                          | Description                                              |
|--------------------------------|--------------------------------------------------------------|
| `host`, `port`, `database`       | Connection target.                                          |
| `username`, `password`           | Credentials.                                                |
| `use-ssl`                        | Whether to connect over SSL.                                |
| `allow-public-key-retrieval`      | MySQL driver option, needed by some auth plugins/setups.    |
| `pool-size`                      | Maximum number of pooled JDBC connections (1-50), managed by HikariCP. |

## SQLite settings

Used when `storage-type: sqlite`.

| Key             | Description                                          |
|-------------------|-----------------------------------------------------------|
| `sqlite.file`      | File name for the local database, stored inside the plugin's data folder. |

## Resilience

- Invalid values anywhere in `database.yml` don't block startup: a safe default is used instead, with a warning logged to console.
- If the configured database is unreachable, the plugin doesn't crash: statistics are disabled for that session, and server operators (plus any player with `gpillars.admin`) are warned in chat on join.
