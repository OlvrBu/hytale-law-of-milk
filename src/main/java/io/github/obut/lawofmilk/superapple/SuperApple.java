package io.github.obut.lawofmilk.superapple;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

/**
 * The super apple: left-clicking a block with an apple in hand, while the player carries exactly
 * the right crystals, instantly removes that block and every block of the same type within
 * {@link #radius} blocks (any block type, no drops).
 *
 * <p>Our asset pack overrides the vanilla {@code Plant_Fruit_Apple} item to make
 * {@link SuperAppleInteraction} its primary interaction. Keep that JSON in sync with the vanilla
 * apple when the game updates.
 */
public final class SuperApple {

    static final String ITEM_ID = "Plant_Fruit_Apple";
    static final String VOID_CRYSTAL_ID = "Ingredient_Crystal_Void";
    static final int VOID_CRYSTALS = 3;
    static final String WHITE_CRYSTAL_ID = "Ingredient_Crystal_White";
    static final int WHITE_CRYSTALS = 1;

    static final int MAX_RADIUS = 32;
    /** Area radius in blocks. Changed at runtime by {@code /lawofmilk appleradius}; not saved. */
    static volatile int radius = 5;

    // The interaction is created by the server from JSON, so it can't be handed the logger.
    private static HytaleLogger logger;

    private SuperApple() {
    }

    /** Must run in the plugin's setup(), before the server loads the apple that refers to the interaction. */
    public static void register(JavaPlugin plugin) {
        logger = plugin.getLogger();
        plugin.getCodecRegistry(Interaction.CODEC)
                .register(SuperAppleInteraction.TYPE, SuperAppleInteraction.class, SuperAppleInteraction.CODEC);
    }

    static HytaleLogger logger() {
        return logger;
    }

    /**
     * Whether {@code itemInHand} is an apple and the player carries exactly 3 void crystals and
     * 1 white crystal, counted over hotbar, storage and backpack.
     */
    static boolean isActive(@Nullable ItemStack itemInHand, ComponentAccessor<EntityStore> accessor, Ref<EntityStore> player) {
        if (ItemStack.isEmpty(itemInHand) || !ITEM_ID.equals(itemInHand.getItemId())) {
            return false;
        }
        CombinedItemContainer inventory = InventoryComponent.getCombined(accessor, player, InventoryComponent.HOTBAR_STORAGE_BACKPACK);
        return count(inventory, VOID_CRYSTAL_ID) == VOID_CRYSTALS && count(inventory, WHITE_CRYSTAL_ID) == WHITE_CRYSTALS;
    }

    private static int count(CombinedItemContainer inventory, String itemId) {
        return inventory.countItemStacks(stack -> itemId.equals(stack.getItemId()));
    }
}
