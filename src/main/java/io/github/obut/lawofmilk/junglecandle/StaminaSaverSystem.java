package io.github.obut.lawofmilk.junglecandle;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Every tick, gives back part of any stamina the player lost since the last tick while the candle
 * is in the off-hand.
 *
 * <p>Stamina is spent in many unrelated places (sprint/glide drain in {@code Stamina.json}, attack
 * and dodge interactions, shield blocking in {@code DamageSystems.DamageStamina}), and none of them
 * can be scaled by an off-hand item. Watching the stat's value catches all of them.
 */
public class StaminaSaverSystem extends EntityTickingSystem<EntityStore> {

    // Stamina after our last tick, per player. Weak keys: entries vanish with the player's Ref.
    // Synchronized because each world ticks on its own thread.
    private final Map<Ref<EntityStore>, Float> lastStamina = Collections.synchronizedMap(new WeakHashMap<>());
    private final HytaleLogger logger;

    public StaminaSaverSystem(HytaleLogger logger) {
        this.logger = logger;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (!JungleCandle.isInOffhand(archetypeChunk.getComponent(index, InventoryComponent.Utility.getComponentType()))) {
            if (lastStamina.remove(ref) != null) {
                logger.atInfo().log("[JungleCandle] Candle left the off-hand of %s", playerName(archetypeChunk, index));
            }
            return;
        }
        EntityStatMap stats = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        int staminaIndex = DefaultEntityStatTypes.getStamina();
        EntityStatValue stamina = stats.get(staminaIndex);
        if (stamina == null) {
            return;
        }
        float current = stamina.get();
        Float previous = lastStamina.get(ref);
        if (previous == null) {
            logger.atInfo().log("[JungleCandle] Candle in the off-hand of %s, saving stamina", playerName(archetypeChunk, index));
        } else {
            float refund = StaminaRefund.amount(previous, current, JungleCandle.STAMINA_REDUCTION);
            if (refund > 0) {
                current = stats.addStatValue(staminaIndex, refund);
            }
        }
        lastStamina.put(ref, current);
    }

    private static String playerName(ArchetypeChunk<EntityStore> archetypeChunk, int index) {
        return archetypeChunk.getComponent(index, PlayerRef.getComponentType()).getUsername();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), PlayerRef.getComponentType(), EntityStatMap.getComponentType(), InventoryComponent.Utility.getComponentType());
    }
}
