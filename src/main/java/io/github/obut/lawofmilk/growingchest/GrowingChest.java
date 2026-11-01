package io.github.obut.lawofmilk.growingchest;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * The growing chest: a single (not double) {@code Furniture_Ancient_Chest_Small} standing on a
 * {@code Metal_Goblin_Iron_Pipe_Large_Mouthpiece}, which stands on a
 * {@code Metal_Goblin_Iron_Decorative}, both with rotation index 0 (the chest may face any way).
 * Every {@link #INTERVAL} seconds each unique item ID in the chest gains one fresh item: added to an
 * existing stack, or to a free slot if no stack has room. Nothing shows this in game. All three
 * blocks are unchanged vanilla blocks.
 */
public final class GrowingChest {

    static final String CHEST_ID = "Furniture_Ancient_Chest_Small";
    static final String PIPE_ID = "Metal_Goblin_Iron_Pipe_Large_Mouthpiece";
    static final String BASE_ID = "Metal_Goblin_Iron_Decorative";
    /** Required rotation index of the pipe and the base (yaw 0, pitch 0, roll 0). */
    static final int REQUIRED_ROTATION = 0;
    /** Seconds between growth steps. */
    static final float INTERVAL = 60f;

    private GrowingChest() {
    }

    public static void register(JavaPlugin plugin) {
        plugin.getChunkStoreRegistry().registerSystem(new ChestGrowthSystem(plugin.getLogger()));
    }
}
