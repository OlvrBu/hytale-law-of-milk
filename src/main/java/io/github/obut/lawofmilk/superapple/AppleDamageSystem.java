package io.github.obut.lawofmilk.superapple;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * While the super apple is active, every hit does at least {@link SuperApple#DAMAGE}, whatever the
 * material.
 *
 * <p>Limits: the server only fires {@link DamageBlockEvent} if the bare-hand gather specs cover the
 * block's gather type with enough quality. They have no quality, and no spec for GoblinMetal,
 * SoftWoods and Pickaxe_Tier0, so quality-gated blocks (e.g. Rock_Basalt), those gather types and
 * Unbreakable blocks stay unminable.
 */
class AppleDamageSystem extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    AppleDamageSystem() {
        super(DamageBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DamageBlockEvent event) {
        Ref<EntityStore> player = archetypeChunk.getReferenceTo(index);
        if (event.isCancelled() || !SuperApple.isActive(event.getItemInHand(), store, player)) {
            return;
        }
        event.setDamage(Math.max(event.getDamage(), SuperApple.DAMAGE));
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
