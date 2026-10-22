package io.github.obut.lawofmilk.superhatchet;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * While the super hatchet is active, every hit does at least {@link SuperHatchet#DAMAGE}, whatever
 * the material, and the hit's durability loss is undone.
 *
 * <p>Limits: the server only fires {@link DamageBlockEvent} if the tool has a gather spec for the
 * block's gather type. The Iron Hatchet has one for nearly everything, but not for GoblinMetal,
 * SoftWoods and Pickaxe_Tier0 (9 vanilla blocks), and never for Unbreakable; those stay unminable.
 */
class HatchetDamageSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    HatchetDamageSystem() {
        super(DamageBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DamageBlockEvent event) {
        Ref<EntityStore> player = archetypeChunk.getReferenceTo(index);
        if (event.isCancelled() || !SuperHatchet.isActive(event.getItemInHand(), store, player)) {
            return;
        }
        event.setDamage(Math.max(event.getDamage(), SuperHatchet.DAMAGE));
        keepDurability(store, player);
    }

    /**
     * The server deducts durability right after this event, with no hook to prevent it, so remember
     * the held hatchet's durability now and restore it in a task that runs after the hit.
     */
    private static void keepDurability(Store<EntityStore> store, Ref<EntityStore> player) {
        InventoryComponent.Hotbar hotbar = store.getComponent(player, InventoryComponent.Hotbar.getComponentType());
        if (hotbar == null || hotbar.getActiveSlot() == InventoryComponent.INACTIVE_SLOT_INDEX) {
            return;
        }
        byte slot = hotbar.getActiveSlot();
        ItemStack before = hotbar.getActiveItem();
        if (ItemStack.isEmpty(before) || !SuperHatchet.TOOL_ID.equals(before.getItemId())) {
            return;
        }
        double durability = before.getDurability();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!player.isValid()) {
                return;
            }
            Store<EntityStore> entityStore = world.getEntityStore().getStore();
            InventoryComponent.Hotbar nowHotbar = entityStore.getComponent(player, InventoryComponent.Hotbar.getComponentType());
            if (nowHotbar == null) {
                return;
            }
            ItemStack now = nowHotbar.getInventory().getItemStack(slot);
            if (ItemStack.isEmpty(now) || !SuperHatchet.TOOL_ID.equals(now.getItemId()) || now.getDurability() >= durability) {
                return;
            }
            ItemUtils.updateItemStackDurability(player, now, nowHotbar.getInventory(), slot,
                    durability - now.getDurability(), entityStore);
        });
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
