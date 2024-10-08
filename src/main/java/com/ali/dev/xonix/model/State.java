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
    int lifes = INIT_LIFES;
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

    public long getTickId() {
        return tickId;
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

        if (progress * 100.0 >= getCurLevel().getTarget()) {
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

    public boolean checkHeadCollisions(Item item, int x, int y) {
        XY pos = new XY(x, y);
        if ((head.curPath.contains(pos) && item.getArea() != ItemAreaType.OutFiled)
                || (head.pos.x == x && head.pos.y == y)) {
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
        activeBonuses.stream().forEach(b -> b.type.restore.accept(this));
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
        return getCurLevel().getAreas().stream()
                .filter(a->a.getType().equals("slider"))
                .anyMatch(sl -> sl.contains(x, y));
    }

    public void initData() {
        checkingBusy = new EntityType[GRID_SIZE_Y][GRID_SIZE_X];
        for (int i = 0; i < GRID_SIZE_Y; i++) {
            for (int j = 0; j < GRID_SIZE_X; j++) {
                entityGrid[i][j] = EntityType.FREE;
            }
        }

        prepare();

        int width = GRID_SIZE_X - 4;
        int height = GRID_SIZE_Y - 4;
        Random rnd = new Random();
        // std
        Level.ItemModel std = getCurLevel().items.stream()
                .filter(itemModel -> itemModel.getType().equals("standard")).findAny().orElse(null);
        if (std != null) {
            for (int i = 0; i < std.getCount(); i++) {
                ItemType itemType = ItemType.STD;
                Item item = createItem(rnd, width, height, itemType, ItemAreaType.InField, std.getVelocity());
                items.add(item);
            }
        }

        //destroyer
        Level.ItemModel destroyer = getCurLevel().items.stream()
                .filter(itemModel -> itemModel.getType().equals("destroyer")).findAny().orElse(null);
        if (destroyer != null) {
            for (int i = 0; i < destroyer.getCount(); i++) {
                ItemType itemType = ItemType.DESTROYER;
                Item item = createItem(rnd, width, height, itemType, ItemAreaType.InField, destroyer.getVelocity());
                items.add(item);
            }
        }

        //ground
        Level.ItemModel grounds = getCurLevel().items.stream()
                .filter(itemModel -> itemModel.getType().equals("ground")).findAny().orElse(null);
        if (grounds != null) {
            for (int i = 0; i < grounds.getCount(); i++) {
                ItemType itemType = ItemType.STD;
                Item item = createItem(rnd, width, height, itemType, ItemAreaType.OutFiled, grounds.getVelocity());
                items.add(item);
            }
        }

        head.init();
        bonuses.clear();
        initBusyCells = calcBusyCells();
        updateProgress();
        score = 0;
    }

    private Item createItem(Random rnd, int width, int height,
                            ItemType itemType,
                            ItemAreaType itemAreaType,
                            double velocity) {
        int x = rnd.nextInt(width) + 2;
        int y = height + 2;
        if (itemAreaType == ItemAreaType.InField) {
            y = rnd.nextInt(height) + 2;
        }
        int d = rnd.nextInt(4);
        XY dir = XY.DIRECTIONS[d];
        return new Item(new XY(x, y), new XY(dir.x, dir.y), itemType,
                itemAreaType, velocity);
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

    public void setCurLevel(int curLevel) {
        this.curLevel = curLevel;
    }
}
