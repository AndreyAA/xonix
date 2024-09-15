package com.ali.dev.xonix.model;

import com.ali.dev.xonix.KeyboardInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

import static com.ali.dev.xonix.Config.*;
import static com.ali.dev.xonix.XonixApp.calcX;
import static com.ali.dev.xonix.XonixApp.calcY;

public class Engine {
    private static final Logger log = LoggerFactory.getLogger(Engine.class);
    private final State state;
    private final KeyboardInput keyboard;
    private final Random random = new Random();
    private final GameOverListener listener;
    private final Algo algo;

    private long lastAnimateTime = System.currentTimeMillis();

    public Engine(State state, KeyboardInput keyboard, GameOverListener listener) {
        this.state = state;
        this.algo = new Algo(state);
        this.keyboard = keyboard;
        this.listener = listener;
    }

    public void tick() {
        long start = System.currentTimeMillis();

        if (keyboard.isPressedOnce(KeyEvent.VK_SPACE)) {
            state.isPause = !state.isPause;
        }

        if (state.lifes <= 0 && !state.isGameOver) {
            gameOverEvent();
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
        }

        int x = calcX(state.head.pos.x);
        int y = calcY(state.head.pos.y);

        if (!state.inSliders(x, y)) {
            updateKeys();
        }

        if (isActive(TickAction.ItemMoving) && !state.isGameOver) {
            //remove expired bonuses
            state.bonuses.stream().filter(b -> b.lastTick < state.tickId).forEach(b -> b.type.onExpire.accept(state, b));
            state.bonuses.removeIf(b -> b.lastTick < state.tickId);

            //remove expired active bonuses
            state.activeBonuses.stream()
                    .filter(b -> b.lastTick < state.tickId)
                    .forEach(b -> b.type.reject.accept(state));
            state.activeBonuses.removeIf(b -> b.lastTick < state.tickId);

            // generate new bonuses
            List<String> availableBonuses = state.getCurLevel().getAvailableBonuses();
            if (state.tickId % (TIME_FOR_BONUS_MS / TICK_TIME_MS) == 0 && availableBonuses.size()>0) {
                var b = new Bonus();
                b.pos = new XY(random.nextInt(GRID_SIZE_X - 2), random.nextInt(GRID_SIZE_Y - 2));
                b.type = BonusType.valueOf(availableBonuses.get(random.nextInt(availableBonuses.size())));
                b.size = new XY(2, 2);
                b.lastTick = state.tickId + BONUS_LIVE_MS / TICK_TIME_MS;
                state.bonuses.add(b);
            }

            // one tick - move 1 pixel
            moveHead();
            moveItems();
            state.progress = state.calcProgressLight(state.busyCells);
        }

        // animation
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnimateTime >= BLINK_EACH_MS) { // 500 миллисекунд на
            if (!state.isPause) {
                state.bonuses.forEach(Bonus::incFrame);
                state.activeBonuses.forEach(Bonus::incFrame);
            }
            lastAnimateTime = currentTime;
        }

        int tickTime = (int) (System.currentTimeMillis() - start);
        if (tickTime > TICK_TIME_MS) {
            log.warn("tick time: {} ms", tickTime);
        }
    }

    private void moveHead() {
        int moveEachTick = 5;
        if (state.head.velocity > 1) {
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

            if (!state.head.curPath.isEmpty()) {
                if (state.entityGrid[newPos.y][newPos.x].isBusy) {
                    //вернулись назад
                    state.head.curPath.clear();
                    log.debug("end moving: {}", state.head.pos);
                    algo.updateState();
                } else {
                    // продолжаем движение
                    state.head.curPath.add(newPos);
                    state.markBorder(newPos.y, newPos.x, true, EntityType.BLOCK);
                }
            } else if (!state.entityGrid[newPos.y][newPos.x].isBusy) {
                log.debug("start moving: {}", state.head.pos);
                state.head.startPoint = state.head.pos;
                state.head.curPath.add(newPos);
                state.markBorder(newPos.y, newPos.x, true, EntityType.BLOCK);
            }
            state.head.pos = newPos;
        }

        var bonuses = state.bonuses.stream().filter(b -> hasCollision(state.head, b.pos, b.size)).collect(Collectors.toList());
        bonuses.forEach(b -> {
            b.type.apply.accept(state);
            if (b.type.durable) {
                b.lastTick = state.tickId + state.getCurLevel().getBonusSpawnSec()*1000 / TICK_TIME_MS;
                state.activeBonuses.add(b);
            }
        });
        state.bonuses.removeAll(bonuses);

    }

    private boolean hasCollision(Head item, XY targetPos, XY size) {
        return (calcX(item.pos.x) + CELL_SIZE > calcX(targetPos.x) && calcX(item.pos.x) < calcX(targetPos.x) + size.x * CELL_SIZE)
                &&
                (calcY(item.pos.y) + CELL_SIZE > calcY(targetPos.y) && calcY(item.pos.y) < calcY(targetPos.y) + size.y * CELL_SIZE);
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
    }

    private void gameOverEvent() {
        state.isGameOver = true;
        int minScore = state.topScores.stream().mapToInt(Score::getScore).min().orElse(0);
        state.enterName = minScore < state.score;
        listener.onGameOver();
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

}
