package com.ali.dev.xonix.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlgoTest {

    @Test
    void updateStateFillsOnlyRegionWithoutEnemies() {
        State state = ModelTestSupport.state(ModelTestSupport.level(50.0, List.of(), 10, List.of()));
        state.initData();

        for (int y = 2; y <= 77; y++) {
            state.entityGrid[y][10] = EntityType.BLOCK;
            state.head.curPath.add(new XY(10, y));
        }

        state.items.clear();
        state.items.add(new Item(new XY(20, 20), XY.RIGHT, ItemType.STD, ItemAreaType.InField, 1.0));

        Algo algo = new Algo(state);
        algo.updateState();

        assertEquals(EntityType.BLOCK, state.entityGrid[20][5]);
        assertEquals(EntityType.FREE, state.entityGrid[20][20]);
    }
}
