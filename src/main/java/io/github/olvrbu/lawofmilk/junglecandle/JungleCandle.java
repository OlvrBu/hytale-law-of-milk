package io.github.olvrbu.lawofmilk.junglecandle;

import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nullable;

/**
 * The jungle candle: while the vanilla {@code Furniture_Jungle_Candle} sits in any off-hand slot
 * (active or not), stamina costs are cut by {@link #STAMINA_REDUCTION}, health slowly regenerates
 * and fall damage is cut by {@link #FALL_DAMAGE_REDUCTION}. Nothing shows this in game. The vanilla
 * item is unchanged.
 */
public final class JungleCandle {

    static final String ITEM_ID = "Furniture_Jungle_Candle";
    /** Share of every stamina cost that is given back. */
    static final float STAMINA_REDUCTION = 0.75f;
    /** Health healed every REGEN_INTERVAL seconds. */
    static final float REGEN_AMOUNT = 1.0f;
    static final float REGEN_INTERVAL = 3.0f;
    /** Share of fall damage that is taken away. */
    static final float FALL_DAMAGE_REDUCTION = 0.75f;

    private JungleCandle() {
    }

    public static void register(JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new StaminaSaverSystem(plugin.getLogger()));
        plugin.getEntityStoreRegistry().registerSystem(new HealthRegenSystem());
        // Needs the damage module set up first (manifest dependency Hytale:DamageModule).
        plugin.getEntityStoreRegistry().registerSystem(new FallDamageReductionSystem());
    }

    /** Whether any of the off-hand (utility) slots holds the candle, not only the active one. */
    static boolean isInOffhand(@Nullable InventoryComponent.Utility utility) {
        if (utility == null) {
            return false;
        }
        ItemContainer slots = utility.getInventory();
        for (short slot = 0; slot < slots.getCapacity(); slot++) {
            ItemStack stack = slots.getItemStack(slot);
            if (!ItemStack.isEmpty(stack) && ITEM_ID.equals(stack.getItemId())) {
                return true;
            }
        }
        return false;
    }
}
