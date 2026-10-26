package io.github.obut.lawofmilk.hostilecrate;

/** Game-independent capture conditions of the hostile crate. */
public final class CaptureRule {

    private CaptureRule() {
    }

    /**
     * True if a creature with {@code health} out of {@code maxHealth} is at or below
     * {@code maxHealthFraction} of its maximum health. A creature without a usable maximum counts
     * as weak enough, so odd NPCs don't become uncatchable.
     */
    public static boolean isWeakEnough(float health, float maxHealth, double maxHealthFraction) {
        if (!(maxHealth > 0)) {
            return true;
        }
        return health <= maxHealth * maxHealthFraction;
    }

    /** The fraction as a whole percentage for messages, e.g. 0.25 -> "25". */
    public static String percent(double fraction) {
        return Long.toString(Math.round(fraction * 100));
    }
}
