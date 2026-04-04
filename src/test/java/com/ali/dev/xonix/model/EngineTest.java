package com.ali.dev.xonix.model;

import com.ali.dev.xonix.KeyboardInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ali.dev.xonix.Config.BONUS_LIVE_MS;
import static com.ali.dev.xonix.Config.GRID_SIZE_X;
import static com.ali.dev.xonix.Config.GRID_SIZE_Y;
import static com.ali.dev.xonix.Config.TICK_TIME_MS;
import static com.ali.dev.xonix.Config.TIME_FOR_BONUS_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineTest {

    @Test
    void completingLastLevelTriggersGameOverInsteadOfAccessingMissingLevel() {
        Level lastLevel = ModelTestSupport.level(50.0, List.of(), 10, List.of());
        State state = ModelTestSupport.state(lastLevel);
        state.isReadyForNewLevel = true;
        state.nextLevelTick = 0;
        state.topScores = List.of(new Score("bob", 10));

        RecordingGameOverListener listener = new RecordingGameOverListener();
        Engine engine = new Engine(state, new KeyboardInput(), listener);

        engine.tick();

        assertTrue(state.isGameOver());
        assertTrue(listener.invoked);
        assertEquals(0, state.getCurLevelNumber());
    }

    @Test
    void generatedBonusAlwaysStaysInsidePlayableArea() {
        Level level = ModelTestSupport.level(50.0, List.of("LIFE"), 10, List.of());
        State state = ModelTestSupport.state(level);
        state.initData();
        state.tickId = TIME_FOR_BONUS_MS / TICK_TIME_MS - 1L;

        Engine engine = new Engine(state, new KeyboardInput(), () -> {
        });

        engine.tick();

        assertEquals(1, state.getBonuses().size());
        Bonus bonus = state.getBonuses().get(0);
        assertNotNull(bonus);
        assertEquals(BonusType.LIFE, bonus.getType());
        assertTrue(bonus.getPos().getX() >= 2);
        assertTrue(bonus.getPos().getX() <= GRID_SIZE_X - 4);
        assertTrue(bonus.getPos().getY() >= 2);
        assertTrue(bonus.getPos().getY() <= GRID_SIZE_Y - 4);
        assertEquals(state.getTickId() + BONUS_LIVE_MS / TICK_TIME_MS, bonus.getLastTick());
    }

    @Test
    void expiredDurableBonusRestoresStateOnTick() {
        Level level = ModelTestSupport.level(50.0, List.of(), 1, List.of());
        State state = ModelTestSupport.state(level);
        state.initData();
        state.head.velocity = 2;

        Bonus bonus = new Bonus();
        bonus.type = BonusType.HEAD_SPEED_UP;
        bonus.lastTick = 0;
        state.activeBonuses.add(bonus);
        state.tickId = 1;

        Engine engine = new Engine(state, new KeyboardInput(), () -> {
        });

        engine.tick();

        assertEquals(1, state.head.velocity);
        assertTrue(state.activeBonuses.isEmpty());
        assertFalse(state.isGameOver());
    }

    private static final class RecordingGameOverListener implements GameOverListener {
        private boolean invoked;

        @Override
        public void onGameOver() {
            invoked = true;
        }
    }
}
