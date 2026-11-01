package io.github.obut.lawofmilk.growingchest;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniqueItemsTest {

    @Test
    void severalStacksOfOneIdCountOnce() {
        List<String> slots = Arrays.asList("Soil_Dirt", null, "Soil_Dirt", "Rock_Stone", null);
        assertEquals(List.of("Soil_Dirt", "Rock_Stone"), UniqueItems.inSlotOrder(slots));
    }

    @Test
    void keepsSlotOrder() {
        List<String> slots = Arrays.asList(null, "Rock_Stone", "Soil_Dirt", "Rock_Stone");
        assertEquals(List.of("Rock_Stone", "Soil_Dirt"), UniqueItems.inSlotOrder(slots));
    }

    @Test
    void emptyChestGrowsNothing() {
        assertEquals(List.of(), UniqueItems.inSlotOrder(Arrays.asList(null, null)));
    }
}
