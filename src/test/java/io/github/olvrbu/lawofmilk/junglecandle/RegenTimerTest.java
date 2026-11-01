package io.github.olvrbu.lawofmilk.junglecandle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegenTimerTest {

    @Test
    void nothingBeforeAFullInterval() {
        RegenTimer timer = new RegenTimer();
        assertEquals(0, timer.advance(4.9f, 5f));
    }

    @Test
    void oneHealPerIntervalAtServerTickRate() {
        RegenTimer timer = new RegenTimer();
        int heals = 0;
        // 61 s at 30 TPS (the extra second absorbs float rounding of 1/30).
        for (int tick = 0; tick < 30 * 61; tick++) {
            heals += timer.advance(1f / 30f, 5f);
        }
        assertEquals(12, heals);
    }

    @Test
    void keepsTheRemainder() {
        RegenTimer timer = new RegenTimer();
        assertEquals(1, timer.advance(7f, 5f));
        assertEquals(1, timer.advance(3f, 5f));
    }

    @Test
    void catchesUpAfterALongTick() {
        assertEquals(2, new RegenTimer().advance(11f, 5f));
    }
}
