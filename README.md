# Law Of Milk

A Hytale server plugin. Work in progress.

> Built against Hytale Early Access **0.7.5** (`ServerVersion >=0.7.5 <0.8.0`).

## Development

Requirements: **Java 25** (JetBrains Runtime recommended). Gradle comes with the wrapper.

```bash
./gradlew setupHytaleDev   # download the server + assets (needs a Hytale account login)
./gradlew build            # compile, run unit tests, build build/libs/lawofmilk-<version>.jar
./gradlew runServer        # local dev server on port 5520; join it from the game
```

Project metadata (IDs, version, author, server version range) lives in `gradle.properties`; the
manifest is generated from it (`./gradlew updatePluginManifest`). Project notes and design decisions
are in [`CLAUDE.md`](CLAUDE.md).

Changes per version are in [`CHANGELOG.md`](CHANGELOG.md).

## Credits

Built on the [HytaleModding plugin template](https://github.com/HytaleModding/plugin-template) and
AzureDoom's [Hytale Gradle Plugin](https://github.com/AzureDoom/Hytale-Gradle-Plugin).

## License

MIT (as declared in `gradle.properties`).
