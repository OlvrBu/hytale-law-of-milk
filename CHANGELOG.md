# Changelog

## 1.4.0 (2026-11-01)

- The mod's ID changed from `obut:lawofmilk` to `olvrbu:lawofmilk` (new author name).
  **When updating, delete the old `lawofmilk-1.3.0.jar`** (or older) from your `mods/` folder,
  otherwise both versions load. Law Of Milk keeps no settings or data files, so there is nothing
  else to move. Items and blocks keep their IDs.
- No gameplay changes.

## 1.3.0 (2026-11-01)

Requires Hytale 0.7.5 or newer.

- **Growing chest:** a single Ancient Small Chest standing on a Large Iron Pipe Mouthpiece, which
  stands on a Decorative Goblin Iron block (both placed upright, the chest facing any way), adds one
  of each item it holds every minute. Two stacks of the same item still count once. New items fill
  existing stacks first and then free slots, and are always fresh copies (full durability, no
  contents). Nothing is added while the chunk isn't loaded.
- **Super apple:** now removes blocks in a cone along your view instead of a sphere: 10 blocks long,
  40 degrees across, and as tall as you right where you stand.
- **Super apple:** the `/lawofmilk appleradius` command is gone; the mod has no commands now.
- **Jungle candle:** stronger: stamina costs −75% (was −50%), 1 health every 3 seconds (was 5),
  fall damage −75% (was −50%).
- The mod no longer writes to the server console in normal play. Its messages are at debug level;
  show them with `log lawofmilk|P --level=FINE` in the server console.

## 1.2.0 (2026-10-26)

- **Void Capture Crate:** a new capture crate for hostile creatures, bosses included. A creature can
  only be caught once it's below 25% health, and the crate breaks when you release it. It's crafted
  at the Arcanist's Workbench from 1 Veilbound Key, 10 Azure Logs, 10 Greater Essence of Life and
  2 Voidtaken Grizzly Hides, so it can only be made inside the Guttering Hollows.

## 1.1.0 (2026-10-24)

- **Suspicious plushies:** using one now plays the normal plushie squeak, with a 1% chance of the
  gnome scream from the plushie itself.
- **Suspicious plushies:** random screams now pick a player within 15 blocks and play on the ground
  5–16 blocks from them, more often behind them than in front. With nobody nearby they still play
  within 6 blocks of the plushie.
- **Jungle candle:** also regenerates 1 health every 5 seconds and halves fall damage while it's in
  an off-hand slot.

## 0.1.0 (2026-10-23)

First version.

- **Suspicious plushies:** Kweebec and Goblin plushie copies, crafted at the Builder's Workbench,
  that play a gnome scream within 6 blocks about every 1.5 hours and whenever they're used.
- **Super apple:** with a pumpkin pie and an apple pie in the last two storage slots, left-clicking a
  block with an apple removes every block of that type within a radius (default 5, set with
  `/lawofmilk appleradius`, operator only).
- **Jungle candle:** a vanilla Jungle Candle in any off-hand slot halves stamina costs.
- **Void Scythe:** craftable at the Arcanist's Workbench from 2 Veilbound Keys, 10 Voidhearts,
  2 Voidtaken Grizzly Hides and 10 Adamantite Bars, which only works inside the Guttering Hollows,
  since the keys don't leave it.
