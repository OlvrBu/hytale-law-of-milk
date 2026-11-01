package io.github.olvrbu.lawofmilk.superapple;

/**
 * Game-independent geometry of the super apple's area: a cone pointing along the view direction
 * whose tip is a vertical line (the player's height) instead of a single point. A point is inside
 * if, seen from some point of the tip, it is at most {@code length} away and at most
 * {@code halfAngle} off the axis. So the shape is as tall as the tip at the player, widens with
 * the angle, and its far end is rounded.
 */
final class Cone {

    /** The tip is tested every this many blocks along its height. */
    private static final double TIP_STEP = 0.1;

    private final double tipX, tipBottomY, tipZ;
    private final double tipHeight;
    private final int tipSteps;
    private final double dirX, dirY, dirZ;
    private final double length;
    private final double cosHalfAngle;

    /**
     * The tip runs from {@code (tipX, tipBottomY, tipZ)} straight up by {@code tipHeight} (0 for a
     * point). {@code dir} need not be normalized but must not be zero. {@code halfAngleDegrees} is
     * half the opening angle.
     */
    Cone(double tipX, double tipBottomY, double tipZ, double tipHeight,
         double dirX, double dirY, double dirZ, double length, double halfAngleDegrees) {
        double norm = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (norm == 0) {
            throw new IllegalArgumentException("Cone direction must not be zero");
        }
        if (tipHeight < 0) {
            throw new IllegalArgumentException("Tip height must not be negative");
        }
        this.tipX = tipX;
        this.tipBottomY = tipBottomY;
        this.tipZ = tipZ;
        this.tipHeight = tipHeight;
        this.tipSteps = (int) Math.ceil(tipHeight / TIP_STEP);
        this.dirX = dirX / norm;
        this.dirY = dirY / norm;
        this.dirZ = dirZ / norm;
        this.length = length;
        this.cosHalfAngle = Math.cos(Math.toRadians(halfAngleDegrees));
    }

    /** Whether the point is inside the cone. Points on the tip count as inside. */
    boolean contains(double x, double y, double z) {
        for (int i = 0; i <= tipSteps; i++) {
            double apexY = tipSteps == 0 ? tipBottomY : tipBottomY + tipHeight * i / tipSteps;
            if (containsFrom(tipX, apexY, tipZ, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the block at (x, y, z) has its center inside the cone. */
    boolean containsBlock(int x, int y, int z) {
        return contains(x + 0.5, y + 0.5, z + 0.5);
    }

    /** The single-point cone test with its apex at (ax, ay, az). */
    private boolean containsFrom(double ax, double ay, double az, double x, double y, double z) {
        double vx = x - ax;
        double vy = y - ay;
        double vz = z - az;
        double distSquared = vx * vx + vy * vy + vz * vz;
        if (distSquared > length * length) {
            return false;
        }
        // Angle test without acos: v·dir >= |v| cos(halfAngle).
        double along = vx * dirX + vy * dirY + vz * dirZ;
        return along >= Math.sqrt(distSquared) * cosHalfAngle;
    }

    // Bounding box of the shape, for the block loop.

    double minX() {
        return tipX - length;
    }

    double maxX() {
        return tipX + length;
    }

    double minY() {
        return tipBottomY - length;
    }

    double maxY() {
        return tipBottomY + tipHeight + length;
    }

    double minZ() {
        return tipZ - length;
    }

    double maxZ() {
        return tipZ + length;
    }

    double length() {
        return length;
    }
}
