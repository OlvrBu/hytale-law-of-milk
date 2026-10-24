package io.github.obut.lawofmilk.gnomeblock;

import java.util.OptionalInt;
import java.util.random.RandomGenerator;

/**
 * Picks where a random scream plays around a listening player: on solid ground, between
 * {@link #MIN_DISTANCE} and {@link #MAX_DISTANCE} blocks away horizontally, and more likely behind
 * the player than in front of them. Game-independent so it can be unit-tested.
 */
final class ScreamSpot {

    /** Players within this many blocks of the plushie can be picked as the listener. */
    static final double PLAYER_RADIUS = 15.0;
    static final double MIN_DISTANCE = 5.0;
    static final double MAX_DISTANCE = 16.0;
    /** How many blocks above and below the player's feet to look for ground. */
    static final int GROUND_SEARCH = 4;
    /** Spots to try before giving up (e.g. a player over a cliff or in a cave). */
    static final int MAX_TRIES = 20;

    /** Tells whether the block at (x, y, z) is solid. */
    @FunctionalInterface
    interface Solidity {
        boolean isSolid(int x, int y, int z);
    }

    private ScreamSpot() {
    }

    /**
     * Relative likelihood of a spot at horizontal offset (dx, dz) from a player looking along
     * (lookX, lookZ): 1/3 straight ahead, 2/3 to the side, 1 directly behind, i.e. (2 - cos θ) / 3.
     * Without a horizontal look direction (looking straight up or down) every spot counts the same.
     */
    static double viewWeight(double lookX, double lookZ, double dx, double dz) {
        double lookLength = Math.hypot(lookX, lookZ);
        double offsetLength = Math.hypot(dx, dz);
        if (lookLength < 1e-9 || offsetLength < 1e-9) {
            return 1.0;
        }
        double cos = (lookX * dx + lookZ * dz) / (lookLength * offsetLength);
        return (2.0 - cos) / 3.0;
    }

    /**
     * A random horizontal offset {dx, dz}, uniform over the ring between MIN_DISTANCE and
     * MAX_DISTANCE, then kept with probability {@link #viewWeight} (rejection sampling; the
     * weight is at least 1/3, so this takes 3 tries at most on average).
     */
    static double[] randomOffset(RandomGenerator random, double lookX, double lookZ) {
        double minSquared = MIN_DISTANCE * MIN_DISTANCE;
        double maxSquared = MAX_DISTANCE * MAX_DISTANCE;
        while (true) {
            // sqrt makes the points uniform over the ring's area instead of bunching up inside.
            double distance = Math.sqrt(random.nextDouble(minSquared, maxSquared));
            double angle = random.nextDouble(0.0, 2.0 * Math.PI);
            double dx = distance * Math.cos(angle);
            double dz = distance * Math.sin(angle);
            if (random.nextDouble() < viewWeight(lookX, lookZ, dx, dz)) {
                return new double[]{dx, dz};
            }
        }
    }

    /**
     * The lowest y of a non-solid block standing on a solid one in column (x, z), searching
     * outwards from {@code feetY} (feetY, feetY+1, feetY-1, ...) up to GROUND_SEARCH blocks.
     */
    static OptionalInt groundY(Solidity world, int x, int z, int feetY) {
        for (int i = 0; i <= GROUND_SEARCH; i++) {
            for (int y : new int[]{feetY + i, feetY - i}) {
                if (world.isSolid(x, y - 1, z) && !world.isSolid(x, y, z)) {
                    return OptionalInt.of(y);
                }
            }
        }
        return OptionalInt.empty();
    }

    /**
     * A sound position {x, y, z} on the ground around a player at (playerX, playerY, playerZ)
     * looking along (lookX, lookZ), or null if no ground was found in MAX_TRIES tries.
     */
    static double[] find(Solidity world, RandomGenerator random, double playerX, double playerY, double playerZ,
                         double lookX, double lookZ) {
        int feetY = (int) Math.floor(playerY);
        for (int i = 0; i < MAX_TRIES; i++) {
            double[] offset = randomOffset(random, lookX, lookZ);
            int x = (int) Math.floor(playerX + offset[0]);
            int z = (int) Math.floor(playerZ + offset[1]);
            OptionalInt y = groundY(world, x, z, feetY);
            if (y.isPresent()) {
                return new double[]{x + 0.5, y.getAsInt() + 0.5, z + 0.5};
            }
        }
        return null;
    }
}
