package io.github.obut.lawofmilk.superhatchet;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

/**
 * The super hatchet: an Iron Hatchet used while the player carries exactly the right crystals
 * mines any material (as fast as it chops wood), keeps its durability, and breaking a block also
 * removes every block of the same type within {@link #radius} blocks (without drops; the block
 * actually hit drops normally).
 */
public final class SuperHatchet {

    static final String TOOL_ID = "Tool_Hatchet_Iron";
    static final String VOID_CRYSTAL_ID = "Ingredient_Crystal_Void";
    static final int VOID_CRYSTALS = 3;
    static final String WHITE_CRYSTAL_ID = "Ingredient_Crystal_White";
    static final int WHITE_CRYSTALS = 1;

    /** Damage per hit on any material: the Iron Hatchet's power on wood (blocks have 1.0 health). */
    static final float DAMAGE = 0.3f;

    static final int MAX_RADIUS = 32;
    /** Area break radius in blocks. Changed at runtime by {@code /lawofmilk hatchetradius}; not saved. */
    static volatile int radius = 5;

    private SuperHatchet() {
    }

    public static void register(JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new HatchetDamageSystem());
        plugin.getEntityStoreRegistry().registerSystem(new HatchetBreakSystem(plugin.getLogger()));
    }

    /**
     * Whether {@code itemInHand} is the hatchet and the player carries exactly 3 void crystals and
     * 1 white crystal, counted over hotbar, storage and backpack.
     */
    static boolean isActive(@Nullable ItemStack itemInHand, ComponentAccessor<EntityStore> accessor, Ref<EntityStore> player) {
        if (ItemStack.isEmpty(itemInHand) || !TOOL_ID.equals(itemInHand.getItemId())) {
            return false;
        }
        CombinedItemContainer inventory = InventoryComponent.getCombined(accessor, player, InventoryComponent.HOTBAR_STORAGE_BACKPACK);
        return count(inventory, VOID_CRYSTAL_ID) == VOID_CRYSTALS && count(inventory, WHITE_CRYSTAL_ID) == WHITE_CRYSTALS;
    }

    private static int count(CombinedItemContainer inventory, String itemId) {
        return inventory.countItemStacks(stack -> itemId.equals(stack.getItemId()));
    }
}
