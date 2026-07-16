# Contributing to GourPillars

Thanks for taking the time to contribute! This document covers everything you need to set up the project, follow its conventions, and get a change merged.

## Requirements

- JDK 21
- Git
- A Paper 1.21.11 server for manual testing (see [Running the plugin](#running-the-plugin))

You don't need to install Gradle — the repo ships the Gradle Wrapper (`gradlew` / `gradlew.bat`), which downloads the correct version automatically.

## Getting started

```bash
git clone https://github.com/frapank/GourPillars.git
cd GourPillars
./gradlew build
```

The shaded plugin jar is produced at `build/libs/GourPillars-<version>-all.jar`. The example plugins under `examples/` (the API smoke-test plugin and the events addon) are built as part of the same `./gradlew build`, each producing its own `-all.jar` under `examples/<name>/build/libs/`.

## Making changes

1. Fork the repo and create a branch off `main`:
   ```bash
   git checkout -b feat/short-description
   ```
   Use a prefix that matches the change: `feat/`, `fix/`, `refactor/`, `docs/`.
2. Keep changes focused. Unrelated fixes/refactors should be a separate PR.
3. If you touch `config.yml`, `database.yml`, or `language.yml`, make sure new keys have sane defaults and update the matching doc page — see the config resilience behavior described in [docs/features.md](docs/features.md#config-resilience).
4. If you add a command or permission, register it in `build.gradle.kts` (`bukkit { permissions { ... } }`) and document it in [docs/commands.md](docs/commands.md).

## Code style & formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with `ktlint` to enforce Kotlin style. **Run it before committing** — CI will fail the build otherwise:

```bash
./gradlew spotlessApply   # auto-formats Kotlin sources and *.gradle.kts
./gradlew spotlessCheck   # verifies formatting without modifying files
```

`spotlessApply` covers:
- `src/**/*.kt` — ktlint, trailing whitespace trimmed, file ends with a newline
- `*.gradle.kts` — ktlint

Since `build` depends on `shadowJar` but **not** on `spotlessCheck`, a clean `./gradlew build` won't catch formatting issues by itself — run `spotlessCheck` (or `spotlessApply`) explicitly, or run:

```bash
./gradlew spotlessCheck build
```

## Building & running

```bash
./gradlew build          # compile, shade, produce the plugin jar
./gradlew runServer      # launch a local Paper 1.21.11 test server with the plugin installed
```

`runServer` downloads a Paper server into `run/` on first use. `Multiverse-Core` is a hard dependency at runtime — grab it separately and drop it in `run/plugins/` before testing arenas that involve per-arena worlds. `PlaceholderAPI` is optional but needed to test the placeholder expansion.

When testing a gameplay change manually, actually play through it in-game (join an arena, trigger the event/feature) rather than relying on compilation success alone.

## Commit messages

Keep commits scoped and the message focused on *why*, not just *what*. for the existing style (e.g. `refactor: rename in-game titles to pillars of fortune`, `add: logo`) and follow it — a short `type: summary` line is preferred over long prose.

## Submitting a pull request

1. Make sure `./gradlew spotlessCheck build` passes locally.
2. Update relevant docs under `docs/` (and `README.md` if you touched the feature list or requirements).
3. Open a PR against `main` with a clear description of the change and, for gameplay changes, how you tested it.
4. Be responsive to review feedback — small, iterative fixups are easier to review than a single large rewrite.

## License

By contributing, you agree that your contributions will be licensed under the project's [GPLv3 license](LICENSE).
