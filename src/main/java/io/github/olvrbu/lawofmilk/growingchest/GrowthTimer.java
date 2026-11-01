package io.github.olvrbu.lawofmilk.growingchest;

/** Game-independent timing for one chest: counts whole intervals as time passes. */
final class GrowthTimer {

    private float elapsed;

    /** Adds {@code dt} seconds and returns how many whole {@code interval}s are now complete. */
    int advance(float dt, float interval) {
        elapsed += dt;
        int due = (int) (elapsed / interval);
        elapsed -= due * interval;
        return due;
    }
}
