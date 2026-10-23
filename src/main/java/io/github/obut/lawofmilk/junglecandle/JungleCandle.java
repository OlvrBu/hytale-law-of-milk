package io.github.obut.lawofmilk.junglecandle;

import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nullable;

/**
 * The jungle candle: while the vanilla {@code Furniture_Jungle_Candle} sits in any off-hand slot
 * (active or not), stamina costs are cut by {@link #STAMINA_REDUCTION}. Nothing shows this in game.
 * The vanilla item is unchanged.
 */
public final class JungleCandle {

    static final String ITEM_ID = "Furniture_Jungle_Candle";
    /** Share of every stamina cost that is given back. */
    static final float STAMINA_REDUCTION = 0.5f;

    private JungleCandle() {
    }

    public static void register(JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new StaminaSaverSystem(plugin.getLogger()));
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
