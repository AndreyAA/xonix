package com.ali.dev.xonix;

import com.ali.dev.xonix.State.XY;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.ali.dev.xonix.Config.*;
import static com.ali.dev.xonix.XonixApp.calcX;
import static com.ali.dev.xonix.XonixApp.calcY;

class Engine {
    private final State state;
    private final KeyboardInput keyboard;
    private final Random random = new Random();
    private final int TIME_FOR_BONUS_MS = 5000;
    private final int BONUS_LIVE_MS = 10000;

    public Engine(State state, KeyboardInput keyboard) {
        this.state = state;
        this.keyboard = keyboard;
        state.initNewWave();
    }

    public static int calcCol(int x) {
        return (x - MIN_X) / CELL_SIZE;
    }

    public static int calcRow(int y) {
        return (y - MIN_Y) / CELL_SIZE;
    }

    public void tick() {
        long start = System.currentTimeMillis();

        if (keyboard.isPressedOnce(KeyEvent.VK_P)) {
            state.isPause = !state.isPause;
        }

        if (!state.isPause && !state.isDebug) {
            state.tickId++;
        }

        if (state.isReadyForNewLevel) {
            if (state.tickId < state.nextLevelTick) {
                return;
            } else {
                state.nextLevel();
                return;
            }
        } else {
            updateKeys();
        }

        if (isActive(TickAction.MouseEvent)) {
            if (state.mouseEvent2 != null) {
                processMouseEvent2();
                state.mouseEvent2 = null;
            }
            if (state.mouseEvent != null) {
//                processMouseEvent();
                state.mouseEvent = null;
            }
        }

        if (isActive(TickAction.ItemMoving)) {
/*            if (state.nextWaveTick != 0 && state.tickId == state.nextWaveTick) {
                state.groupCount = state.curWave.countItems();
                state.nextWaveTick = 0;
                System.out.println(state.curWave);
            }*/

            //hide bonuses
            state.bonuses.removeIf(b -> b.lastTick < state.tickId);

            if (state.tickId % (TIME_FOR_BONUS_MS / TICK_TIME_MS) == 0) {
                var b = new State.Bonus();
                b.pos = new XY(random.nextInt(GRID_SIZE_X - 2), random.nextInt(GRID_SIZE_Y - 2));
                b.type = State.BonusType.values()[random.nextInt(State.BonusType.values().length)];
                b.size = new XY(2, 2);
                b.lastTick = state.tickId + BONUS_LIVE_MS / TICK_TIME_MS;
                state.bonuses.add(b);
            }


            if (state.tickId % 1 == 0) {
                moveHead();
            }

            // one tick - move 1 pixel
            if (state.tickId % 1 == 0) {
                moveItems();
            }
            //moveBullets();

        }

/*        if (state.initNewWave) {
            state.bullets.clear();
        }*/


        int tickTime = (int) (System.currentTimeMillis() - start);
        if (tickTime > 16) {
            System.out.println("tick time: " + tickTime + " ms");
        }
    }

    private void moveHead() {
        int moveEachTick = 5;
        if (state.head.velocity>1) {
            moveEachTick = 3;
        }
        if (state.tickId % moveEachTick != 0) {
            return;
        }
        // moving
        if (state.head.shift.x != 0 || state.head.shift.y != 0) {
            XY newPos = new XY(
                    Math.min(Math.max(0, state.head.pos.x + state.head.shift.x), GRID_SIZE_X - 1)
                    , Math.min(Math.max(0, state.head.pos.y + state.head.shift.y), GRID_SIZE_Y - 1)
            );

            if (state.head.curPath.contains(newPos)) {
                state.failHead();
                return;
            }

            if (state.head.curPath.size() > 0) {
                if (state.entityGrid[newPos.y][newPos.x].isBusy) {
                    //вернулись назад
                    state.head.curPath.clear();
                    System.out.println("end moving");
                    updateState();
                } else {
                    // продолжаем движение
                    state.head.curPath.add(newPos);
                    state.markBorder(newPos.y, newPos.x, true, EntityType.BORDER);
                }
            } else if (!state.entityGrid[newPos.y][newPos.x].isBusy) {
                System.out.println("start moving");
                state.head.startPoint = state.head.pos;
                state.head.curPath.add(newPos);
                state.markBorder(newPos.y, newPos.x, true, EntityType.BORDER);
            }
            state.head.pos = newPos;
        }

        var bonuses = state.bonuses.stream().filter(b -> hasCollision(state.head, b.pos, b.size)).collect(Collectors.toList());
        bonuses.forEach(b -> {
            b.type.consumer.accept(state);
        });
        state.bonuses.removeAll(bonuses);

    }

    private boolean hasCollision(State.Head item, XY targetPos, XY size) {
        return (calcX(item.pos.x) + CELL_SIZE > calcX(targetPos.x) && calcX(item.pos.x) < calcX(targetPos.x) + size.x * CELL_SIZE)
                &&
                (calcY(item.pos.y) + CELL_SIZE > calcY(targetPos.y) && calcY(item.pos.y) < calcY(targetPos.y) + size.y * CELL_SIZE);
    }

    private void updateState() {
        // take first free cell and fill all
        // if item is present in this cluster than skip it
        // and take next free cell


        var itemsSet = new HashSet<>();
        state.items.stream().filter(i -> i.type == ItemType.InField).forEach(i -> itemsSet.add(new XY(i.pos.x, i.pos.y)));
        // copy busy arr todo use the same array
        for (int i = 0; i < state.entityGrid.length; i++) {
            for (int j = 0; j < state.entityGrid[0].length; j++) {
                state.checkingBusy[i][j] = state.entityGrid[i][j];
            }
        }

        XY emptyPoint = processGrid(itemsSet, state.checkingBusy);
        if (emptyPoint != null) {
            //System.out.println("empty point: " + emptyPoint);
            isEmptyArea(itemsSet, state.entityGrid, emptyPoint, (y, x) -> {
                state.entityGrid[y][x] = EntityType.BORDER;
            });
        }

        state.updateProgress();
    }

    private XY processGrid(HashSet<Object> itemsSet, EntityType[][] busy) {
        XY startPoint;
        while ((startPoint = findFirstFreePoint(busy)) != null) {
            System.out.println("check area from point: " + startPoint);
            if (isEmptyArea(itemsSet, busy, startPoint, (y, x) -> {
            })) {
                System.out.println("empty area");
                return startPoint;
            } else {
                System.out.println("not empty area");
            }
        }
        return null;
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
            System.out.println("found item");
            return false;
        } else {
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
            busy[newY][newX] = EntityType.BORDER;
            consumer.accept(newY, newX);
        }
        boolean res = itemsSet.contains(new XY(newX, newY));
        if (res) {
            System.out.println("found item: x:" + newX + ", y: " + newY);
        }
        return res;
    }

    private void updateKeys() {
        if (keyboard.keyDown(KeyEvent.VK_LEFT)) {
            state.head.shift = XY.LEFT;
        } else if (keyboard.keyDown(KeyEvent.VK_RIGHT)) {
            state.head.shift = XY.RIGHT;
        } else if (keyboard.keyDown(KeyEvent.VK_UP)) {
            state.head.shift = XY.TOP;
        } else if (keyboard.keyDown(KeyEvent.VK_DOWN)) {
            state.head.shift = XY.DOWN;
        } else {
            state.head.shift = XY.STOP;
        }

      /*  if (keyboard.isPressed(KeyEvent.VK_O)) {
            state.isDebug = !state.isDebug;
            System.out.println("debug: " + state.isDebug);
        }

        if (state.isDebug && keyboard.isPressed(KeyEvent.VK_S)) {
            System.out.println("serialize state");
            Serializator.serialize(state);
        }

        if (state.isDebug && keyboard.isPressed(KeyEvent.VK_G)) {
            System.out.println("add money");
            state.money+=500;
        }*/
    }

   /* private void processMouseEvent() {
        MouseEvent e = state.mouseEvent;
        int row = calcRow(e.getY());
        int col = calcCol(e.getX());

        if (row >= 1 && row < GRID_SIZE_Y && col >= 1 && col < GRID_SIZE_X-1) {
            if (state.activeButton == 0) {
                // remove
                if (state.entityGrid[row][col] != EntityType.BORDER) {
                    clearCell(row, col);
                    state.towers.removeIf(t -> t.col == col && t.row == row);
                    System.out.println("clear block row:" + row + ", col:" + col);
                    updateItemPaths();
                }
            } else if (state.activeButton == 1) {
                if (state.entityGrid[row][col]==null && state.money >= BLOCK_PRICE) {
                    state.money -= BLOCK_PRICE;
                    markBorder(row, col, true, EntityType.BLOCK);
                    *//*if (checkAllPaths(row, col)) {
                        System.out.println("block row:" + row + ", col:" + col);
                        updateItemPaths();
                    } else {
                        clearCell(row, col);
                    }*//*
                }
            } else if (state.activeButton == 2 || state.activeButton == 3) {
                if (state.entityGrid[row][col]==null && state.money >= TOWER1_PRICE) {
                    state.money -= TOWER1_PRICE;
                    markBorder(row, col, true, state.currentEntityType);
 *//*                   if (checkAllPaths(row, col)) {
                        TowerType type = (state.activeButton == 2) ? TowerType.STD : TowerType.FREEZE;
                        state.towers.add(new Tower(row, col, 1, TOWER_RANGE, type));
                        System.out.println("tower row:" + row + ", col:" + col);
                        updateItemPaths();
                    } else {
                        clearCell(row, col);
                    }*//*
//                repaint();
                }
            }
        }
        if (row == GRID_SIZE_Y && (col >= GRID_SIZE_X / 2) && (col <= GRID_SIZE_X / 2 + 3)) {
            state.activeButton = col - GRID_SIZE_X / 2;
            System.out.println("activeButton: " + state.activeButton);
        }

        // process selected tower
        if (state.selectedTower!=null) {
            int shift = (state.selectedTower.type == TowerType.STD)?2:3;

            if (row > GRID_SIZE_Y && row <= GRID_SIZE_Y + 3 && (col == GRID_SIZE_X / 2 +shift)) {
                int tempAction2 = row - GRID_SIZE_Y;
                int nextLevelMoney = state.selectedTower.levels.getLevel(tempAction2).nextLevelMoney();
                if (!state.selectedTower.levels.inUpgrade()
                        && state.money>=nextLevelMoney) {
                    state.activeButton2 = tempAction2;

                    processClickUpgrade(state.selectedTower, state.activeButton2);
                    System.out.println("Up tower: range:" + state.selectedTower.range +
                            ", damage:" + state.selectedTower.damage +
                            ", rate:" + state.selectedTower.rate
                    );
                }
            }
*//*            int base = GRID_SIZE_X / 2 + 6;
            if (row == GRID_SIZE_Y && col >= base && col < base + ShootStrategy.values().length) {
                int strategy = col - base;
                state.selectedTower.shootStrategy = ShootStrategy.values()[strategy];
            }*//*
        }
    }*/

  /*  private void processClickUpgrade(Tower selectedTower, int action) {
        switch (action) {
            case 1: {
                // range
                selectedTower.levels.range.startFrom = state.tickId + TOWER_UPGRADE_TIME_MS /TICK_TIME_MS;
                selectedTower.levels.range.inited = state.tickId;
                state.money -= selectedTower.levels.range.nextLevelMoney();
                selectedTower.levels.range.consumer = (item)->{
                    item.levels.range.value+=1;
                    item.range+=CELL_SIZE;
                };
                return;
            }
            case 2: {
                // damage
                selectedTower.levels.damage.startFrom = state.tickId + TOWER_UPGRADE_TIME_MS /TICK_TIME_MS;
                selectedTower.levels.damage.inited = state.tickId;
                state.money -= selectedTower.levels.damage.nextLevelMoney();
                selectedTower.levels.damage.consumer = (item)->{
                    item.levels.damage.value+=1;
                    item.damage+= selectedTower.initDamage;
                };
                return;
            }
            case 3: {
                // rate 30-> 15 -> 7
                int cRate = Math.max(5, selectedTower.rate / 2);
                if (cRate!=selectedTower.rate) {
                    selectedTower.levels.rate.inited = state.tickId;
                    selectedTower.levels.rate.startFrom = state.tickId + TOWER_UPGRADE_TIME_MS /TICK_TIME_MS;
                    state.money -= selectedTower.levels.rate.nextLevelMoney();
                    selectedTower.levels.rate.consumer = (item)->{
                        item.levels.rate.value+=1;
                        selectedTower.rate = Math.max(5, cRate);
                    };
                }
                return;
            }
            default:
        }
    }*/

    private void logXY() {
        System.out.println("x:" + state.head.pos.x + ", y:" + state.head.pos.y);
    }

    private void processMouseEvent2() {
        int row2 = (state.mouseEvent2 != null) ? calcRow(state.mouseEvent2.getY()) : 0;
        int col2 = (state.mouseEvent2 != null) ? calcCol(state.mouseEvent2.getX()) : 0;
/*        var sTower = state.towers.stream().filter(t -> t.col == col2 && t.row == row2).findAny().orElse(null);
        if (sTower!=null) {
            state.selectedTower = sTower;
        }*/
    }

    private void clearCell(int row, int col) {
        state.markBorder(row, col, false, EntityType.FREE);
    }

    private void clearGrid() {
        for (int row = 0; row < GRID_SIZE_Y; row++) {
            for (int col = 0; col < GRID_SIZE_X; col++) {
                clearCell(row, col);
            }
        }
        state.items.clear();
        state.score = 0;
    }

    private void moveItems() {
        for (Item item : state.items) {
            item.move(state);
        }
    }

    private boolean isActive(TickAction action) {
        switch (action) {
            case MouseEvent: {
                return !state.isPause;
            }
            case ItemMoving: {
                return !state.isPause && !state.isDebug;
            }
        }
        return false;
    }

    enum TickAction {
        MouseEvent,
        ItemMoving
    }
}
