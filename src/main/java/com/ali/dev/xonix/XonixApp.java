package com.ali.dev.xonix;

import com.ali.dev.xonix.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;

import static com.ali.dev.xonix.Config.*;

public class XonixApp extends JFrame implements GameOverListener {
    private static final Logger log = LoggerFactory.getLogger(XonixApp.class);
    private static final int HUD_HEIGHT = 60;
    private static final int HUD_PANEL_X = 16;
    private static final int HUD_FIELD_GAP = 6;
    private static final int HUD_PANEL_Y = 6;
    private static final int HUD_PANEL_HEIGHT = MIN_Y - HUD_PANEL_Y - HUD_FIELD_GAP;
    private static final int HUD_PANEL_ARC = 20;
    private static final Color HUD_PANEL_COLOR = new Color(12, 16, 22, 215);
    private static final Color HUD_BORDER_COLOR = new Color(255, 255, 255, 70);
    private static final Color HUD_MUTED_COLOR = new Color(173, 186, 199);
    private static final Color HUD_ACCENT_COLOR = new Color(74, 226, 196);
    private static final Color HUD_TARGET_COLOR = new Color(255, 196, 77);
    private static final Color HUD_DANGER_COLOR = new Color(255, 96, 96);
    private static final Color HUD_BONUS_BG = new Color(255, 255, 255, 28);
    private static final Dimension LOGICAL_SCREEN_SIZE = new Dimension(Config.WIDTH, Config.HEIGHT + HUD_HEIGHT);
    private static JFrame splashFrame;
    private final KeyboardInput keyboard = new KeyboardInput();
    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;
    private final GamePanel gamePanel;
    private final State state;
    private final Engine engine;
    private final Timer timer;
    private final StringBuilder nameInput = new StringBuilder().append(YOU_NAME);

    public XonixApp(java.util.List<Level> levels, int curLevel) throws IOException {
        setTitle("Xonix");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        state = new State(new EntityType[GRID_SIZE_Y][GRID_SIZE_X], levels);
        state.setCurLevel(curLevel);
        state.readScores();
        state.initData();

        buffer = new BufferedImage(LOGICAL_SCREEN_SIZE.width, LOGICAL_SCREEN_SIZE.height, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setBackground(CLEAR_COLOR);
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(LOGICAL_SCREEN_SIZE);
        setContentPane(gamePanel);

        engine = new Engine(state, keyboard, this);

        startKeyboardThread();
        registerInputHandlers(gamePanel);

        timer = new Timer(Config.TICK_TIME_MS, e -> {
            SwingUtilities.invokeLater(engine::tick);
            gamePanel.repaint();
        });
        timer.start();

        pack();
        setMinimumSize(LOGICAL_SCREEN_SIZE);
        setLocationRelativeTo(null);
    }

    private void registerInputHandlers(JComponent component) {
        component.setFocusable(true);
        component.addKeyListener(keyboard);
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (state.isGameOver()) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        processEnterName();
                    } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && nameInput.length() > YOU_NAME.length()) {
                        nameInput.deleteCharAt(nameInput.length() - 1);
                    } else if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED && Character.isLetterOrDigit(e.getKeyChar())) {
                        nameInput.append(e.getKeyChar());
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        processEscapeKey();
                    }
                }
            }
        });
    }

    private static java.util.List<Level> readLevels(String path, InputStream inputStream ) throws IOException {

        if (inputStream == null) {
            throw new IllegalArgumentException("file not found: " + path);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        java.util.List<Level> levels = objectMapper.readValue(inputStream,
                objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, Level.class));
        log.info("read levels: {}", levels.size());
        return levels;
    }

    private void processEscapeKey() {
        state.setGameOver(false);
        state.setEnterName(false);
        state.setLifes(INIT_LIFES);
        state.thisLevel();
    }

    private void processEnterName() {
        if (nameInput.length() >= YOU_NAME.length() + NAME_MIN_LENGTH) {
            state.addScore(nameInput.substring(YOU_NAME.length()));
            nameInput.delete(0, nameInput.length());
            state.setEnterName(false);
            try {
                state.storeScores();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void startKeyboardThread() {
        Runnable keyboardThread = () -> {
            while (true) {
                keyboard.poll();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(keyboardThread).start();
    }


    private static void paintRect(Graphics2D g2d, int x, int y) {
        g2d.fillRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        Options options = new Options();

        options.addOption(Option.builder("l")
                .longOpt("level")
                .hasArg()
                .argName("level")
                .desc("active level")
                .build());

        options.addOption(Option.builder("s")
                .longOpt("settings")
                .hasArg()
                .argName("filePath")
                .desc("Path to the source file")
                .build());

        // Создаем объект CommandLineParser
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        int level = 0;
        String filePath = null;

        try {
            // Парсим аргументы командной строки
            cmd = parser.parse(options, args);

            // Обрабатываем опции
            if (cmd.hasOption("l")) {
                String lengthStr = cmd.getOptionValue("l");
                level = Integer.parseInt(lengthStr);
            }

            if (cmd.hasOption("s")) {
                filePath = cmd.getOptionValue("s");
            }
        } catch (ParseException e) {
            System.out.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp("CommandLineApp", options);
        }

        InputStream inputStream = getLevelsInputStream(filePath);
        java.util.List<Level> levels;
        try {
            levels = readLevels("", inputStream);
        } finally {
            inputStream.close();
        }

        createSplashScreen();
        Thread.sleep(SPLASH_SCREEN_DELAY);
        splashFrame.dispose();

        int curLevel = level;
        SwingUtilities.invokeLater(() -> {
            XonixApp app = null;
            try {
                app = new XonixApp(levels, curLevel);
                app.setVisible(true);
                app.gamePanel.requestFocusInWindow();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static InputStream getLevelsInputStream(String filePath) throws FileNotFoundException {
        InputStream inputStream;
        if (filePath!=null) {
            inputStream = new FileInputStream(filePath);
            log.info("load levels from file: {}", filePath);
        } else {
            ClassLoader classLoader = Images.class.getClassLoader();
            // Get the resource as an InputStream
             inputStream = classLoader.getResourceAsStream(LEVELS_PATH);
        }
        return inputStream;
    }

    public static int calcX(int col) {
        return col * CELL_SIZE + MIN_X;
    }

    public static int calcCol(double x) {
        return (int) ((x - MIN_X) / CELL_SIZE);
    }

    public static int calcRow(double y) {
        return (int) ((y - MIN_Y) / CELL_SIZE);
    }

    public static int calcY(int row) {
        return row * CELL_SIZE + MIN_Y;
    }

    private void renderFrame() {
        long start = System.currentTimeMillis();
        bufferGraphics.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());

        for (int row = 0; row < GRID_SIZE_Y; row++) {
            for (int col = 0; col < GRID_SIZE_X; col++) {
                if (state.entityGrid[row][col] != null) {
                    bufferGraphics.setColor(Color.WHITE);
                    drawShape(bufferGraphics, row, col, state.entityGrid[row][col]);
                }
            }
        }

        state.getBonuses().forEach(b -> {
            if (needPaint(b)) {
                bufferGraphics.drawImage(b.type.image, calcX(b.pos.getX()), calcY(b.pos.getY()), null);
            }
        });

        // Draw the items
        for (Item item : state.getItems()) {
            bufferGraphics.setColor(calcColor(item.getArea(), item.getType()));
            bufferGraphics.fillOval((int) item.getCurrentX() + CELL_SIZE / 4, (int) item.getCurrentY() + CELL_SIZE / 4, 3 * CELL_SIZE / 4, 3 * CELL_SIZE / 4);
        }

        // Draw the highlighted cell
        highLightCell(Color.YELLOW, state.getHead().getPos().getX(), state.getHead().getPos().getY());

        // Устанавливаем стиль линии
        switchOnSlidersStrikes(bufferGraphics);
        state.getCurLevel().getAreas().stream().filter(a->a.getType().equals("slider")).forEach(sl -> {
            bufferGraphics.drawRect(sl.getX(), sl.getY(), sl.getWidth(), sl.getHeight());
        });
        switchOffSlidersStrikes(bufferGraphics);
        int timePaint = (int) (System.currentTimeMillis() - start);
        if (timePaint > 10) {
            log.debug("time paint: {} ms", timePaint);
        }
    }

    private void switchOnSlidersStrikes(Graphics2D graphics) {
        graphics.setColor(Color.cyan);
        graphics.setStroke(DASHED_STROKE);
    }

    private void switchOffSlidersStrikes(Graphics2D graphics) {
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke());
    }

    private boolean needPaint(Bonus b) {
        return b.getFrame() % 2 == 0 || (b.lastTick - state.getTickId()) > BONUS_STRART_BLINK_MS / TICK_TIME_MS;
    }

    private void paintGameOverArea(Graphics2D bufferGraphics) {
        // Рисуем сообщение "Game Over" и таблицу игроков
        bufferGraphics.setColor(Color.BLACK);

        bufferGraphics.fillRect(200, 200, Config.WIDTH - 400, 500);
        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.setFont(new Font("Arial", Font.BOLD, 48));
        bufferGraphics.drawString("Game Over", 400, 200);

        bufferGraphics.setFont(new Font("Arial", Font.PLAIN, 24));
        int yOffset = 300;
        int inputX = 450;
        for (int i = 0; i < state.getTopScores().size(); i++) {
            var score = state.getTopScores().get(i);
            bufferGraphics.drawString((i + 1) + ". " + score.getName() + ": " + score.getScore(), inputX, yOffset);
            yOffset += 40;
        }


        if (state.isEnterName()) {
            bufferGraphics.setColor(Color.YELLOW);
            bufferGraphics.drawString(nameInput.toString(), inputX, yOffset);
        }
    }

    private void paintPauseArea(Graphics2D bufferGraphics) {
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.fillRect(200, 120, Config.WIDTH - 400, 500);
        bufferGraphics.setColor(Color.GRAY);
        bufferGraphics.drawRect(200, 120, Config.WIDTH - 400, 500);

        bufferGraphics.setColor(Color.WHITE);
        bufferGraphics.setFont(new Font("Arial", Font.BOLD, 48));
        bufferGraphics.drawString("Pause", 470, 180);

        bufferGraphics.setFont(new Font("Arial", Font.PLAIN, 24));
        int yOffset = 210;
        int inputX = 250;
        int i = 1;
        int IMAGE_SHIFT = 17;
        int SIZE = 40;
        bufferGraphics.drawString("Controls:", inputX, yOffset + SIZE * i++);
        bufferGraphics.drawString("left, right, up, down ", inputX, yOffset + SIZE * i++);
        bufferGraphics.drawString("space: pause", inputX, yOffset + SIZE * i++);
        bufferGraphics.drawString("ESC: return", inputX, yOffset + SIZE * i++);
        i++;
        bufferGraphics.drawString("Enemies:", inputX, yOffset + SIZE * i++);
        paintLegendBall(bufferGraphics, inputX, yOffset, i, IMAGE_SHIFT, ItemAreaType.InField, ItemType.STD);
        bufferGraphics.drawString("standard", inputX + 30, yOffset + SIZE * i++);

        paintLegendBall(bufferGraphics, inputX, yOffset, i, IMAGE_SHIFT, ItemAreaType.InField, ItemType.DESTROYER);
        bufferGraphics.drawString("destroyer", inputX + 30, yOffset + SIZE * i++);

        paintLegendBall(bufferGraphics, inputX, yOffset, i, IMAGE_SHIFT, ItemAreaType.OutFiled, ItemType.STD);
        bufferGraphics.drawString("ground", inputX + 30, yOffset + SIZE * i++);

        i = 1;
        inputX = 620;

        bufferGraphics.drawString("Areas:", inputX, yOffset + SIZE * i++);

        switchOnSlidersStrikes(bufferGraphics);
        bufferGraphics.drawRect(inputX, yOffset + SIZE * i - IMAGE_SHIFT, 20, 20);
        switchOffSlidersStrikes(bufferGraphics);
        bufferGraphics.drawString("unstoppable", inputX + 30, yOffset + SIZE * i++);

        i += 3;
        bufferGraphics.drawString("Bonuses:", inputX, yOffset + SIZE * i++);

        bufferGraphics.drawImage(BonusType.LIFE.image, inputX, yOffset + SIZE * i - IMAGE_SHIFT, null);
        bufferGraphics.drawString("life", inputX + 30, yOffset + SIZE * i++);

        bufferGraphics.drawImage(BonusType.HEAD_SPEED_UP.image, inputX, yOffset + SIZE * i - IMAGE_SHIFT, null);
        bufferGraphics.drawString("speed up", inputX + 30, yOffset + SIZE * i++);

        bufferGraphics.drawImage(BonusType.SLOW_DOWN.image, inputX, yOffset + SIZE * i - IMAGE_SHIFT, null);
        bufferGraphics.drawString("slow down", inputX + 30, yOffset + SIZE * i++);

        bufferGraphics.drawImage(BonusType.BOMB.image, inputX, yOffset + SIZE * i - IMAGE_SHIFT, null);
        bufferGraphics.drawString("bomb", inputX + 30, yOffset + SIZE * i++);
    }

    private void paintLegendBall(Graphics2D bufferGraphics, int inputX, int yOffset, int i, int IMAGE_SHIFT, ItemAreaType itemAreaType, ItemType itemType) {
        bufferGraphics.setColor(calcColor(itemAreaType, itemType));
        bufferGraphics.fillOval(inputX, yOffset + 40 * i - IMAGE_SHIFT, 2 * CELL_SIZE, 2 * CELL_SIZE);
        bufferGraphics.setColor(Color.WHITE);
    }

    private Color calcColor(ItemAreaType type, ItemType itemType) {
        if (itemType == ItemType.DESTROYER) {
            return Color.YELLOW;
        }
        return type == ItemAreaType.InField ? Color.WHITE : Color.RED;
    }

    private void highLightCell(Color color, int col, int row) {
        bufferGraphics.setColor(color);
        bufferGraphics.drawRect(calcX(col), calcY(row), CELL_SIZE, CELL_SIZE);
    }

    private void printStatus() {
        printStatus(bufferGraphics);
    }

    private void printStatus(Graphics2D graphics) {
        paintHud(graphics);
    }

    private void paintOverlay(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        printStatus(graphics);

        if (state.isReadyForNewLevel()) {
            graphics.setColor(Color.WHITE);
            graphics.setFont(TIMER_FONT);
            String mes = "Next Level: ";
            graphics.drawString(mes + (state.getCurLevelNumber() + 2),
                    Config.WIDTH / 2 - mes.length() * graphics.getFont().getSize() / 3,
                    Config.HEIGHT / 2);
        }

        if (state.isGameOver()) {
            paintGameOverArea(graphics);
        }

        if (state.isPause()) {
            paintPauseArea(graphics);
        }
    }

    private void paintHud(Graphics2D graphics) {
        int panelWidth = Config.WIDTH - HUD_PANEL_X * 2;
        graphics.setColor(HUD_PANEL_COLOR);
        graphics.fillRoundRect(HUD_PANEL_X, HUD_PANEL_Y, panelWidth, HUD_PANEL_HEIGHT, HUD_PANEL_ARC, HUD_PANEL_ARC);
        graphics.setColor(HUD_BORDER_COLOR);
        graphics.drawRoundRect(HUD_PANEL_X, HUD_PANEL_Y, panelWidth, HUD_PANEL_HEIGHT, HUD_PANEL_ARC, HUD_PANEL_ARC);

        paintLivesBlock(graphics, HUD_PANEL_X + 18, HUD_PANEL_Y + 16);
        paintScoreBlock(graphics, HUD_PANEL_X + 220, HUD_PANEL_Y + 14);
        paintObjectiveBlock(graphics, HUD_PANEL_X + 520, HUD_PANEL_Y + 16);
        paintBonusesBlock(graphics, Config.WIDTH - 248, HUD_PANEL_Y + 12);
    }

    private void paintLivesBlock(Graphics2D graphics, int x, int y) {
        drawLabel(graphics, "LIVES", x, y);
        for (int i = 0; i < state.getLifes(); i++) {
            graphics.drawImage(BonusType.LIFE.image, x + i * 24, y + 10, 18, 18, null);
        }
        graphics.setFont(HUD_SMALL_FONT);
        graphics.setColor(state.getLifes() <= 1 ? HUD_DANGER_COLOR : HUD_MUTED_COLOR);
        graphics.drawString("LEVEL " + (state.getCurLevelNumber() + 1), x, y + 42);
        graphics.drawString(state.getLifes() + " remaining", x + 88, y + 25);
    }

    private void paintScoreBlock(Graphics2D graphics, int x, int y) {
        drawLabel(graphics, "SCORE", x, y);
        graphics.setFont(HUD_SCORE_FONT);
        graphics.setColor(STATUS_COLOR);
        graphics.drawString(String.valueOf(state.getScore()), x, y + 30);
        paintProgressBar(graphics, x, y + 40, 250, 12);
    }

    private void paintObjectiveBlock(Graphics2D graphics, int x, int y) {
        drawLabel(graphics, "OBJECTIVE", x, y);
        graphics.setFont(HUD_VALUE_FONT);
        graphics.setColor(HUD_ACCENT_COLOR);
        graphics.drawString(String.format("%5.1f%%", state.getProgress() * 100), x, y + 24);
        graphics.setFont(HUD_SMALL_FONT);
        graphics.setColor(HUD_TARGET_COLOR);
        graphics.drawString("Target " + String.format("%4.1f%%", state.getCurLevel().getTarget()), x, y + 44);
    }

    private void paintBonusesBlock(Graphics2D graphics, int x, int y) {
        drawLabel(graphics, "ACTIVE", x, y);
        if (state.getActiveBonuses().isEmpty()) {
            graphics.setFont(HUD_SMALL_FONT);
            graphics.setColor(HUD_MUTED_COLOR);
            graphics.drawString("No active bonuses", x, y + 24);
            return;
        }

        int chipX = x;
        for (Bonus bonus : state.getActiveBonuses()) {
            if (!needPaint(bonus)) {
                continue;
            }
            paintBonusChip(graphics, chipX, y + 8, bonus);
            chipX += 72;
        }
    }

    private void paintBonusChip(Graphics2D graphics, int x, int y, Bonus bonus) {
        graphics.setColor(HUD_BONUS_BG);
        graphics.fillRoundRect(x, y, 64, 38, 14, 14);
        graphics.setColor(HUD_BORDER_COLOR);
        graphics.drawRoundRect(x, y, 64, 38, 14, 14);
        graphics.drawImage(bonus.type.image, x + 8, y + 9, 18, 18, null);

        int ticksLeft = Math.max(0, (int) (bonus.lastTick - state.getTickId()));
        double durationTicks = Math.max(1, state.getCurLevel().getBonusSpawnSec() * 1000.0 / TICK_TIME_MS);
        int barWidth = (int) Math.round(28 * Math.min(1.0, ticksLeft / durationTicks));

        graphics.setFont(HUD_SMALL_FONT);
        graphics.setColor(STATUS_COLOR);
        graphics.drawString(String.valueOf(ticksLeft * TICK_TIME_MS / 1000), x + 34, y + 20);
        graphics.setColor(HUD_MUTED_COLOR);
        graphics.drawString("s", x + 47, y + 20);
        graphics.setColor(HUD_ACCENT_COLOR);
        graphics.fillRoundRect(x + 28, y + 26, barWidth, 5, 5, 5);
    }

    private void paintProgressBar(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setColor(new Color(255, 255, 255, 26));
        graphics.fillRoundRect(x, y, width, height, height, height);

        int progressWidth = (int) Math.round(width * Math.max(0, Math.min(1, state.getProgress())));
        graphics.setColor(HUD_ACCENT_COLOR);
        graphics.fillRoundRect(x, y, progressWidth, height, height, height);

        int targetX = x + (int) Math.round(width * Math.max(0, Math.min(1, state.getCurLevel().getTarget() / 100.0)));
        graphics.setColor(HUD_TARGET_COLOR);
        graphics.fillRoundRect(targetX - 2, y - 2, 4, height + 4, 4, 4);

        graphics.setFont(HUD_SMALL_FONT);
        graphics.setColor(HUD_MUTED_COLOR);
        graphics.drawString(String.format("%.1f%% secured", state.getProgress() * 100), x, y + 25);
    }

    private void drawLabel(Graphics2D graphics, String label, int x, int y) {
        graphics.setFont(HUD_LABEL_FONT);
        graphics.setColor(HUD_MUTED_COLOR);
        graphics.drawString(label, x, y);
    }

    private void drawShape(Graphics2D g2d, int row, int col, EntityType entityType) {
        int x = calcX(col);
        int y = calcY(row);
        g2d.setColor(Color.WHITE);

        switch (entityType) {
            case BORDER:
                g2d.setColor(Color.DARK_GRAY);
                paintRect(g2d, x, y);
                break;
            case BLOCK:
                g2d.setColor(Color.GRAY);
                paintRect(g2d, x, y);
                break;
        }
    }

    @Override
    public void onGameOver() {
        // restore
        nameInput.delete(0, nameInput.length());
        nameInput.append(YOU_NAME);
    }

    private static void createSplashScreen() {
        splashFrame = new JFrame();
        splashFrame.setUndecorated(true);
        splashFrame.setSize(400, 300);
        splashFrame.setLocationRelativeTo(null);

        JPanel splashPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ImageIcon icon = new ImageIcon(Images.splash);
                g.drawImage(icon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };

        splashFrame.add(splashPanel);
        splashFrame.setVisible(true);
    }

    private final class GamePanel extends JPanel {

        private GamePanel() {
            setBackground(CLEAR_COLOR);
            setDoubleBuffered(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            renderFrame();

            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

                double scale = Math.min(
                        getWidth() / (double) LOGICAL_SCREEN_SIZE.width,
                        getHeight() / (double) LOGICAL_SCREEN_SIZE.height
                );
                int scaledWidth = Math.max(1, (int) Math.round(LOGICAL_SCREEN_SIZE.width * scale));
                int scaledHeight = Math.max(1, (int) Math.round(LOGICAL_SCREEN_SIZE.height * scale));
                int offsetX = (getWidth() - scaledWidth) / 2;
                int offsetY = (getHeight() - scaledHeight) / 2;

                g2d.drawImage(buffer, offsetX, offsetY, scaledWidth, scaledHeight, null);

                Graphics2D overlayGraphics = (Graphics2D) g2d.create();
                try {
                    overlayGraphics.translate(offsetX, offsetY);
                    overlayGraphics.scale(scale, scale);
                    paintOverlay(overlayGraphics);
                } finally {
                    overlayGraphics.dispose();
                }
            } finally {
                g2d.dispose();
            }
        }
    }
}
