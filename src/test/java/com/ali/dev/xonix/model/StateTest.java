package com.ali.dev.xonix.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.ali.dev.xonix.Config.GRID_SIZE_X;
import static com.ali.dev.xonix.Config.GRID_SIZE_Y;
import static com.ali.dev.xonix.Config.INIT_LIFES;
import static com.ali.dev.xonix.Config.NEXT_LEVEL_WAIT_MS;
import static com.ali.dev.xonix.Config.TICK_TIME_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateTest {

    private final Path scoreFile = Path.of("scores.txt");
    private byte[] originalScoreFile;
    private boolean scoreFileExistedBefore;

    @AfterEach
    void restoreScoreFile() throws IOException {
        if (originalScoreFile != null) {
            Files.write(scoreFile, originalScoreFile);
        } else if (!scoreFileExistedBefore) {
            Files.deleteIfExists(scoreFile);
        }
    }

    @Test
    void updateProgressMarksLevelReadyWhenTargetReached() {
        State state = ModelTestSupport.state(ModelTestSupport.level(0.01, List.of(), 10, List.of()));
        state.initData();
        state.tickId = 42;

        state.entityGrid[10][10] = EntityType.BLOCK;
        state.updateProgress();

        assertTrue(state.isReadyForNewLevel());
        assertEquals(42 + NEXT_LEVEL_WAIT_MS / TICK_TIME_MS, state.nextLevelTick);
    }

    @Test
    void failHeadClearsPathRestoresStateAndConsumesOnlyOneLifePerTick() {
        State state = ModelTestSupport.state(ModelTestSupport.level(50.0, List.of(), 10, List.of()));
        state.initData();
        state.head.startPoint = new XY(15, 15);
        state.head.pos = new XY(20, 20);
        state.head.curPath.add(new XY(21, 20));
        state.head.curPath.add(new XY(22, 20));
        state.entityGrid[20][21] = EntityType.BLOCK;
        state.entityGrid[20][22] = EntityType.BLOCK;
        state.head.velocity = 2;

        Bonus activeBonus = new Bonus();
        activeBonus.type = BonusType.HEAD_SPEED_UP;
        state.activeBonuses.add(activeBonus);

        state.failHead();
        state.failHead();

        assertEquals(INIT_LIFES - 1, state.lifes);
        assertEquals(new XY(15, 15), state.head.pos);
        assertEquals(XY.STOP, state.head.shift);
        assertEquals(1, state.head.velocity);
        assertTrue(state.head.curPath.isEmpty());
        assertTrue(state.activeBonuses.isEmpty());
        assertEquals(EntityType.FREE, state.entityGrid[20][21]);
        assertEquals(EntityType.FREE, state.entityGrid[20][22]);
    }

    @Test
    void readScoresCreatesDefaultFileWhenMissing() throws IOException {
        backupScoreFile();
        Files.deleteIfExists(scoreFile);

        State state = ModelTestSupport.state(ModelTestSupport.level(50.0, List.of(), 10, List.of()));
        state.readScores();

        assertTrue(Files.exists(scoreFile));
        assertEquals(7, state.getTopScores().size());
        assertTrue(state.getTopScores().stream().allMatch(score -> score.getScore() == 0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void readScoreFileIgnoresMalformedEntriesAndPadsDefaults() throws Exception {
        State state = ModelTestSupport.state(ModelTestSupport.level(50.0, List.of(), 10, List.of()));
        Path malformed = Files.createTempFile("xonix-scores", ".txt");
        Files.write(malformed, List.of("alice;20", "", "broken", "bob;xx", "carol;15"), StandardCharsets.UTF_8);

        Method readScoreFile = State.class.getDeclaredMethod("readScoreFile", Path.class);
        readScoreFile.setAccessible(true);
        List<Score> scores = (List<Score>) readScoreFile.invoke(state, malformed);

        assertEquals(7, scores.size());
        assertEquals("alice", scores.get(0).getName());
        assertEquals(20, scores.get(0).getScore());
        assertEquals("carol", scores.get(1).getName());
        assertEquals(15, scores.get(1).getScore());
        assertEquals("***", scores.get(6).getName());
        assertEquals(0, scores.get(6).getScore());

        Files.deleteIfExists(malformed);
    }

    @Test
    void hasNextLevelReturnsFalseForLastConfiguredLevel() {
        State state = ModelTestSupport.state(
                ModelTestSupport.level(50.0, List.of(), 10, List.of()),
                ModelTestSupport.level(60.0, List.of(), 10, List.of())
        );

        state.setCurLevel(0);
        assertTrue(state.hasNextLevel());

        state.setCurLevel(1);
        assertFalse(state.hasNextLevel());
    }

    private void backupScoreFile() throws IOException {
        scoreFileExistedBefore = Files.exists(scoreFile);
        if (scoreFileExistedBefore) {
            originalScoreFile = Files.readAllBytes(scoreFile);
        }
    }
}
