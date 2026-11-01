# CLAUDE.md — Law Of Milk (Hytale server plugin)

## What this project is

A Hytale (Early Access, 0.7.5+) server plugin + asset pack. Name: "Law Of Milk" (mod ID `lawofmilk`, package `io.github.olvrbu.lawofmilk`, main class `LawOfMilkPlugin`).

The mod is a collection of small, independent fun ideas, of very different implementation depth. It's both for fun and for exploring and pushing what the modding API can do. Each idea lives in its own subpackage of `io.github.olvrbu.lawofmilk` with a `register(...)` method called from `LawOfMilkPlugin.setup()`, and its own section under [Features](#features).

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

### Super apple (`superapple`): done, confirmed in game 2026-10-22, cone shape confirmed 2026-11-01

- **Activation:** left-clicking a block with a vanilla `Plant_Fruit_Apple` in hand, while the storage's **bottom-right slot** holds at least one `Food_Pie_Pumpkin` and the **slot to its left** at least one `Food_Pie_Apple`. Hotbar and backpack don't count.
  - **Slot numbers:** storage is 9 columns × 4 rows (slots 0–35). The client shows it row by row from the top left, so bottom-right = last slot (35) and its left neighbor = 34; confirmed in game 2026-10-22.
  - **Replaced condition:** exactly 3 `Ingredient_Crystal_Void` + 1 `Ingredient_Crystal_White` anywhere in the inventory (the earlier version).
- **Effect:** the clicked block and every block of the same type (same block id, filler cells skipped) inside a cone vanish at once; the blocks don't have to be connected.
  - **Cone (decided and confirmed in game 2026-11-01):** pointing along the view direction (`HeadRotation`). The tip is a vertical line from the player's feet up `TIP_HEIGHT` (2 blocks, the player's height), not a point. A block counts if its center, seen from some point of the tip (tested every 0.1 block), is at most `SuperApple.LENGTH` (10) away and at most `HALF_ANGLE_DEGREES` (20°, so a 40° opening angle) off the axis. So the cut is 2 blocks tall at the player, widens from there, and has a rounded far end. The clicked block is always removed, even if its center is just outside. Math in `Cone` (unit-tested).
  - **First cone version (2026-11-01, tried in game the same day):** point tip at the eyes (feet + model eye height), 15° half angle. The owner wanted the tip as tall as the player and a wider angle.
  - **Replaced shape:** a sphere of radius 5 around the clicked block (2026-10-22 to 2026-11-01). No damage, no cracks, no drops (owner's choice), and **any** block type works, including quality-gated (`Rock_Basalt`) and Unbreakable blocks (owner's choice: allow everything).
- **Mechanism (decided 2026-10-22):**
  - **Apple override:** our asset pack overrides the vanilla `Plant_Fruit_Apple` (`Server/Item/Items/Plant/Fruit/Plant_Fruit_Apple.json`, same path as vanilla; confirmed in game that ours wins). It's a copy of the vanilla JSON plus `Interactions`:
    - `Primary`: the custom `LawOfMilk_SuperApple` interaction (`SuperAppleInteraction`, a `SimpleBlockInteraction` with `UseLatestTarget`) with `"Failed": "Block_Primary"`, so without the condition the vanilla punch chain runs. Cooldowns are copied from the vanilla root `Block_Primary`.
    - `Secondary`: eating, stated explicitly.

    **Re-sync this JSON with the vanilla apple after game updates.**
  - **Why an interaction:** it runs before the server's gather/quality checks, which only happen inside the vanilla `BreakBlock` interaction.
- **Removal:** `AreaRemover` sets the blocks straight to air with `BlockOperations.setBlock` (full overload, chunk section components cached per 32³ section, `NO_SEND_PARTICLES`). This means no `BreakBlockEvent` (other plugins can't veto it), no drops, sounds, physics or neighbor updates, no on-break interactions. Heightmap, lighting, block entities and filler cells are still updated.
  - **No commands (decided 2026-11-01):** the dev command `/lawofmilk appleradius` (radius, later cone length, at runtime) was removed together with the whole `/lawofmilk` command; the mod has no commands now.
  - **Timing:** every removal logs its block count and time (`[SuperApple]`, debug level).
- **History (2026-10-22):**
  1. Iron Hatchet trigger with `DamageBlockEvent` (damage boost) + `BreakBlockEvent` (area), area via per-block `BlockHarvestUtils.performBlockBreak`.
  2. Area switched to `setBlock`.
  3. Apple trigger; the hatchet was removed.
  4. The damage/break event hooks were replaced by the apple's own primary interaction, because quality-gated blocks never reach `DamageBlockEvent`.

  `PlayerMouseButtonEvent` was tried as a direct "hit" hook: it does **not** fire for normal clicks.
- **Measured (radius 32, `setBlock`):** 74k stone blocks took 143 ms, 61k took 78 ms, 12.8k basalt took 25 ms. Per-block `performBlockBreak` took 300–400 ms for about 70k, and the client showed the breaks in shells moving outward from the player, because it applies chunk section updates nearest-first.

### Jungle candle (`junglecandle`): done, confirmed in game 2026-10-23, health regen and fall damage reduction confirmed 2026-10-24

- **Trigger (decided 2026-10-23):** the vanilla `Furniture_Jungle_Candle` (already `"Utility": { "Usable": true }`) in **any** of the 4 off-hand slots, active or not, whatever is in the main hand. The vanilla item is not changed or overridden.
- **Effect (decided 2026-10-23):** stamina costs reduced by 75% (`JungleCandle.STAMINA_REDUCTION`; 50% until 2026-11-01). Hidden: no icon, tooltip or status effect.
- **More hidden effects (decided 2026-10-24, confirmed in game the same day; the owner likes the regen rate), same trigger:**
  - **Health regen:** +1 HP every 3 s (`REGEN_AMOUNT`, `REGEN_INTERVAL`; every 5 s until 2026-11-01) via `HealthRegenSystem`, a player `EntityTickingSystem` with a per-player `RegenTimer` (unit-tested). It skips dead players (`DeathComponent`, same check as vanilla stat regen) and players at full health; the timer restarts at full health, so the first heal comes 3 s after a hit. Not paused by taking damage.
  - **Fall damage −75%** (`FALL_DAMAGE_REDUCTION`; −50% until 2026-11-01) via `FallDamageReductionSystem`, a `DamageEventSystem` in the damage module's filter group (like vanilla `ArmorDamageReduction`). It scales `Damage` events whose cause is `Fall`, which `DamageSystems` creates for players on landing. Needs the manifest dependency `Hytale:DamageModule` so the filter group exists when our `setup()` runs.
- **Mechanism:** `StaminaSaverSystem`, a player `EntityTickingSystem`, remembers each player's stamina after its last tick and gives back 75% of any drop since then (`StaminaRefund`, unit-tested). It's done this way because stamina is spent in many unrelated places (sprint/glide drain in `Stamina.json`, `ChangeStat`/`ChangeStatWithModifier` in about 140 weapon and dodge interaction assets, shield blocking in `DamageSystems.DamageStamina`), none of which an off-hand item can scale. The only vanilla cost modifier, armor `InteractionModifiers`, is armor-only and only works through `ChangeStatWithModifier`.
- **Stronger values (owner, 2026-11-01):** stamina −75%, regen every 3 s, fall damage −75%; not retested in game (owner's choice).
- **Known limits:**
  - The "enough stamina?" checks (`StatsConditionWithModifier` `Costs`) still need the full cost.
  - A cost that crosses 0 still triggers the `Stamina.json` `MinValueEffects` (regen delay, `Stamina_Broken` check) before the refund.
  - Only the net change per tick is seen, so regen in the same tick hides a tiny part of a cost.
- **Logging:** `[JungleCandle]` debug lines when the candle starts and stops counting for a player.
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

### Void Capture Crate (`hostilecrate`): built 2026-10-25, working in game 2026-10-26 (owner tested with the 0.9 threshold and the final recipe)

- **Idea (owner, 2026-10-25):** like the vanilla Capture Crate (`Tool_Capture_Crate`, small critters only), but hard to get and for hostile creatures.
- **Item:** `Tool_Capture_Crate_Hostile`, shown as "Void Capture Crate" (renamed from "Hostile Capture Crate" on 2026-10-26; the item ID, package and interaction type keep "Hostile"), `"Parent": "Tool_Capture_Crate"` so it looks like the vanilla crate; `Quality: Epic`, `ItemLevel: 30`. Same sounds and fail/success branches as vanilla.
- **Targets (decided 2026-10-25: everything hostile, bosses included):** any NPC whose role's resolved `DefaultPlayerAttitude` is `Hostile` (read from the `WorldSupport` component). The NPC group `LawOfMilk_Hostile_Crate` (`Server/NPC/Groups/LawOfMilk/`) matches every role (`"*"`) and is only there because the vanilla code requires a group; add `ExcludeRoles` there to make something uncatchable. Neutral creatures (e.g. bears) don't count, even when they're fighting you.
- **Condition (decided 2026-10-25):** only catchable at or below `"MaxHealthFraction"` of max health (item JSON, 0.25; raised to 0.9 for in-game testing on 2026-10-26 and set back the same day). Otherwise the catch fails, plays the vanilla fail sound and sends a chat message (`lawofmilk.hostileCrate.notHostile` / `.tooHealthy`). Logic in `CaptureRule` (unit-tested).
- **Single use (decided 2026-10-25):** releasing the creature (clicking a block, as in vanilla) removes the crate from the hotbar slot. The released NPC is a fresh spawn of the role (full health), as in vanilla.
- **Obtaining (decided 2026-10-25: fragment-only recipe):** 1 `Coffer_Key` (Veilbound Key) + 10 `Wood_Azure_Trunk` (Azure Log) + 10 `Ingredient_Life_Essence_Concentrated` (Greater Essence of Life) + 2 `Ingredient_Voidtaken_Grizzly_Hide` at the Arcanist's Workbench (`Arcanebench` / `Arcane_Misc`), 10 s (ingredients decided 2026-10-26; craft time not discussed). The key is the only fragment-bound item in 0.7.2 (only `Goblin_Breach` has `EphemeralItems`), so it's crafted in the Guttering Hollows like the Void Scythe. The crate itself isn't ephemeral and can be taken out.
- **Mechanism:** `HostileCrateInteraction` (type `LawOfMilk_UseHostileCrate`) subclasses the vanilla `UseCaptureCrateInteraction` (`builtin/adventure/farming/interactions/`) and inherits its codec (`AcceptedNpcGroups`, `FullIcon`). `tick0` runs our checks before the vanilla catch; `interactWithBlock` removes the crate after the vanilla release. On refusal it jumps to the `Failed` label itself, copying `SimpleInteraction.tick0`. **Re-check against the vanilla interaction after game updates.**
- **Unverified:** the `nameFull` lang key (`Void Capture Crate ({npc})`) mirrors vanilla's `items.Tool_Capture_Crate.nameFull`, but the client code that reads it isn't visible to us.

### Growing chest (`growingchest`): done, confirmed in game 2026-11-01

- **Idea (owner, 2026-11-01):** a chest on a goblin pipe pedestal slowly multiplies its contents.
- **Structure (decided 2026-11-01):** a single `Furniture_Ancient_Chest_Small` (the double chest is a different block, `Furniture_Ancient_Chest_Large`, so it doesn't count) directly on a `Metal_Goblin_Iron_Pipe_Large_Mouthpiece`, directly on a `Metal_Goblin_Iron_Decorative`. Both lower blocks must have **rotation index 0** (yaw 0, pitch 0, roll 0; the owner checked in game which way that is). The chest may face any way. All three are unchanged vanilla blocks.
  - **Rotation index:** `RotationTuple` index = `roll·16 + pitch·4 + yaw` (each 0–3 for 0/90/180/270°). Both lower blocks use `VariantRotation` `DoublePipe`, which only allows 0, 4–7 (pitch 90°) and 8 (pitch 180°). No vanilla command prints the index (`/block get` shows only the ID; `/block inspectrotation` only draws coloured debug cubes).
- **Growth (decided 2026-11-01):** every `GrowingChest.INTERVAL` seconds, each **unique item ID** in the chest gains one item, however many stacks of it there are. The item goes to a stack with room (up to its max stack size), else to a free slot; if neither exists, nothing happens. Non-stackable items get one copy in a free slot.
  - **Fresh copies (decided 2026-11-01):** uniqueness is by item ID only, and the new item is always a fresh `new ItemStack(id, 1)`: full durability, no metadata (a filled Void Capture Crate yields empty crates). A stackable item whose stacks carry metadata (rare) therefore starts its own fresh stack.
  - **Rate (decided 2026-11-01):** one item per unique ID every 60 s (`GrowingChest.INTERVAL`). Tested in game at 2 s with a dev command `/lawofmilk chestinterval`, which was removed afterwards (owner, 2026-11-01).
  - Only while the chunk is loaded; no catch-up for time the chunk was unloaded.
- **Hidden (decided 2026-11-01):** no sound, particles or UI. `[GrowingChest]` debug lines when a chest starts or stops growing (with its position). "Stopped" is only logged when the stack breaks while loaded; a chunk unload or a broken chest just ends ticking, and the reloaded or new chest logs "is growing" again.
- **Mechanism:** `ChestGrowthSystem`, a chunk store `EntityTickingSystem` over all container blocks (`ItemContainerBlock` + `BlockModule.BlockStateInfo`, the same block entity lookup as the vanilla `BenchSystems`). It checks the chest's base block ID (`getDefaultStateKey()`, falling back to the ID: an opened or closed chest is a different block type per state, `OpenWindow`/`CloseWindow`; comparing the plain ID stopped growth after the first opening, fixed 2026-11-01) and the two blocks below (ID, not a filler cell, rotation index), keeps a per-chest `GrowthTimer` (unit-tested), and adds items with `ItemContainer.addItemStack`. The container's change event marks the chest for saving (vanilla `ItemContainerSystems`). Unique IDs in slot order: `UniqueItems` (unit-tested). When the chest is nearly full, IDs earlier in slot order get the free slots first.

## Versions

Targets Hytale **0.7.5** since 2026-11-01 (owner; manifest `>=0.7.5 <0.8.0`, was 0.7.2). Vanilla assets: `~/.gradle/caches/hytale-assets/release-0.7.5-Assets.zip`.

Version in `gradle.properties` (`./gradlew updatePluginManifest` after changing it). Every release gets a short, player-facing entry in `CHANGELOG.md` (decided 2026-10-24). 0.1.0 is the state of tag `v0.1.0` (2026-10-23); the owner then jumped to 1.1.0 on 2026-10-24.

- **1.4.0 (2026-11-01):** only the package/group rename to `olvrbu` (mod ID `olvrbu:lawofmilk`; no data folder or commands, so only the old jar must go). Released without an in-game test (owner's choice). First release built by `release.yml`.

**Releasing (since 2026-11-01):** bump `version`, `./gradlew updatePluginManifest`, give the changelog section its heading (`## <version> (<date>)`), commit, tag `v<version>`, push the tag. `.github/workflows/release.yml` (same as the other mods) builds the jar on GitHub and publishes a GitHub Release with it. v0.1.0–v1.2.0 target 0.7.2, which GitHub can't download anymore, so their release jars were built in the sandbox and uploaded with `gh release create`.

## Code conventions

- One responsibility per class.
- Log through the plugin logger with a clear prefix; no `System.out`.
- **Silent unless debugging (decided 2026-11-01):** the mod prints nothing to the console in normal play. All its log lines use `logger.atFine()`; the server's default level is INFO, so they're hidden. To see them, set the plugin logger (`lawofmilk|P`) to FINE: `log lawofmilk|P --level=FINE` in the server console (until restart; add `--save` to store it in `run/config.json` `LogLevels`; `--level=INFO` turns it off again), or start the server with `--log lawofmilk|P:FINE`. Warnings and errors may still use higher levels. Player-facing chat messages (the capture crate's refusals) are not console output and stay.
- Keep game-independent logic in plain Java classes and unit-test it without a server.
- Small, reviewable changes. Explain non-obvious Hytale-specific code with a short comment.
- Don't add third-party dependencies without asking.
