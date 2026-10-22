# CLAUDE.md — Law Of Milk (Hytale server plugin)

## What this project is

A Hytale (Early Access, 0.7.x) server plugin + asset pack. Name: "Law Of Milk" (mod ID `lawofmilk`, package `io.github.obut.lawofmilk`, main class `LawOfMilkPlugin`).

The mod is a collection of small, independent fun ideas, of very different implementation depth. It's both for fun and for exploring and pushing what the modding API can do. Each idea lives in its own subpackage of `io.github.obut.lawofmilk` with a `register(...)` method called from `LawOfMilkPlugin.setup()`, and its own section under [Features](#features).

Created on 2026-10-22 from the HytaleModding plugin template (https://github.com/HytaleModding/plugin-template, commit `b7d12fd`). The template's example command, config and event classes were removed on 2026-10-22 (see the first commit for them). Note: the dev server uses `src/main/resources` as the plugin's data folder, so a plugin config saved with `Config.save()` lands there, along with a `.bak` of the previous version. The owner's first mod, Transmutation Chest (`../transmutation_chest`), is a finished, working example of a block component, custom UI, commands, config files and unit tests on this server version. Look there first for patterns that are already verified.

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

## Features

Record design decisions per feature as the owner makes them; don't change decided behavior without asking.

### Suspicious plushies (`gnomeblock`): done, confirmed in game 2026-10-22

- **Items:** `Deco_Kweebec_Plush_Suspicious` and `Deco_Goblin_Plush_Suspicious` ("Suspicious … Plushie"). They inherit the vanilla plushies via `"Parent"`, so they look the same.
- **Recipes:** both are crafted at the Builder's Workbench (`StructuralCrafting` / `Builders`, category `Decorative`). Each comes from 1 of its vanilla plushie; this is one-way. The goblin one can also be crafted from 1 Kweebec Plushie, via a standalone recipe file in `Server/Item/Recipes/LawOfMilk/`, since an item JSON holds only one `Recipe`. No natural world generation.
- **Random screams:** they play the gnome scream (`SFX_LawOfMilk_Gnome_Scream`, `Common/Sounds/LawOfMilk/Gnome_Scream.ogg`, converted from an mp3 the owner supplied) at a random point within 6 blocks.
  - **Mechanism:** a custom random tick procedure, `LawOfMilk_RandomSound`, with `Chance` 0.05.
  - **Frequency:** every loaded block gets a random tick about every 8192 server ticks (30 TPS), so this is about one scream per plushie every 1.5 hours. Chance 1.0 (about every 4.6 min) is the fastest this mechanism allows.
- **On use:** using a plushie plays the scream the same way, through the custom interaction `LawOfMilk_PlaySoundAround`. This replaces the vanilla squeak and makes testing easy.
- **Rejected:** an echo variant of the sound. The owner didn't like it, so it was removed.

### Super apple (`superapple`): working, confirmed in game 2026-10-22

- **Activation:** hitting blocks with a vanilla `Plant_Fruit_Apple` in hand, while hotbar + storage + backpack hold **exactly** 3 `Ingredient_Crystal_Void` and 1 `Ingredient_Crystal_White`. No vanilla item is changed; eating the apple (secondary) works as normal.
  - **History:** it started as an Iron Hatchet trigger. The owner preferred the apple, and the hatchet was removed on 2026-10-22.
- **Any material:** the apple isn't a tool, so hits use the server's bare-hand gather specs (`Server/Item/Unarmed/Gathering`). `DamageBlockEvent` raises each hit to at least 0.3 damage (every block has 1.0 health), so anything breaks in 4 hits. The client still predicts bare-hand damage, so its crack animation lags behind.
- **Area:** on `BreakBlockEvent`, every block of the same type (same block id, filler cells skipped) within a sphere of radius `SuperApple.radius` is also removed; the blocks don't have to be connected. It runs as a world task after the original break.
  - **Mechanism (decided 2026-10-22):** the area blocks are set straight to air with `BlockOperations.setBlock` (full overload, chunk section components cached per 32³ section, `NO_SEND_PARTICLES`). They are not broken like mined blocks: no `BreakBlockEvent` (other plugins can't veto it), no drops, sounds, physics or neighbor updates, no on-break interactions. Heightmap, lighting, block entities and filler cells are still updated. The owner dropped the area-drops option for this.
  - **Defaults:** radius 5. The hit block itself breaks and drops normally.
  - **Dev command:** `/lawofmilk appleradius <0-32>` sets the radius at runtime. It's operator-only and not saved.
  - **Timing:** every area removal logs its block count and time.
- **Measured (2026-10-22, radius 32):**
  - with `setBlock`: 74k stone blocks took 143 ms, 61k took 78 ms, 12.8k basalt took 25 ms;
  - before, with per-block `performBlockBreak` (break events, sounds, particles): about 70k stone took 300–400 ms.

  With the old version, the client showed the breaks in shells moving outward from the player, because it applies chunk section updates nearest-first.
- **Known limits:** the server only fires `DamageBlockEvent` if the bare-hand specs cover the block's gather type with enough quality. They have no quality, so these stay unminable:
  - quality-gated blocks: `Rock_Basalt`, limestone, shale cobble, stalactites, geysers, Mithril-in-stone, Adamantite-in-magma (13 vanilla blocks);
  - gather types without a bare-hand spec: GoblinMetal, SoftWoods, Pickaxe_Tier0 (9 blocks);
  - Unbreakable blocks.

  The owner has an idea for fixing this (not yet discussed).

## Code conventions

- One responsibility per class.
- Log through the plugin logger with a clear prefix; no `System.out`.
- Keep game-independent logic in plain Java classes and unit-test it without a server.
- Small, reviewable changes. Explain non-obvious Hytale-specific code with a short comment.
- Don't add third-party dependencies without asking.
