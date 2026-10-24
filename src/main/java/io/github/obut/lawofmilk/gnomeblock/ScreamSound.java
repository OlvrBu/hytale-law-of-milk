package io.github.obut.lawofmilk.gnomeblock;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Plays a sound around a block, either at a random point or around a nearby player. */
final class ScreamSound {

    private ScreamSound() {
    }

    /**
     * Plays {@code soundEventId} at a uniformly random point within {@code offsetRadius} blocks of
     * the center of the block at (x, y, z). The point may be inside other blocks or underground.
     */
    static void playAround(World world, String soundEventId, int x, int y, int z, double offsetRadius) {
        int soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (soundEventIndex == Integer.MIN_VALUE) {
            return;
        }
        // Finding the players who can hear the sound needs the entity store. Callers may be in the
        // middle of another store's tick (random ticks run in the chunk store), so play it as a
        // separate task on the world thread.
        world.execute(() -> playAroundBlock(world, soundEventIndex, x, y, z, offsetRadius));
    }

    /**
     * Picks a random player within {@link ScreamSpot#PLAYER_RADIUS} blocks of the block at
     * (x, y, z) and plays {@code soundEventId} on the ground around them, preferably out of their
     * view (see {@link ScreamSpot}). Without such a player, or without ground around them, it
     * plays like {@link #playAround} instead.
     */
    static void playNearPlayer(World world, String soundEventId, int x, int y, int z, double fallbackRadius) {
        int soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (soundEventIndex == Integer.MIN_VALUE) {
            return;
        }
        // On the world thread, like playAround; reading player components needs it too.
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            double centerX = x + 0.5, centerY = y + 0.5, centerZ = z + 0.5;
            double maxSquared = ScreamSpot.PLAYER_RADIUS * ScreamSpot.PLAYER_RADIUS;
            List<Ref<EntityStore>> nearby = new ArrayList<>();
            for (PlayerRef playerRef : world.getPlayerRefs()) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) {
                    continue;
                }
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform != null && transform.getPosition().distanceSquared(centerX, centerY, centerZ) <= maxSquared) {
                    nearby.add(ref);
                }
            }
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (!nearby.isEmpty()) {
                Ref<EntityStore> listener = nearby.get(random.nextInt(nearby.size()));
                Vector3d position = store.getComponent(listener, TransformComponent.getComponentType()).getPosition();
                HeadRotation head = store.getComponent(listener, HeadRotation.getComponentType());
                Vector3d look = head == null ? new Vector3d() : head.getDirection();
                double[] spot = ScreamSpot.find((bx, by, bz) -> isSolid(world, bx, by, bz), random,
                        position.x, position.y, position.z, look.x, look.z);
                if (spot != null) {
                    SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX, spot[0], spot[1], spot[2], store);
                    return;
                }
            }
            playAroundBlock(world, soundEventIndex, x, y, z, fallbackRadius);
        });
    }

    // Must run on the world thread.
    private static void playAroundBlock(World world, int soundEventIndex, int x, int y, int z, double offsetRadius) {
        // Rejection sampling: random points in the enclosing cube until one lies inside the sphere.
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double dx, dy, dz;
        do {
            dx = random.nextDouble(-1.0, 1.0);
            dy = random.nextDouble(-1.0, 1.0);
            dz = random.nextDouble(-1.0, 1.0);
        } while (dx * dx + dy * dy + dz * dz > 1.0);
        SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX,
                x + 0.5 + dx * offsetRadius, y + 0.5 + dy * offsetRadius, z + 0.5 + dz * offsetRadius,
                world.getEntityStore().getStore());
    }

    // Same block lookup as SuperAppleInteraction. Blocks in unloaded chunks count as not solid.
    private static boolean isSolid(World world, int x, int y, int z) {
        ChunkStore chunks = world.getChunkStore();
        Ref<ChunkStore> section = chunks.getChunkSectionReferenceAtBlock(x, y, z);
        if (section == null || !section.isValid()) {
            return false;
        }
        BlockSection blocks = chunks.getStore().getComponent(section, BlockSection.getComponentType());
        if (blocks == null) {
            return false;
        }
        BlockType blockType = BlockType.getAssetMap().getAsset(blocks.get(x, y, z));
        return blockType != null && blockType.getMaterial() == BlockMaterial.Solid;
    }
}
