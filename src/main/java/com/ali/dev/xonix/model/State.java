package com.ali.dev.xonix.model;

import com.ali.dev.xonix.Config;
import com.ali.dev.xonix.ScoreCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.ali.dev.xonix.Config.*;

public class State {
    private static final Logger log = LoggerFactory.getLogger(State.class);
    protected final List<Level> levels;
    private final ScoreCalculator scoreCalculator = new ScoreCalculator();


    public EntityType[][] entityGrid;
    long tickId = 0;
    long nextLevelTick = 0;
    int score = 0;
    int lifes = 3;
    boolean isPause;
    boolean isDebug;
    int curLevel;

    // items
    protected List<Item> items = new ArrayList<>();
    EntityType[][] checkingBusy;
    Head head = new Head();
    List<Bonus> bonuses = new ArrayList<>();
    List<Bonus> activeBonuses = new ArrayList<>();
    double progress;
    boolean isReadyForNewLevel;
    int initBusyCells;
    int busyCells;
    boolean isGameOver;
    boolean enterName;
    List<Score> topScores;

    public State(EntityType[][] entityTypes, List<Level> levels) {
        entityGrid = entityTypes;
        this.levels = levels;
    }

    public void setLifes(int lifes) {
        this.lifes = lifes;
    }

    public List<Score> getTopScores() {
        return topScores;
    }

    public List<Item> getItems() {
        return items;
    }

    public Head getHead() {
        return head;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public void updateProgress() {
        double newProgress = calcProgress();
        int deltaScore = scoreCalculator.calcScore(progress, newProgress, this);
        progress = newProgress;
        score += deltaScore;

        if (progress * 100.0 >= getCurLevel().getLevelThreshold()) {
            isReadyForNewLevel = true;
            nextLevelTick = tickId + NEXT_LEVEL_WAIT_MS / Config.TICK_TIME_MS;
        }
    }

    public List<Bonus> getActiveBonuses() {
        return activeBonuses;
    }

    public List<Bonus> getBonuses() {
        return bonuses;
    }

    public double getProgress() {
        return progress;
    }

    private double calcProgress() {
        busyCells = calcBusyCells();
        return calcProgressLight(busyCells);
    }

    double calcProgressLight(int busyCells) {
        int sum = busyCells - initBusyCells;
        int total = GRID_SIZE_Y * GRID_SIZE_X - initBusyCells;
        return sum * 1.0 / total;
    }

    private int calcBusyCells() {
        int sum = 0;
        for (int y = 0; y < GRID_SIZE_Y; y++) {
            for (int x = 0; x < GRID_SIZE_X; x++) {
                if (entityGrid[y][x].isBusy) {
                    sum++;
                }
            }
        }
        return sum;
    }

    public Level getCurLevel() {
        return levels.get(curLevel);
    }

    public boolean checkCollisions(int x, int y) {
        XY pos = new XY(x, y);
        if (head.curPath.contains(pos) || (head.pos.x == x && head.pos.y == y)) {
            failHead();
            return true;
        }
        return false;
    }

    public void failHead() {
        log.debug("fail head");
        head.curPath.forEach(p -> {
            entityGrid[p.y][p.x] = EntityType.FREE;
        });
        head.curPath.clear();
        head.pos = (head.startPoint != null) ? head.startPoint : new XY(GRID_SIZE_X / 2, 0);
        head.shift = XY.STOP;
        activeBonuses.clear();
        lifes--;
    }

    public void nextLevel() {
        curLevel++;
        thisLevel();
    }

    public int getCurLevelNumber() {
        return curLevel;
    }

    public void thisLevel() {
        log.debug("init level: {}", curLevel);
        items.clear();
        activeBonuses.clear();
        isReadyForNewLevel = false;
        entityGrid = new EntityType[GRID_SIZE_Y][GRID_SIZE_X];
        initData();
        log.debug("level is ready");
    }

    public void prepare() {
        // prepare
        for (int i = 0; i < GRID_SIZE_Y; i++) {
            //left vertical
            markBorder(i, 0, true, EntityType.BORDER);
            markBorder(i, 1, true, EntityType.BORDER);
            //right vertical
            markBorder(i, GRID_SIZE_X - 1, true, EntityType.BORDER);
            markBorder(i, GRID_SIZE_X - 2, true, EntityType.BORDER);
        }

        for (int i = 0; i < GRID_SIZE_X - 1; i++) {
            //top horizontal
            markBorder(0, i, true, EntityType.BORDER);
            markBorder(1, i, true, EntityType.BORDER);
            //bottom horizontal
            markBorder(GRID_SIZE_Y - 1, i, true, EntityType.BORDER);
            markBorder(GRID_SIZE_Y - 2, i, true, EntityType.BORDER);
        }
    }

    void markBorder(int i, int x, boolean x1, EntityType border) {
        entityGrid[i][x] = border;
    }

    public boolean inSliders(int x, int y) {
        return getCurLevel().sliders.stream().anyMatch(sl -> sl.contains(x, y));
    }

    public void initData() {
        checkingBusy = new EntityType[GRID_SIZE_Y][GRID_SIZE_X];
        for (int i = 0; i < GRID_SIZE_Y; i++) {
            for (int j = 0; j < GRID_SIZE_X; j++) {
                entityGrid[i][j] = EntityType.FREE;
            }
        }

        prepare();

        int n = getCurLevel().getItemInField();
        int width = GRID_SIZE_X - 4;
        int height = GRID_SIZE_Y - 4;
        int destroyers = getCurLevel().getItemInFieldDestroyers();
        Random rnd = new Random();
        for (int i = 0; i < n; i++) {
            ItemType itemType = ItemType.STD;
            if (destroyers > 0 && i < destroyers) {
                itemType = ItemType.DESTROYER;
            }
            int x = rnd.nextInt(width) + 2;
            int y = rnd.nextInt(height) + 2;
            int d = rnd.nextInt(4);
            XY dir = XY.DIRECTIONS[d];
            Item item = new Item(new XY(x, y), new XY(dir.x, dir.y), itemType,
                    ItemAreaType.InField, getCurLevel().velocityInField);
            items.add(item);
        }

        int out = getCurLevel().itemOutField;
        for (int i = 0; i < out; i++) {
            int d = rnd.nextInt(4);
            XY dir = XY.DIRECTIONS[d];
            Item item = new Item(new XY(rnd.nextInt(GRID_SIZE_X), GRID_SIZE_Y - 1), new XY(dir.x, dir.y),
                    ItemType.STD, ItemAreaType.OutFiled, getCurLevel().velocityOutField);
            items.add(item);
        }
        head.init();
        bonuses.clear();
        initBusyCells = calcBusyCells();
        updateProgress();
        score = 0;
    }

    public void addScore(String name) {
        topScores.add(new Score(name, score));
        topScores = topScores.stream().sorted(Comparator.comparing(Score::getScore).reversed())
                .limit(7).collect(Collectors.toList());
    }

    public void storeScores() throws IOException {
        String data = topScores.stream()
                .map(s -> s.getName() + ";" + s.getScore())
                .collect(Collectors.joining("\n"));

        Files.write(Paths.get("./scores.txt"), data.getBytes(StandardCharsets.UTF_8));
    }

    public void readScores() throws IOException {
        Path path = Paths.get("scores.txt");

        if (!Files.exists(path)) {
            Files.createFile(path);
            Files.write(path, List.of("***;0", "***;0", "***;0", "***;0", "***;0", "***;0", "***;0"));
        }
        this.topScores = readScoreFile(path);
    }

    private static List<Score> readScoreFile(Path path) throws IOException {
        return Files.readAllLines(path)
                .stream().map(str -> str.split(";"))
                .map(strArr -> new Score(strArr[0], Integer.parseInt(strArr[1])))
                .sorted(Comparator.comparing(Score::getScore).reversed())
                .collect(Collectors.toList());
    }

    public boolean isEnterName() {
        return enterName;
    }

    public void setEnterName(boolean enterName) {
        this.enterName = enterName;
    }


    public int getScore() {
        return score;
    }

    public int getLifes() {
        return lifes;
    }

    public boolean isPause() {
        return isPause;
    }

    public boolean isReadyForNewLevel() {
        return isReadyForNewLevel;
    }


}
