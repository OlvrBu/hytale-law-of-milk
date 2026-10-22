# CLAUDE.md — Law Of Milk (Hytale server plugin)

## What this project is

A Hytale (Early Access, 0.7.x) server plugin + asset pack. Name: "Law Of Milk" (mod ID `lawofmilk`, package `io.github.obut.lawofmilk`, main class `LawOfMilkPlugin`).

Gameplay design: not decided yet (see [Design](#design)).

Created on 2026-10-22 from the HytaleModding plugin template (https://github.com/HytaleModding/plugin-template, commit `b7d12fd`). The template's example command, config and event classes are still in place as references and can be replaced. The owner's first mod, Transmutation Chest (`../transmutation_chest`), is a finished, working example of a block component, custom UI, commands, config files and unit tests on this server version. Look there first for patterns that are already verified.

## Working with the developer

- Background and preferences of the developer are in the workspace `../CLAUDE.md` (not part of this repo); follow them.
- You cannot run the game client. Anything that needs in-game verification: tell the developer exactly what to do in-game and what log output / behaviour to report back. Don't mark a milestone done until they confirm.

## Toolchain & commands

- Java 25 (JetBrains Runtime recommended). Gradle wrapper — never require a system Gradle.
- AzureDoom's `com.azuredoom.hytale-tools` Gradle plugin. Project metadata lives in `gradle.properties`; the manifest is generated from it.

```bash
./gradlew setupHytaleDev                       # fetch/sync server jar + assets for dev
./gradlew build                                # compile, test, package -> build/libs/
./gradlew runServer                            # local dev server; connect from game to localhost:5520
./gradlew runServer -Ddebug=true -Dhotswap=true
./gradlew updatePluginManifest                 # after changing manifest-related properties
./gradlew build --refresh-dependencies         # if resolution fails
./gradlew hytaleJvmDoctor                      # JVM / hot-swap diagnostics
```

- Decompiled server sources (after `setupHytaleDev`): `build/vineflower/hytale-server/`.
- Dev server login: the first `runServer` of this project needs `/auth login device` in the server console; the login is then stored encrypted in `run/`.
- Singleplayer mods folder: `UserData/Mods` in the Hytale data folder

Always run `./gradlew build` after changes and fix all compile errors and test failures before reporting back.

## API accuracy rules (important)

The Hytale API is young, changes between Early Access updates, and is not well represented in your training data. Community docs (hytalemodding.dev) are good but can lag behind the current server version.

- **Never invent class names, method signatures, event names, or JSON keys.** Verify against the actual server JAR resolved by Gradle (decompiled sources, `javap`), the game assets, or an up-to-date guide.
- If you can't verify something, say so explicitly and propose how to check it, rather than guessing.
- When a community guide and the server JAR disagree, the JAR wins.
- Useful references: https://hytalemodding.dev/en/docs, https://github.com/AzureDoom/Hytale-Gradle-Plugin

## Design

Not decided yet. Record decisions here as the owner makes them.

## Code conventions

- One responsibility per class.
- Log through the plugin logger with a clear prefix; no `System.out`.
- Keep game-independent logic in plain Java classes and unit-test it without a server.
- Small, reviewable changes. Explain non-obvious Hytale-specific code with a short comment.
- Don't add third-party dependencies without asking.
