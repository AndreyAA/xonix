package com.ali.dev.xonix.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTest {

    @Test
    void inFieldItemBouncesAtTopBorderWithoutAccessingGridOutOfBounds() {
        State state = ModelTestSupport.state(ModelTestSupport.level(50.0, List.of(), 10, List.of()));
        state.initData();
        state.head.pos = new XY(50, 50);

        Item item = new Item(new XY(5, 0), XY.TOP, ItemType.STD, ItemAreaType.InField, 1.0);
        item.currentY = -0.6;

        assertDoesNotThrow(() -> item.move(state));

        assertEquals(1, item.getShift().getY());
        assertTrue(item.getCurrentY() > -0.6);
    }
}
