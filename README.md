# Palette Shuffle

A client-side Fabric mod for Minecraft **26.3**. Create named palettes of up to nine blocks, choose a hotbar slot, and build with a random mix. Inspired by the interaction design of [Block Palettes](https://modrinth.com/project/TQAZa0Gc); implemented independently.

## Use

- Press **B** (changeable under Controls → Key Binds) or open the configuration through **Mod Menu**.
- Create a palette, enter a name, and add blocks using the Inventory or All blocks picker.
- Select a block and use **− / +** to adjust its weight, or **Equal mix** to reset all weights.
- Save, select the palette card, choose a working hotbar slot, and click **Enable palette**, then **Back to game**.
- Place normally. The held block is the next block placed; successful client-predicted placements trigger a new choice. Chest interactions and failed placements do not reroll.
- Switching hotbar slots suspends placement. Reopen the gallery and click **Disable palette** to stop immediately. The gallery remembers the active palette and slot. Joining or leaving a server resets activation.

Survival uses ordinary inventory swaps and skips unavailable blocks. It stops when supplies are exhausted. Displaced items stay in your inventory. Disabling restores the previous survival item if it is still available. Creative supplies block stacks in that slot and restores its previous contents when disabled. Inventory screens suspend all automatic swapping. The selected slot is outlined with a palette HUD label.

Palettes are stored across worlds in `config/palette-shuffle.json`. Saves are atomic; malformed or unsupported files are preserved, with saving disabled until the file is repaired or moved aside. Missing modded block IDs remain saved and are skipped during placement.

## Requirements

Minecraft 26.3, Java 25, Fabric Loader 0.20.1+, and Fabric API for 26.3. Mod Menu is optional; no server mod is required. Server plugins can reject inventory actions or placements, so multiplayer compatibility needs testing on the target server.

## Build

```sh
./gradlew build
./gradlew runClient
```

On Windows, use `gradlew.bat build`. The playable JAR is `build/libs/palette-shuffle-0.2.0.jar`; the sources JAR is for development. GitHub Actions builds and runs tests, then uploads the playable JAR as `palette-shuffle-26.3`.

Version 0.1.1 fixes the menu-opening crash (`Can only blur once per frame`) by letting Minecraft handle the background once, before the palette screen content.

## Development status

Initial implementation. Unit tests cover palette validation, weighted selection, unavailable blocks, persistence, and corruption protection. In-game validation is still required before release: creative/survival placement, high-latency server rejection, empty stacks, screen layout at different GUI scales, key conflicts, and Mod Menu navigation. No claim of complete multiplayer validation is made.
