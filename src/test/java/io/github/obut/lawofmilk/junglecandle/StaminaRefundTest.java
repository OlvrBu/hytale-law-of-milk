package io.github.obut.lawofmilk.junglecandle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaminaRefundTest {

    @Test
    void givesBackHalfOfADrop() {
        assertEquals(1.0f, StaminaRefund.amount(10f, 8f, 0.5f), 1e-6);
    }

    @Test
    void worksIntoOverdrawnStamina() {
        assertEquals(1.5f, StaminaRefund.amount(1f, -2f, 0.5f), 1e-6);
    }

    @Test
    void nothingWhenStaminaRoseOrStayed() {
        assertEquals(0f, StaminaRefund.amount(5f, 5.3f, 0.5f));
        assertEquals(0f, StaminaRefund.amount(5f, 5f, 0.5f));
    }
}
