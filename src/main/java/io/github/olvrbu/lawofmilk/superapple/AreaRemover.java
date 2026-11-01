package io.github.olvrbu.lawofmilk.superapple;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.heightmap.HeightmapColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

/**
 * Removes every block of one type within a {@link Cone} (the blocks don't have to be connected) and
 * logs how many it removed and how long that took.
 *
 * <p>The blocks are set straight to air with {@link BlockOperations#setBlock}, not broken like
 * mined blocks: no BreakBlockEvent (other plugins can't veto it), no drops, sounds, particles,
 * physics or neighbor updates, no on-break interactions. Heightmap, lighting, block entities and
 * multi-block filler cells are still updated, and players receive the changes as usual.
 */
final class AreaRemover {

    /** The world's build height; the server's own block-break helpers use the same bounds. */
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 320;
    private static final int SETTINGS = SetBlockSettings.NO_SEND_PARTICLES;

    private AreaRemover() {
    }

    /**
     * Removes every block of {@code blockType} whose center lies inside {@code cone}, plus the
     * clicked block itself (it may sit just outside the cone when clicked near its edge). Runs on
     * the world thread.
     */
    static void removeArea(World world, Vector3i clicked, BlockType blockType, Cone cone, HytaleLogger logger) {
        long start = System.nanoTime();
        int blockId = BlockType.getAssetMap().getIndex(blockType.getId());
        ChunkStore chunks = world.getChunkStore();
        Store<ChunkStore> chunkStore = chunks.getStore();
        SectionCache cache = new SectionCache(chunks, chunkStore);
        int checked = 0;
        int removed = 0;
        int minX = (int) Math.floor(cone.minX());
        int maxX = (int) Math.floor(cone.maxX());
        int minY = Math.max(MIN_Y, (int) Math.floor(cone.minY()));
        int maxY = Math.min(MAX_Y - 1, (int) Math.floor(cone.maxY()));
        int minZ = (int) Math.floor(cone.minZ());
        int maxZ = (int) Math.floor(cone.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                // z innermost: consecutive positions are almost always in the same section.
                for (int z = minZ; z <= maxZ; z++) {
                    boolean isClicked = x == clicked.x && y == clicked.y && z == clicked.z;
                    if (!isClicked && !cone.containsBlock(x, y, z)) {
                        continue;
                    }
                    checked++;
                    if (removeIfMatches(chunks, cache, x, y, z, blockId)) {
                        removed++;
                    }
                }
            }
        }
        // The clicked block lies outside the box only if the cone is shorter than the player's reach.
        boolean clickedInBox = clicked.x >= minX && clicked.x <= maxX && clicked.y >= minY && clicked.y <= maxY
                && clicked.z >= minZ && clicked.z <= maxZ;
        if (!clickedInBox) {
            checked++;
            if (removeIfMatches(chunks, cache, clicked.x, clicked.y, clicked.z, blockId)) {
                removed++;
            }
        }
        long micros = (System.nanoTime() - start) / 1000;
        logger.atFine().log("[SuperApple] Removed %d x %s in a cone of length %.0f (%d positions checked) in %.1f ms",
                removed, blockType.getId(), cone.length(), checked, micros / 1000.0);
    }

    /** Sets (x, y, z) to air if it's loaded and holds {@code blockId} itself; returns whether it did. */
    private static boolean removeIfMatches(ChunkStore chunks, SectionCache cache, int x, int y, int z, int blockId) {
        if (!cache.moveTo(x, y, z)) {
            return false; // chunk not loaded
        }
        BlockSection blocks = cache.blockSection;
        // Skip filler cells of multi-block blocks: setBlock on the origin cell removes them.
        if (blocks.get(x, y, z) != blockId || blocks.getFiller(x, y, z) != 0) {
            return false;
        }
        BlockOperations.setBlock(chunks, cache.ref, cache.section, blocks, cache.blockComponents, cache.heightmap,
                x, y, z, 0, BlockType.EMPTY, 0, 0, SETTINGS);
        return true;
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
}
