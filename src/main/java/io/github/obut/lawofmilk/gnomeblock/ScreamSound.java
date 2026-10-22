package io.github.obut.lawofmilk.gnomeblock;

import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.concurrent.ThreadLocalRandom;

/** Plays a sound at a random point around a block. */
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
        // Rejection sampling: random points in the enclosing cube until one lies inside the sphere.
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double dx, dy, dz;
        do {
            dx = random.nextDouble(-1.0, 1.0);
            dy = random.nextDouble(-1.0, 1.0);
            dz = random.nextDouble(-1.0, 1.0);
        } while (dx * dx + dy * dy + dz * dz > 1.0);
        double soundX = x + 0.5 + dx * offsetRadius;
        double soundY = y + 0.5 + dy * offsetRadius;
        double soundZ = z + 0.5 + dz * offsetRadius;
        // Finding the players who can hear the sound needs the entity store. Callers may be in the
        // middle of another store's tick (random ticks run in the chunk store), so play it as a
        // separate task on the world thread.
        world.execute(() -> SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX, soundX, soundY, soundZ,
                world.getEntityStore().getStore()));
    }
}
