package io.github.olvrbu.lawofmilk.hostilecrate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureRuleTest {

    @Test
    void weakEnoughAtOrBelowTheFraction() {
        assertTrue(CaptureRule.isWeakEnough(25f, 100f, 0.25));
        assertTrue(CaptureRule.isWeakEnough(1f, 100f, 0.25));
        assertTrue(CaptureRule.isWeakEnough(0f, 100f, 0.25));
    }

    @Test
    void tooHealthyAboveTheFraction() {
        assertFalse(CaptureRule.isWeakEnough(25.5f, 100f, 0.25));
        assertFalse(CaptureRule.isWeakEnough(100f, 100f, 0.25));
    }

    @Test
    void fullFractionAllowsAnyHealth() {
        assertTrue(CaptureRule.isWeakEnough(100f, 100f, 1.0));
    }

    @Test
    void missingMaximumCountsAsWeak() {
        assertTrue(CaptureRule.isWeakEnough(10f, 0f, 0.25));
        assertTrue(CaptureRule.isWeakEnough(10f, Float.NaN, 0.25));
    }

    @Test
    void percentRounds() {
        assertEquals("25", CaptureRule.percent(0.25));
        assertEquals("33", CaptureRule.percent(1.0 / 3));
    }
}
