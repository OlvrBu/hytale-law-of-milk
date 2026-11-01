package io.github.obut.lawofmilk.growingchest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrowthTimerTest {

    @Test
    void nothingBeforeAFullInterval() {
        assertEquals(0, new GrowthTimer().advance(1.9f, 2f));
    }

    @Test
    void oneStepPerIntervalAtServerTickRate() {
        GrowthTimer timer = new GrowthTimer();
        int steps = 0;
        // 21 s at 30 TPS (the extra second absorbs float rounding of 1/30).
        for (int tick = 0; tick < 30 * 21; tick++) {
            steps += timer.advance(1f / 30f, 2f);
        }
        assertEquals(10, steps);
    }

    @Test
    void catchesUpAfterALongTick() {
        assertEquals(3, new GrowthTimer().advance(6.5f, 2f));
    }
}
