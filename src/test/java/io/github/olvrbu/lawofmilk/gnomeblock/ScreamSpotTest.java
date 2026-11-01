package io.github.olvrbu.lawofmilk.gnomeblock;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreamSpotTest {

    // Flat ground: everything below y = 64 is solid.
    private static final ScreamSpot.Solidity FLAT = (x, y, z) -> y < 64;

    @Test
    void weightIsLowestInFrontAndHighestBehind() {
        assertEquals(1.0 / 3.0, ScreamSpot.viewWeight(1, 0, 5, 0), 1e-9);
        assertEquals(2.0 / 3.0, ScreamSpot.viewWeight(1, 0, 0, 5), 1e-9);
        assertEquals(1.0, ScreamSpot.viewWeight(1, 0, -5, 0), 1e-9);
    }

    @Test
    void weightIgnoresLengths() {
        assertEquals(ScreamSpot.viewWeight(1, 0, 3, 3), ScreamSpot.viewWeight(0.2, 0, 7, 7), 1e-9);
    }

    @Test
    void noPreferenceWhenLookingStraightUpOrDown() {
        assertEquals(1.0, ScreamSpot.viewWeight(0, 0, 5, 0));
    }

    @Test
    void offsetsStayInTheRing() {
        Random random = new Random(1);
        for (int i = 0; i < 10_000; i++) {
            double[] offset = ScreamSpot.randomOffset(random, 1, 0);
            double distance = Math.hypot(offset[0], offset[1]);
            assertTrue(distance >= ScreamSpot.MIN_DISTANCE - 1e-9 && distance <= ScreamSpot.MAX_DISTANCE + 1e-9);
        }
    }

    @Test
    void offsetsAreBiasedBehindButStillReachTheFront() {
        // Looking along +x, "in front" is dx > 0. Expected share in front: (2 - 2/pi) / 4 = 0.34.
        Random random = new Random(2);
        int inFront = 0, n = 30_000;
        for (int i = 0; i < n; i++) {
            if (ScreamSpot.randomOffset(random, 1, 0)[0] > 0) {
                inFront++;
            }
        }
        double share = (double) inFront / n;
        assertTrue(share > 0.25 && share < 0.40, "share in front: " + share);
    }

    @Test
    void groundFoundAtFeetLevel() {
        assertEquals(OptionalInt.of(64), ScreamSpot.groundY(FLAT, 0, 0, 64));
    }

    @Test
    void groundFoundUpAndDownWithinTheSearch() {
        assertEquals(OptionalInt.of(64), ScreamSpot.groundY(FLAT, 0, 0, 60));
        assertEquals(OptionalInt.of(64), ScreamSpot.groundY(FLAT, 0, 0, 68));
        assertEquals(OptionalInt.empty(), ScreamSpot.groundY(FLAT, 0, 0, 69));
    }

    @Test
    void spotIsOnTheGroundAroundThePlayer() {
        double[] spot = ScreamSpot.find(FLAT, new Random(3), 100.3, 64.0, -20.7, 0, 1);
        assertNotNull(spot);
        assertEquals(64.5, spot[1]);
        double distance = Math.hypot(spot[0] - 100.3, spot[2] + 20.7);
        // Block centers can be up to half a diagonal off the sampled point.
        assertTrue(distance >= ScreamSpot.MIN_DISTANCE - 0.75 && distance <= ScreamSpot.MAX_DISTANCE + 0.75);
    }

    @Test
    void noSpotWithoutGround() {
        assertNull(ScreamSpot.find((x, y, z) -> false, new Random(4), 0, 64, 0, 1, 0));
    }
}
