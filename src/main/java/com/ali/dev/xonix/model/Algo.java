package com.ali.dev.xonix.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
        Set<XY> pointsToStartFilling = collectPointsToStartFilling(busy);
        log.debug("points to check: {}", pointsToStartFilling);
        for (XY startPoint : pointsToStartFilling) {

            log.debug("check area from point: {}", startPoint);
            if (busy[startPoint.y][startPoint.x].isBusy) {
                log.debug("already marked as busy: {}", startPoint);
                continue;
            }
            if (isEmptyArea(itemsSet, busy, startPoint, (y, x) -> {
            })) {
                log.debug("empty area");
                // fill in by blocks
                isEmptyArea(itemsSet, state.entityGrid, startPoint,
                        (y, x) -> {
                    state.entityGrid[y][x] = EntityType.BLOCK;
                    log.debug("mark as block x: {}, y:{}", x ,y);
                        }
                );
                // continue to find next empty area
            } else {
                log.debug("not empty area");
            }
        }
    }

    private Set<XY> collectPointsToStartFilling(EntityType[][] busy) {
        Set<XY> result = new HashSet<>();
        for (XY xy: state.head.getCurPath()) {
            addFreeNear(xy, result, busy);
        }
        return result;
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
            log.debug("found item");
            return false;
        } else {
            // надо закрасить на основной доске
            return true;
        }
    }

    private XY findFirstFreePoint(EntityType[][] busy) {
        for (int y = 0; y < busy.length; y++) {
            for (int x = 0; x < busy[y].length; x++) {
                if (!busy[y][x].isBusy) {
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

    private void addFreeNear(XY pos, Set<XY> result, EntityType[][] busy) {
        addFreeNear(pos, 1 ,0, result, busy);
        addFreeNear(pos, -1 ,0, result, busy);
        addFreeNear(pos, 0 ,1, result, busy);
        addFreeNear(pos, 0 ,-1, result, busy);
    }

    private void addFreeNear(XY pos, int dx, int dy, Set<XY> result, EntityType[][] busy) {
        if (!busy[pos.y+dy][pos.x + dx].isBusy) {
            result.add(new XY(pos.x + dx, pos.y+dy));
        }
    }

}
