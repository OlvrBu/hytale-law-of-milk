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

### Suspicious plushies (`gnomeblock`): done, confirmed in game 2026-10-22, on-use change confirmed 2026-10-24, random scream position change confirmed 2026-10-24

- **Items:** `Deco_Kweebec_Plush_Suspicious` and `Deco_Goblin_Plush_Suspicious` ("Suspicious … Plushie"). They inherit the vanilla plushies via `"Parent"`, so they look the same.
- **Recipes:** both are crafted at the Builder's Workbench (`StructuralCrafting` / `Builders`, category `Decorative`). Each comes from 1 of its vanilla plushie; this is one-way. The goblin one can also be crafted from 1 Kweebec Plushie, via a standalone recipe file in `Server/Item/Recipes/LawOfMilk/`, since an item JSON holds only one `Recipe`. No natural world generation.
- **Random screams:** they play the gnome scream (`SFX_LawOfMilk_Gnome_Scream`, `Common/Sounds/LawOfMilk/Gnome_Scream.ogg`, converted from an mp3 the owner supplied).
  - **Position (decided and confirmed in game 2026-10-24):** if any players are within 15 blocks of the plushie, one is picked at random and the scream plays on solid ground 5–16 blocks from them horizontally (3–10 at first; widened the same day because the owner likes it sounding distant and quieter, not retested) (ground searched ±4 blocks around their feet). Spots out of view are favored: weight `(2 − cos θ) / 3` over the horizontal angle θ from their look direction (1/3 in front, 2/3 to the side, 1 behind); the owner wanted only a mild bias. Without a nearby player, or no ground after 20 tries, it plays at a random point within 6 blocks of the plushie (`OffsetRadius`). The math is in `ScreamSpot` (unit-tested); the world lookups in `ScreamSound.playNearPlayer`. Other players near the spot hear it too.
  - **Replaced behavior:** always a random point within 6 blocks of the plushie (2026-10-22).
  - **Mechanism:** a custom random tick procedure, `LawOfMilk_RandomSound`, with `Chance` 0.05.
  - **Frequency:** every loaded block gets a random tick about every 8192 server ticks (30 TPS), so this is about one scream per plushie every 1.5 hours. Chance 1.0 (about every 4.6 min) is the fastest this mechanism allows.
- **On use (decided 2026-10-24):** using a plushie plays the vanilla squeak (`SFX_Plushie_Interact`), except for a 1% chance to play the gnome scream (the owner also calls it the "woo") at the plushie itself instead.
  - **Mechanism:** the custom interaction `LawOfMilk_PlaySoundAround` with `"Chance": 0.01` and `"OffsetRadius": 0`. It's rolled on the server; on a miss the interaction fails and its `"Failed"` branch is the vanilla `Simple` interaction with the squeak (same fallback pattern as the super apple).
  - **Replaced behavior:** every use played the scream at a random point within 6 blocks (2026-10-22).
- **Rejected:** an echo variant of the sound. The owner didn't like it, so it was removed.

### Super apple (`superapple`): done, confirmed in game 2026-10-22

- **Activation:** left-clicking a block with a vanilla `Plant_Fruit_Apple` in hand, while the storage's **bottom-right slot** holds at least one `Food_Pie_Pumpkin` and the **slot to its left** at least one `Food_Pie_Apple`. Hotbar and backpack don't count.
  - **Slot numbers:** storage is 9 columns × 4 rows (slots 0–35). The client shows it row by row from the top left, so bottom-right = last slot (35) and its left neighbor = 34; confirmed in game 2026-10-22.
  - **Replaced condition:** exactly 3 `Ingredient_Crystal_Void` + 1 `Ingredient_Crystal_White` anywhere in the inventory (the earlier version).
- **Effect:** the clicked block and every block of the same type (same block id, filler cells skipped) within a sphere of radius `SuperApple.radius` vanish at once; the blocks don't have to be connected. No damage, no cracks, no drops (owner's choice), and **any** block type works, including quality-gated (`Rock_Basalt`) and Unbreakable blocks (owner's choice: allow everything).
- **Mechanism (decided 2026-10-22):**
  - **Apple override:** our asset pack overrides the vanilla `Plant_Fruit_Apple` (`Server/Item/Items/Plant/Fruit/Plant_Fruit_Apple.json`, same path as vanilla; confirmed in game that ours wins). It's a copy of the vanilla JSON plus `Interactions`:
    - `Primary`: the custom `LawOfMilk_SuperApple` interaction (`SuperAppleInteraction`, a `SimpleBlockInteraction` with `UseLatestTarget`) with `"Failed": "Block_Primary"`, so without the condition the vanilla punch chain runs. Cooldowns are copied from the vanilla root `Block_Primary`.
    - `Secondary`: eating, stated explicitly.

    **Re-sync this JSON with the vanilla apple after game updates.**
  - **Why an interaction:** it runs before the server's gather/quality checks, which only happen inside the vanilla `BreakBlock` interaction.
- **Removal:** `AreaRemover` sets the blocks straight to air with `BlockOperations.setBlock` (full overload, chunk section components cached per 32³ section, `NO_SEND_PARTICLES`). This means no `BreakBlockEvent` (other plugins can't veto it), no drops, sounds, physics or neighbor updates, no on-break interactions. Heightmap, lighting, block entities and filler cells are still updated.
  - **Dev command:** `/lawofmilk appleradius <0-32>` sets the radius at runtime (default 5). It's operator-only and not saved.
  - **Timing:** every removal logs its block count and time.
- **History (2026-10-22):**
  1. Iron Hatchet trigger with `DamageBlockEvent` (damage boost) + `BreakBlockEvent` (area), area via per-block `BlockHarvestUtils.performBlockBreak`.
  2. Area switched to `setBlock`.
  3. Apple trigger; the hatchet was removed.
  4. The damage/break event hooks were replaced by the apple's own primary interaction, because quality-gated blocks never reach `DamageBlockEvent`.

  `PlayerMouseButtonEvent` was tried as a direct "hit" hook: it does **not** fire for normal clicks.
- **Measured (radius 32, `setBlock`):** 74k stone blocks took 143 ms, 61k took 78 ms, 12.8k basalt took 25 ms. Per-block `performBlockBreak` took 300–400 ms for about 70k, and the client showed the breaks in shells moving outward from the player, because it applies chunk section updates nearest-first.

### Jungle candle (`junglecandle`): done, confirmed in game 2026-10-23

- **Trigger (decided 2026-10-23):** the vanilla `Furniture_Jungle_Candle` (already `"Utility": { "Usable": true }`) in **any** of the 4 off-hand slots, active or not, whatever is in the main hand. The vanilla item is not changed or overridden.
- **Effect (decided 2026-10-23):** stamina costs reduced by 50% (`JungleCandle.STAMINA_REDUCTION`). Hidden: no icon, tooltip or status effect.
- **Mechanism:** `StaminaSaverSystem`, a player `EntityTickingSystem`, remembers each player's stamina after its last tick and gives back half of any drop since then (`StaminaRefund`, unit-tested). It's done this way because stamina is spent in many unrelated places (sprint/glide drain in `Stamina.json`, `ChangeStat`/`ChangeStatWithModifier` in about 140 weapon and dodge interaction assets, shield blocking in `DamageSystems.DamageStamina`), none of which an off-hand item can scale. The only vanilla cost modifier, armor `InteractionModifiers`, is armor-only and only works through `ChangeStatWithModifier`.
- **Known limits:**
  - The "enough stamina?" checks (`StatsConditionWithModifier` `Costs`) still need the full cost.
  - A cost that crosses 0 still triggers the `Stamina.json` `MinValueEffects` (regen delay, `Stamina_Broken` check) before the refund.
  - Only the net change per tick is seen, so regen in the same tick hides a tiny part of a cost.
- **Logging:** `[JungleCandle]` info lines when the candle starts and stops counting for a player.
- **Confirmed in game (2026-10-23):** half stamina use while sprinting, also from an inactive off-hand slot with a weapon in the main hand; normal again once the candle is removed. No visible stamina bar flicker from the server-side refund, even with charged weapon abilities.
- **Parked idea (2026-10-23): grass and flowers grow around a candle holder.** The owner scrapped it for now; nothing is implemented. Research so far (0.7.2):
  - **Vanilla:** wild grass, ferns and flowers (`Plant_Grass_*`, `Plant_Fern_*`, 82 `Plant_Flower_*`) are placed by world generation only. They have no random tick and no growth or farming data, so broken ones never come back. They need soil below (`Support.Down: Type=Soil`; flowers also allow `SubType=Planter`).
  - **Only the ground block spreads:** `Soil_Grass` (and its 13 variants) has `RandomTickProcedure` `SpreadTo` (`builtin/randomtick/procedures/SpreadToProcedure.java`). It turns `Spreadable=Grass` blocks (`Soil_Dirt`) in the 8 directions, ±1 Y, into grass if the light above is high enough. It reverts to `RevertBlock` `Soil_Dirt` under a solid full cube or a disallowed fluid. Leaves also use `SpreadTo`. A third procedure, `PlaceBlock` (`Placements`, `Offset`, `RequireEmptyTarget`), is only used by a test block in vanilla.
  - **Planned approach:** a player ticking system, throttled to every few seconds, that reuses `JungleCandle.isInOffhand`. It picks a few random columns within a radius and finds each top block: `Soil_Dirt` with air above becomes `Soil_Grass`, `Soil_Grass` with air above gets a random plant, and anything else is skipped. Blocks are set with `BlockOperations.setBlock` (see `superapple/AreaRemover`), deferred with `world.execute(...)`, since the candle check runs in entity systems and blocks belong to the chunk store (verify this when building it). No asset changes needed.
  - **Open questions for the owner:** which plants (jungle-themed, generic lush, or matching the biome), radius and rate, whether dirt becomes grass too, and whether it grows only while the player walks.

### Void Scythe recipe (assets only, no subpackage): done, confirmed in game 2026-10-23

- **Idea (owner, 2026-10-23):** make the unused vanilla `Weapon_Battleaxe_Scythe_Void` ("Void Scythe") craftable only inside the Guttering Hollows fragment. Players bring an Arcanist's Workbench, find a Veilbound Key and defend the crafter during a long craft.
- **Recipe (decided 2026-10-23):** `Server/Item/Recipes/LawOfMilk/Weapon_Battleaxe_Scythe_Void.json`: 2 `Coffer_Key` (raised from 1 on 2026-10-23 after the in-game test; the change itself wasn't retested) + 10 `Ingredient_Voidheart` + 2 `Ingredient_Voidtaken_Grizzly_Hide` + 10 `Ingredient_Bar_Adamantite` at the Arcanist's Workbench (`Crafting` / `Arcanebench`, category `Arcane_Misc`), `TimeSeconds` exactly 30.
- **Scythe stays vanilla (decided 2026-10-23):** no override; it's `Quality: Common`, `ItemLevel: 30`, 112/224 physical. A standalone recipe file means nothing to re-sync.
- **Why it only works in the fragment:** `Coffer_Key` is listed in `"EphemeralItems"` of `Server/PortalTypes/Goblin_Breach.json` (the Guttering Hollows), so it's removed when the player leaves. It drops from `Void_Spawn_Entombed` and sits in goblin coffer prefabs. The fragment has `TimeLimitSeconds: 600` and Void invasion stages from 120 s.
- **Craft behavior (from `builtin/crafting/component/CraftingManager`, `window/BenchWindow`):** inputs are taken when the craft starts; progress only runs while the bench window is open; closing it (or the player entity being removed) cancels the craft and refunds the inputs, including the key.
- **Confirmed in game (2026-10-23):** the owner crafted the scythe inside the Guttering Hollows, so a bench can be placed there (configs don't override `AllowBlockPlacement`, default true). Not reported: whether taking damage closes the bench window on the client.

## Code conventions

- One responsibility per class.
- Log through the plugin logger with a clear prefix; no `System.out`.
- Keep game-independent logic in plain Java classes and unit-test it without a server.
- Small, reviewable changes. Explain non-obvious Hytale-specific code with a short comment.
- Don't add third-party dependencies without asking.
