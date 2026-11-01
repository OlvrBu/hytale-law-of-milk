package io.github.olvrbu.lawofmilk.superapple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConeTest {

    // Point tip at the origin, looking along +X, 10 blocks long, 30 degree opening angle.
    private final Cone cone = new Cone(0, 0, 0, 0, 1, 0, 0, 10, 15);

    @Test
    void pointsOnTheAxisUpToTheLength() {
        assertTrue(cone.contains(0, 0, 0));
        assertTrue(cone.contains(5, 0, 0));
        assertTrue(cone.contains(10, 0, 0));
        assertFalse(cone.contains(10.01, 0, 0));
    }

    @Test
    void nothingBehindTheApex() {
        assertFalse(cone.contains(-1, 0, 0));
    }

    @Test
    void edgeAtHalfTheOpeningAngle() {
        double tan14 = Math.tan(Math.toRadians(14));
        double tan16 = Math.tan(Math.toRadians(16));
        assertTrue(cone.contains(5, 5 * tan14, 0));
        assertFalse(cone.contains(5, 5 * tan16, 0));
        assertTrue(cone.contains(5, 0, -5 * tan14));
    }

    @Test
    void farEndIsRounded() {
        // 9.8 along the axis and 2.1 to the side is inside the angle, but more than 10 from the apex.
        assertFalse(cone.contains(9.8, 2.1, 0));
        assertTrue(cone.contains(9.7, 2.0, 0));
    }

    @Test
    void directionNeedNotBeNormalizedAndCanPointAnywhere() {
        Cone down = new Cone(0.5, 64, 0.5, 0, 0, -3, 0, 10, 15);
        assertTrue(down.containsBlock(0, 60, 0));   // center (0.5, 60.5, 0.5), straight below
        assertFalse(down.containsBlock(3, 60, 0));  // 3 blocks to the side at 3.5 down: about 41 degrees
        assertFalse(down.containsBlock(0, 65, 0));  // above the tip
    }

    @Test
    void tallTipCoversThePlayersHeight() {
        // Tip from y 64 to 66 (a standing player), looking along +X, 20 degrees to each side.
        Cone tall = new Cone(0.5, 64, 0.5, 2, 1, 0, 0, 10, 20);
        // Right in front of the player: both the leg block and the head block.
        assertTrue(tall.containsBlock(1, 64, 0));
        assertTrue(tall.containsBlock(1, 65, 0));
        // Not the floor or the ceiling right in front, which are too steep from every tip point.
        assertFalse(tall.containsBlock(1, 63, 0));
        assertFalse(tall.containsBlock(1, 66, 0));
        // Further out it widens: 5 blocks ahead, one block above the head is within 20 degrees of the top.
        assertTrue(tall.containsBlock(5, 66, 0));
    }

    @Test
    void tallTipIsTallerThanAPointTip() {
        Cone point = new Cone(0.5, 65, 0.5, 0, 1, 0, 0, 10, 20);
        Cone tall = new Cone(0.5, 64, 0.5, 2, 1, 0, 0, 10, 20);
        // 1 ahead at foot level (center y 64.5): about 27 degrees below a point tip at y 65, but level with part of the tall tip.
        assertFalse(point.containsBlock(1, 64, 0));
        assertTrue(tall.containsBlock(1, 64, 0));
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Cone(0, 0, 0, 0, 0, 0, 0, 10, 15));
        assertThrows(IllegalArgumentException.class, () -> new Cone(0, 0, 0, -1, 1, 0, 0, 10, 15));
    }
}
