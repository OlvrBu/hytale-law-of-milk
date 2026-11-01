package io.github.olvrbu.lawofmilk.junglecandle;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Cuts fall damage by {@link JungleCandle#FALL_DAMAGE_REDUCTION} while the candle is in the off-hand.
 *
 * <p>Every hit is a {@link Damage} event that runs through the damage module's system groups
 * (gather, filter, inspect) before health is lowered. Systems in the filter group may change or
 * cancel it, like the vanilla armor reduction does.
 */
public class FallDamageReductionSystem extends DamageEventSystem {

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), InventoryComponent.Utility.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        if (damage.isCancelled() || damage.getDamageCauseIndex() != DamageCause.getAssetMap().getIndex("Fall")) {
            return;
        }
        if (JungleCandle.isInOffhand(archetypeChunk.getComponent(index, InventoryComponent.Utility.getComponentType()))) {
            damage.setAmount(damage.getAmount() * (1.0f - JungleCandle.FALL_DAMAGE_REDUCTION));
        }
    }
}
