package io.github.obut.lawofmilk.superapple;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

/**
 * The super apple: hitting blocks with an apple in hand while the player carries exactly the right
 * crystals mines any material in 4 hits, and breaking a block also removes every block of the same
 * type within {@link #radius} blocks (without drops; the block actually hit drops normally).
 *
 * <p>The apple isn't a tool, so hits with it use the server's bare-hand gather specs
 * (Server/Item/Unarmed/Gathering). Those cover nearly every gather type with a small power, so
 * {@link DamageBlockEvent} fires and the plugin can raise the damage.
 */
public final class SuperApple {

    static final String ITEM_ID = "Plant_Fruit_Apple";
    static final String VOID_CRYSTAL_ID = "Ingredient_Crystal_Void";
    static final int VOID_CRYSTALS = 3;
    static final String WHITE_CRYSTAL_ID = "Ingredient_Crystal_White";
    static final int WHITE_CRYSTALS = 1;

    /** Damage per hit on any material; blocks have 1.0 health, so 4 hits break anything. */
    static final float DAMAGE = 0.3f;

    static final int MAX_RADIUS = 32;
    /** Area radius in blocks. Changed at runtime by {@code /lawofmilk appleradius}; not saved. */
    static volatile int radius = 5;

    private SuperApple() {
    }

    public static void register(JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new AppleDamageSystem());
        plugin.getEntityStoreRegistry().registerSystem(new AppleBreakSystem(plugin.getLogger()));
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
