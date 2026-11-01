package io.github.olvrbu.lawofmilk.growingchest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Game-independent: which item IDs grow in one step. */
final class UniqueItems {

    private UniqueItems() {
    }

    /**
     * The distinct item IDs of the slots, in the order they first appear. {@code null} entries are
     * empty slots. Several stacks of one ID count once, so every ID grows at the same rate.
     */
    static List<String> inSlotOrder(List<String> slotItemIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String id : slotItemIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        return new ArrayList<>(ids);
    }
}
