package io.github.obut.lawofmilk.superhatchet;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.heightmap.HeightmapColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * When a player breaks a block with the active super hatchet, also removes every block of the
 * same type within {@link SuperHatchet#radius} blocks (a sphere; the blocks don't have to be
 * connected). Logs how many blocks it removed and how long that took.
 *
 * <p>The area's blocks are set straight to air with {@link BlockOperations#setBlock}, not broken
 * like mined blocks: no BreakBlockEvent (other plugins can't veto it), no drops, sounds, particles,
 * physics or neighbor updates, no on-break interactions. Heightmap, lighting, block entities and
 * multi-block filler cells are still updated, and players receive the changes as usual.
 */
class HatchetBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    /** The world's build height; the server's own block-break helpers use the same bounds. */
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 320;
    private static final int SETTINGS = SetBlockSettings.NO_SEND_PARTICLES;

    private final HytaleLogger logger;

    HatchetBreakSystem(HytaleLogger logger) {
        super(BreakBlockEvent.class);
        this.logger = logger;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
        Ref<EntityStore> player = archetypeChunk.getReferenceTo(index);
        if (event.isCancelled() || !SuperHatchet.isActive(event.getItemInHand(), store, player)) {
            return;
        }
        Vector3i center = new Vector3i(event.getTargetBlock());
        BlockType blockType = event.getBlockType();
        int radius = SuperHatchet.radius;
        World world = store.getExternalData().getWorld();
        // This event fires before the target block is removed. Remove the rest in a separate task on
        // the world thread, after the server has finished breaking the target.
        world.execute(() -> clearArea(world, center, blockType, radius));
    }

    private void clearArea(World world, Vector3i center, BlockType blockType, int radius) {
        long start = System.nanoTime();
        int blockId = BlockType.getAssetMap().getIndex(blockType.getId());
        ChunkStore chunks = world.getChunkStore();
        Store<ChunkStore> chunkStore = chunks.getStore();
        SectionCache cache = new SectionCache(chunks, chunkStore);
        int checked = 0;
        int removed = 0;
        int radiusSquared = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int y = center.y + dy;
                if (y < MIN_Y || y >= MAX_Y) {
                    continue;
                }
                // z innermost: consecutive positions are almost always in the same section.
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }
                    int x = center.x + dx;
                    int z = center.z + dz;
                    checked++;
                    if (!cache.moveTo(x, y, z)) {
                        continue; // chunk not loaded
                    }
                    BlockSection blocks = cache.blockSection;
                    // Skip filler cells of multi-block blocks: setBlock on the origin cell removes them.
                    if (blocks.get(x, y, z) != blockId || blocks.getFiller(x, y, z) != 0) {
                        continue;
                    }
                    BlockOperations.setBlock(chunks, cache.ref, cache.section, blocks, cache.blockComponents, cache.heightmap,
                            x, y, z, 0, BlockType.EMPTY, 0, 0, SETTINGS);
                    removed++;
                }
            }
        }
        long micros = (System.nanoTime() - start) / 1000;
        logger.at(Level.INFO).log("Super hatchet: removed %d x %s within radius %d (%d positions checked) in %.1f ms",
                removed, blockType.getId(), radius, checked, micros / 1000.0);
    }

    /**
     * The chunk section components for the position last moved to, looked up again only when a
     * position lies in a different section (sections are 32x32x32 blocks).
     */
    private static final class SectionCache {
        private final ChunkStore chunks;
        private final Store<ChunkStore> store;
        private long key = Long.MIN_VALUE;
        private boolean loaded;

        Ref<ChunkStore> ref;
        ChunkSection section;
        BlockSection blockSection;
        BlockComponentSection blockComponents;
        HeightmapColumn heightmap;

        SectionCache(ChunkStore chunks, Store<ChunkStore> store) {
            this.chunks = chunks;
            this.store = store;
        }

        /** Returns whether the section containing (x, y, z) is loaded. */
        boolean moveTo(int x, int y, int z) {
            long newKey = ((long) (x >> 5) & 0x1FFFFF) << 42 | ((long) (y >> 5) & 0x1FFFFF) << 21 | ((long) (z >> 5) & 0x1FFFFF);
            if (newKey == key) {
                return loaded;
            }
            key = newKey;
            loaded = false;
            ref = chunks.getChunkSectionReferenceAtBlock(x, y, z);
            if (ref == null || !ref.isValid()) {
                return false;
            }
            section = store.getComponent(ref, ChunkSection.getComponentType());
            blockSection = store.getComponent(ref, BlockSection.getComponentType());
            if (section == null || blockSection == null) {
                return false;
            }
            blockComponents = store.getComponent(ref, BlockComponentSection.getComponentType());
            heightmap = store.getComponent(section.getChunkColumnReference(), HeightmapColumn.getComponentType());
            loaded = heightmap != null;
            return loaded;
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
