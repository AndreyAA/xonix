package com.ali.dev.xonix.model;

import java.util.ArrayList;
import java.util.List;

final class ModelTestSupport {

    private ModelTestSupport() {
    }

    static Level level(double target, List<String> availableBonuses, int bonusSpawnSec, List<Level.ItemModel> items) {
        Level level = new Level();
        level.id = 1;
        level.target = target;
        level.availableBonuses = availableBonuses;
        level.bonusSpawnSec = bonusSpawnSec;
        level.items = items;
        level.areas = List.of();
        return level;
    }

    static Level.ItemModel itemModel(String type, int count, double velocity) {
        Level.ItemModel itemModel = new Level.ItemModel();
        itemModel.type = type;
        itemModel.count = count;
        itemModel.velocity = velocity;
        return itemModel;
    }

    static State state(Level... levels) {
        State state = new State(new EntityType[com.ali.dev.xonix.Config.GRID_SIZE_Y][com.ali.dev.xonix.Config.GRID_SIZE_X], List.of(levels));
        state.setCurLevel(0);
        state.topScores = new ArrayList<>(List.of(
                new Score("***", 0),
                new Score("***", 0),
                new Score("***", 0),
                new Score("***", 0),
                new Score("***", 0),
                new Score("***", 0),
                new Score("***", 0)
        ));
        return state;
    }

    static void fillWithFreeCells(State state) {
        for (int y = 0; y < state.entityGrid.length; y++) {
            for (int x = 0; x < state.entityGrid[y].length; x++) {
                state.entityGrid[y][x] = EntityType.FREE;
            }
        }
    }
}
