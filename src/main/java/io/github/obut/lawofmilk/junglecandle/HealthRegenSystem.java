package io.github.obut.lawofmilk.junglecandle;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Heals {@link JungleCandle#REGEN_AMOUNT} health every {@link JungleCandle#REGEN_INTERVAL} seconds
 * while the candle is in the off-hand and the player is alive and hurt.
 */
public class HealthRegenSystem extends EntityTickingSystem<EntityStore> {

    // Time towards the next heal, per player. Weak keys: entries vanish with the player's Ref.
    // Synchronized because each world ticks on its own thread.
    private final Map<Ref<EntityStore>, RegenTimer> timers = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        // Dead players keep their entity until they respawn; same check as vanilla stat regen.
        boolean dead = archetypeChunk.getArchetype().contains(DeathComponent.getComponentType());
        if (dead || !JungleCandle.isInOffhand(archetypeChunk.getComponent(index, InventoryComponent.Utility.getComponentType()))) {
            timers.remove(ref);
            return;
        }
        EntityStatMap stats = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = stats.get(healthIndex);
        if (health == null || health.get() >= health.getMax()) {
            // At full health the timer starts over, so the first heal comes a full interval after a hit.
            timers.remove(ref);
            return;
        }
        int heals = timers.computeIfAbsent(ref, r -> new RegenTimer()).advance(dt, JungleCandle.REGEN_INTERVAL);
        if (heals > 0) {
            stats.addStatValue(healthIndex, heals * JungleCandle.REGEN_AMOUNT);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), EntityStatMap.getComponentType(), InventoryComponent.Utility.getComponentType());
    }
}
