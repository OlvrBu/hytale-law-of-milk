package io.github.obut.lawofmilk.growingchest;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Ticks every container block (they are entities in the chunk store, like benches) and lets the
 * ones that are a growing chest on its pedestal grow their contents.
 */
public class ChestGrowthSystem extends EntityTickingSystem<ChunkStore> {

    private final HytaleLogger logger;
    // Time towards the next growth step, per active chest. Weak keys: entries vanish with the chest's Ref.
    // Synchronized because each world ticks on its own thread.
    private final Map<Ref<ChunkStore>, GrowthTimer> timers = Collections.synchronizedMap(new WeakHashMap<>());

    public ChestGrowthSystem(HytaleLogger logger) {
        this.logger = logger;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store,
                     @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
        BlockModule.BlockStateInfo info = archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
        // The block entity only knows its section and its index inside it; same lookup as the vanilla bench systems.
        Ref<ChunkStore> sectionRef = info.getSectionRef();
        ChunkSection section = sectionRef.isValid() ? store.getComponent(sectionRef, ChunkSection.getComponentType()) : null;
        BlockSection blocks = sectionRef.isValid() ? store.getComponent(sectionRef, BlockSection.getComponentType()) : null;
        if (section == null || blocks == null) {
            return;
        }
        int blockIndex = info.getIndex();
        int x = ChunkUtil.worldCoordFromLocalCoord(section.getX(), ChunkUtil.xFromIndex(blockIndex));
        int y = ChunkUtil.worldCoordFromLocalCoord(section.getY(), ChunkUtil.yFromIndex(blockIndex));
        int z = ChunkUtil.worldCoordFromLocalCoord(section.getZ(), ChunkUtil.zFromIndex(blockIndex));

        boolean active = isBlock(blocks.get(blockIndex), GrowingChest.CHEST_ID)
                && isUnrotated(store, x, y - 1, z, GrowingChest.PIPE_ID)
                && isUnrotated(store, x, y - 2, z, GrowingChest.BASE_ID);
        if (!active) {
            if (timers.remove(ref) != null) {
                logger.atFine().log("[GrowingChest] Chest at %d %d %d stopped growing", x, y, z);
            }
            return;
        }
        GrowthTimer timer = timers.get(ref);
        if (timer == null) {
            timer = new GrowthTimer();
            timers.put(ref, timer);
            logger.atFine().log("[GrowingChest] Chest at %d %d %d is growing", x, y, z);
        }
        int steps = timer.advance(dt, GrowingChest.INTERVAL);
        ItemContainer items = archetypeChunk.getComponent(index, ItemContainerBlock.getComponentType()).getItemContainer();
        for (int i = 0; i < steps; i++) {
            grow(items);
        }
    }

    /** Adds one fresh item (no durability loss, no metadata) per unique item ID in the container. */
    private static void grow(ItemContainer items) {
        List<String> slotIds = new ArrayList<>(items.getCapacity());
        for (short slot = 0; slot < items.getCapacity(); slot++) {
            ItemStack stack = items.getItemStack(slot);
            slotIds.add(ItemStack.isEmpty(stack) ? null : stack.getItemId());
        }
        for (String id : UniqueItems.inSlotOrder(slotIds)) {
            // Items whose asset is gone (e.g. from a removed mod) can't be created.
            if (Item.getAssetMap().getAsset(id) == null) {
                continue;
            }
            // Fills a stack that has room first, else takes a free slot; does nothing if neither exists.
            // The container's change event marks the chest for saving.
            items.addItemStack(new ItemStack(id, 1));
        }
    }

    /** Whether (x, y, z) is loaded, holds {@code blockId} itself (not a filler cell) and has rotation index 0. */
    private static boolean isUnrotated(Store<ChunkStore> store, int x, int y, int z, String blockId) {
        Ref<ChunkStore> sectionRef = store.getExternalData().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return false;
        }
        BlockSection blocks = store.getComponent(sectionRef, BlockSection.getComponentType());
        return blocks != null
                && isBlock(blocks.get(x, y, z), blockId)
                && blocks.getFiller(x, y, z) == 0
                && blocks.getRotationIndex(x, y, z) == GrowingChest.REQUIRED_ROTATION;
    }

    /**
     * Whether the block is {@code blockId} in any of its states. A chest swaps to another block type
     * with its own ID while open or closing ("OpenWindow"/"CloseWindow" states); the default state
     * key is the base ID, as in vanilla {@code OpenContainerInteraction}.
     */
    private static boolean isBlock(int numericId, String blockId) {
        BlockType type = BlockType.getAssetMap().getAsset(numericId);
        return type != null && blockId.equals(Objects.requireNonNullElse(type.getDefaultStateKey(), type.getId()));
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(ItemContainerBlock.getComponentType(), BlockModule.BlockStateInfo.getComponentType());
    }
}
