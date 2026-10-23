package io.github.obut.lawofmilk.junglecandle;

/** Game-independent math for the stamina saving: how much of a stamina drop to give back. */
public final class StaminaRefund {

    private StaminaRefund() {
    }

    /**
     * Stamina to add back after it went from {@code previous} to {@code current}, so that only
     * {@code 1 - reduction} of the drop is kept. Returns 0 if stamina didn't drop.
     *
     * <p>Only the net change is visible, so regeneration in the same tick hides a little of the cost.
     */
    public static float amount(float previous, float current, float reduction) {
        float drop = previous - current;
        return drop > 0 ? drop * reduction : 0;
    }
}
