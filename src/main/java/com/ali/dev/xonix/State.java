package com.ali.dev.xonix;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static com.ali.dev.xonix.Config.GRID_SIZE_X;
import static com.ali.dev.xonix.Config.GRID_SIZE_Y;

public class State {

    protected final List<Level> levels = List.of(
            new Level(2, 2,0, 2, 1, 80),
            new Level(5, 0,1, 3, 1, 90),
            new Level(7, 0,2, 1,1, 95),
            new Level(10, 0, 4, 3, 1, 97)
    );
    private final ScoreCalculator scoreCalculator = new ScoreCalculator();
    protected int waveWaitTimeMs = 5000;
    protected EntityType[][] entityGrid;
    protected long tickId = 0;
    protected long nextLevelTick = 0;
    protected int mouseX = -1;
    protected int mouseY = -1;
    protected int highlightedRow = -1;
    protected int highlightedCol = -1;
    protected int score = 0;
    protected int lifes = 3;
    protected int breakItems = 3;
    protected volatile boolean isPause;
    protected volatile boolean isDebug;
    protected EventListener eventListener;
    protected int curLevel;
    protected volatile MouseEvent mouseEvent;
    protected volatile MouseEvent mouseEvent2;
    EntityType[][] checkingBusy;
    Head head = new Head();
    // items
    protected List<Item> items = new ArrayList<>();
    List<Bonus> bonuses = new ArrayList<>();
    List<Bonus> activeBonuses = new ArrayList<>();
    double progress;
    boolean isReadyForNewLevel;
    private int initBusyCells;

    public State(EventListener eventListener, EntityType[][] entityTypes) {
        this.eventListener = eventListener;
        entityGrid = entityTypes; // todo copy
    }

    public void updateProgress() {
        double newProgress = calcProgress();
        int deltaScore = scoreCalculator.calcScore(progress, newProgress, this);
        progress = newProgress;
        score+=deltaScore;

        if (progress * 100.0 >= getCurLevel().levelThreshold) {
            isReadyForNewLevel = true;
            nextLevelTick = tickId + waveWaitTimeMs / Config.TICK_TIME_MS;
        }
    }

    private double calcProgress() {
        int sum = calcBusyCells() - initBusyCells;
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
        System.out.println("fail head");
        head.curPath.forEach(p -> {
            entityGrid[p.y][p.x] = EntityType.FREE;
        });
        head.curPath.clear();
        head.pos = (head.startPoint != null) ? head.startPoint : new XY(GRID_SIZE_X / 2, 0);
        head.shift = XY.STOP;
        lifes--;
    }

    public void nextLevel() {
        System.out.println("init new level");
        items.clear();
        curLevel++;
        isReadyForNewLevel = false;
        entityGrid = new EntityType[GRID_SIZE_Y][GRID_SIZE_X];
        initData();
        System.out.println("new level is ready");
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

    public void initData() {
        checkingBusy = new EntityType[GRID_SIZE_Y][GRID_SIZE_X];
        for (int i = 0; i < GRID_SIZE_Y; i++) {
            for (int j = 0; j < GRID_SIZE_X; j++) {
                entityGrid[i][j] = EntityType.FREE;
            }
        }

        prepare();

        int n = getCurLevel().itemInField;
        int width = GRID_SIZE_X - 4;
        int height = GRID_SIZE_Y - 4;
        int destroyers = getCurLevel().itemInFieldDestroyers;
        Random rnd = new Random();
        for (int i = 0; i < n; i++) {
            ItemType itemType = ItemType.STD;
            if (destroyers>0 && i<destroyers) {
                itemType = ItemType.DESTROYER;
            }
            int x = rnd.nextInt(width) + 2;
            int y = rnd.nextInt(height) + 2;
            int d = rnd.nextInt(4);
            XY dir = XY.DIRECTIONS[d];
            Item item = new Item(new XY(x, y), new XY(dir.x, dir.y), itemType,
                    ItemArea.InField, getCurLevel().velocityInField);
            items.add(item);
        }

        int out = getCurLevel().itemOutField;
        for (int i = 0; i < out; i++) {
            int d = rnd.nextInt(4);
            XY dir = XY.DIRECTIONS[d];
            Item item = new Item(new XY(rnd.nextInt(GRID_SIZE_X), GRID_SIZE_Y - 1), new XY(dir.x, dir.y),
                    ItemType.STD, ItemArea.OutFiled, getCurLevel().velocityOutField);
            items.add(item);
        }
        head.init();
        bonuses.clear();
        initBusyCells = calcBusyCells();
        updateProgress();
        score = 0;

    }

    public void initNewWave() {
        System.out.println("init new wave in tick: " + tickId);
    }


    enum BonusType {
        LIFE(s -> s.lifes++, s-> {}, Images.bonusLife, false),
        HEAD_SPEED(s -> s.head.velocity = 2, s -> s.head.velocity = 1, Images.speedUp, true),
        FREEZE(s -> s.items.stream().filter(i->i.area == ItemArea.InField).forEach(Item::slowDown),
                s -> s.items.stream().filter(i->i.area == ItemArea.InField).forEach(Item::restore),
                Images.freeze, true);

        final Consumer<State> apply;
        final Consumer<State> regect;
        final Image image;
        final boolean durable;

        BonusType(Consumer<State> apply, Consumer<State> regect, Image image, boolean durable) {
            this.apply = apply;
            this.regect = regect;
            this.image = image;
            this.durable = durable;
        }
    }

    public static class Head {
        // head
        XY pos;
        XY shift;
        Set<XY> curPath;
        XY startPoint;
        int velocity;

        public void init() {
            pos = new XY(GRID_SIZE_X / 2, 1);
            shift = new XY(0, 0);
            curPath = new HashSet<>();
            startPoint = null;
            velocity = 1;
        }
    }

    public static class XY {
        public static final XY TOP = new XY(0, -1);
        public static final XY DOWN = new XY(0, 1);
        public static final XY LEFT = new XY(-1, 0);
        public static final XY RIGHT = new XY(1, 0);
        public static final XY STOP = new XY(0, 0);


        public static final XY[] DIRECTIONS = new XY[]{
                new XY(1, 1),
                new XY(-1, -1),
                new XY(1, -1),
                new XY(-1, 1),
        };

        final int x;
        final int y;

        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public XY copy() {
            return new XY(x, y);
        }

        public XY createMove(int dx, int dy) {
            return new XY(x + dx, y + dy);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XY xy = (XY) o;
            return x == xy.x && y == xy.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "XY{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    public static class Level {
        int itemInField;
        int itemInFieldDestroyers;
        int itemOutField;
        double velocityInField;
        double velocityOutField;
        double levelThreshold;

        public Level(int itemInField, int itemInFieldDestroyers, int itemOutField, double velocityInField, double velocityOutField, double levelThreshold) {
            this.itemInField = itemInField;
            this.itemInFieldDestroyers = itemInFieldDestroyers;
            if (itemInField<itemInFieldDestroyers) {
                throw new IllegalArgumentException("itemInFieldDestroyers");
            }
            this.itemOutField = itemOutField;
            this.velocityInField = velocityInField;
            this.velocityOutField = velocityOutField;
            this.levelThreshold = levelThreshold;
        }
    }

    public static class Bonus {
        XY pos;
        BonusType type;
        XY size;
        long lastTick;
    }

}
