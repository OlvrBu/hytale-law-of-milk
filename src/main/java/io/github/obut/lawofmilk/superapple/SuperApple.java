package io.github.obut.lawofmilk.superapple;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

/**
 * The super apple: left-clicking a block with an apple in hand, while the right pies sit in the
 * last two storage slots (see {@link #isActive}), instantly removes that block and every block of the same type within
 * {@link #radius} blocks (any block type, no drops).
 *
 * <p>Our asset pack overrides the vanilla {@code Plant_Fruit_Apple} item to make
 * {@link SuperAppleInteraction} its primary interaction. Keep that JSON in sync with the vanilla
 * apple when the game updates.
 */
public final class SuperApple {

    static final String ITEM_ID = "Plant_Fruit_Apple";
    /** Required in the storage's bottom-right slot. */
    static final String LAST_SLOT_ITEM_ID = "Food_Pie_Pumpkin";
    /** Required in the slot to the left of it. */
    static final String SECOND_LAST_SLOT_ITEM_ID = "Food_Pie_Apple";

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
     * Whether {@code itemInHand} is an apple and the player's storage (the 9x4 grid above the
     * hotbar) holds at least one pumpkin pie in its bottom-right slot and at least one apple pie in
     * the slot to the left of it.
     *
     * <p>Assumes the client shows storage slots row by row from the top left, so the bottom-right
     * slot is the last one (35 of 0-35); confirmed in game.
     */
    static boolean isActive(@Nullable ItemStack itemInHand, ComponentAccessor<EntityStore> accessor, Ref<EntityStore> player) {
        if (ItemStack.isEmpty(itemInHand) || !ITEM_ID.equals(itemInHand.getItemId())) {
            return false;
        }
        InventoryComponent.Storage storage = accessor.getComponent(player, InventoryComponent.Storage.getComponentType());
        if (storage == null) {
            return false;
        }
        ItemContainer slots = storage.getInventory();
        short last = (short) (slots.getCapacity() - 1);
        return last >= 1
                && holds(slots, last, LAST_SLOT_ITEM_ID)
                && holds(slots, (short) (last - 1), SECOND_LAST_SLOT_ITEM_ID);
    }

    private static boolean holds(ItemContainer slots, short slot, String itemId) {
        ItemStack stack = slots.getItemStack(slot);
        return !ItemStack.isEmpty(stack) && itemId.equals(stack.getItemId());
    }
}
