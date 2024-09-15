package com.ali.dev.xonix.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.function.BiConsumer;

import static com.ali.dev.xonix.Config.GRID_SIZE_X;
import static com.ali.dev.xonix.Config.GRID_SIZE_Y;

public class Algo {
    private static final Logger log = LoggerFactory.getLogger(Algo.class);
    private final State state;

    public Algo(State state) {
        this.state = state;
    }

    public void updateState() {
        // take first free cell and fill all
        // if item is present in this cluster than skip it
        // and take next free cell
        var itemsSet = new HashSet<>();
        state.items.stream().filter(i -> i.area == ItemAreaType.InField).forEach(i -> itemsSet.add(new XY(i.pos.x, i.pos.y)));
        for (int i = 0; i < state.entityGrid.length; i++) {
            System.arraycopy(state.entityGrid[i], 0, state.checkingBusy[i], 0, state.entityGrid[0].length);
        }

        processGrid(itemsSet, state.checkingBusy);
        state.updateProgress();
    }

    private void processGrid(HashSet<Object> itemsSet, EntityType[][] busy) {
        XY startPoint;
        while ((startPoint = findFirstFreePoint(busy)) != null) {
            log.debug("check area from point: {}", startPoint);
            if (isEmptyArea(itemsSet, busy, startPoint, (y, x) -> {
            })) {
                log.debug("empty area");
                // fill in by blocks
                isEmptyArea(itemsSet, state.entityGrid, startPoint,
                        (y, x) -> state.entityGrid[y][x] = EntityType.BLOCK
                );
                // continue to find next empty area
            } else {
                log.debug("not empty area");
            }
        }
    }

    private boolean isEmptyArea(HashSet<Object> itemsSet, EntityType[][] busy, XY startPoint, BiConsumer<Integer, Integer> consumer) {
        Queue<XY> points = new ArrayDeque<>();
        points.add(startPoint);

        boolean foundItem = false;
        while (!points.isEmpty()) {
            XY point = points.poll();
            foundItem |= process(itemsSet, busy, point, points, 0, 1, consumer);
            foundItem |= process(itemsSet, busy, point, points, 0, -1, consumer);
            foundItem |= process(itemsSet, busy, point, points, 1, 0, consumer);
            foundItem |= process(itemsSet, busy, point, points, -1, 0, consumer);
            foundItem |= process(itemsSet, busy, point, points, 0, 0, consumer);
        }

        if (foundItem) {
            // надо закрасить на основной доске
            log.debug("found item");
            return false;
        } else {
            return true;
        }
    }

    private XY findFirstFreePoint(EntityType[][] busy) {
        for (int y = 0; y < busy.length; y++) {
            for (int x = 0; x < busy[y].length; x++) {
                if (!busy[y][x].isBusy && busy[y][x] != EntityType.FREE_AFTER_BOMB) {
                    return new XY(x, y);
                }
            }
        }
        return null;
    }

    private boolean process(HashSet<Object> itemsSet, EntityType[][] busy, XY point, Queue<XY> points,
                            int dx, int dy, BiConsumer<Integer, Integer> consumer) {
        int newY = point.y + dy;
        int newX = point.x + dx;
        if (newX < 0 || newY < 0 || newX >= GRID_SIZE_X - 1 || newY >= GRID_SIZE_Y - 1) {
            return false;
        }
        if (!busy[newY][newX].isBusy) {
            points.add(new XY(newX, newY));
            busy[newY][newX] = EntityType.BLOCK;
            consumer.accept(newY, newX);
        }
        boolean res = itemsSet.contains(new XY(newX, newY));
        if (res) {
            log.trace("found item: x: {}, y: {}", newX, newY);
        }
        return res;
    }

}
