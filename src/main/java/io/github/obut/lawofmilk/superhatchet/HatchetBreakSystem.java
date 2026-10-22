package io.github.obut.lawofmilk.superhatchet;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.SoftBlockDropType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * When a player breaks a block with the active super hatchet, also breaks every block of the same
 * type within {@link SuperHatchet#radius} blocks (a sphere; the blocks don't have to be connected),
 * with normal drops if {@link SuperHatchet#areaDrops} is on, otherwise none. Logs how many blocks
 * it broke and how long that took.
 */
class HatchetBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    /** The world's build height; the server's own block-break helpers use the same bounds. */
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 320;

    private final HytaleLogger logger;

    HatchetBreakSystem(HytaleLogger logger) {
        super(BreakBlockEvent.class);
        this.logger = logger;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
        Ref<EntityStore> player = archetypeChunk.getReferenceTo(index);
        // The area's own breaks fire this event again, but with no item in hand, so they stop here.
        if (event.isCancelled() || !SuperHatchet.isActive(event.getItemInHand(), store, player)) {
            return;
        }
        Vector3i center = new Vector3i(event.getTargetBlock());
        BlockType blockType = event.getBlockType();
        int radius = SuperHatchet.radius;
        boolean dropItems = SuperHatchet.areaDrops;
        World world = store.getExternalData().getWorld();
        // This event fires before the target block is removed. Break the rest in a separate task on
        // the world thread, after the server has finished breaking the target.
        world.execute(() -> breakArea(world, player, center, blockType, radius, dropItems));
    }

    private void breakArea(World world, Ref<EntityStore> player, Vector3i center, BlockType blockType, int radius,
                           boolean dropItems) {
        if (!player.isValid()) {
            return;
        }
        long start = System.nanoTime();
        int blockId = BlockType.getAssetMap().getIndex(blockType.getId());
        Drops drops = Drops.of(blockType);
        int settings = dropItems ? 0 : SetBlockSettings.NO_DROP_ITEMS;
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        int checked = 0;
        int broken = 0;
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int y = center.y + dy;
                if (y < MIN_Y || y >= MAX_Y) {
                    continue;
                }
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }
                    int x = center.x + dx;
                    int z = center.z + dz;
                    checked++;
                    Ref<ChunkStore> section = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
                    if (section == null || !section.isValid()) {
                        continue; // chunk not loaded
                    }
                    BlockSection blocks = chunkStore.getComponent(section, BlockSection.getComponentType());
                    // Skip filler cells of multi-block blocks; their origin cell is checked on its own.
                    if (blocks == null || blocks.get(x, y, z) != blockId || blocks.getFiller(x, y, z) != 0) {
                        continue;
                    }
                    // No item in hand: no durability loss, and the BreakBlockEvent this fires is ignored above.
                    BlockHarvestUtils.performBlockBreak(new Vector3i(x, y, z), blockType, null,
                            drops.quantity, drops.itemId, drops.dropListId, settings, player, section, entityStore, chunkStore);
                    broken++;
                }
            }
        }
        long micros = (System.nanoTime() - start) / 1000;
        logger.at(Level.INFO).log("Super hatchet: broke %d x %s within radius %d (%d positions checked, drops %s) in %.1f ms",
                broken, blockType.getId(), radius, checked, dropItems ? "on" : "off", micros / 1000.0);
    }

    /** What a block drops when broken by a tool, as the server's own mining code reads it. */
    private record Drops(int quantity, String itemId, String dropListId) {
        static Drops of(BlockType blockType) {
            BlockGathering gathering = blockType.getGathering();
            if (gathering != null && gathering.isSoft()) {
                SoftBlockDropType soft = gathering.getSoft();
                return new Drops(1, soft.getItemId(), soft.getDropListId());
            }
            BlockBreakingDropType breaking = gathering != null ? gathering.getBreaking() : null;
            if (breaking == null) {
                return new Drops(1, null, null);
            }
            return new Drops(breaking.getQuantity(), breaking.getItemId(), breaking.getDropListId());
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
