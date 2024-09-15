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
    private static JFrame splashFrame;
    private final KeyboardInput keyboard = new KeyboardInput();
    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;
    private final State state;
    private final Engine engine;
    private final Timer timer;
    private final StringBuilder nameInput = new StringBuilder().append(YOU_NAME);

    public XonixApp(java.util.List<Level> levels, int curLevel) throws IOException {
        setTitle("Xonix");
        setSize(Config.WIDTH, Config.HEIGHT + 60); // Adjusted for the new panel position
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        state = new State(new EntityType[GRID_SIZE_Y][GRID_SIZE_X], levels);
        state.setCurLevel(curLevel);
        state.readScores();
        state.initData();

        buffer = new BufferedImage(Config.WIDTH, Config.HEIGHT + 60, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setBackground(CLEAR_COLOR);

        engine = new Engine(state, keyboard, this);

        startKeyboardThread();
        addKeyListener(keyboard);

        addKeyListener(new KeyAdapter() {
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

        timer = new Timer(Config.TICK_TIME_MS, e -> {
            SwingUtilities.invokeLater(engine::tick);
            repaint();
        });
        timer.start();

        setFocusable(true);
        requestFocus();
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

    @Override
    public void paint(Graphics g) {
        long start = System.currentTimeMillis();
        // Clear the buffer
        bufferGraphics.clearRect(0, 0, Config.WIDTH, Config.HEIGHT + 120);

        // Draw the grid
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

        printStatus();

        if (state.isReadyForNewLevel()) {
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.setFont(TIMER_FONT);
            String mes = "Next Level: ";
            bufferGraphics.drawString(mes + (state.getCurLevelNumber() + 2), Config.WIDTH / 2 - mes.length() * bufferGraphics.getFont().getSize() / 3, Config.HEIGHT / 2);
        }

        // Устанавливаем стиль линии
        switchOnSlidersStrikes();
        state.getCurLevel().getAreas().stream().filter(a->a.getType().equals("slider")).forEach(sl -> {
            bufferGraphics.drawRect(sl.getX(), sl.getY(), sl.getWidth(), sl.getHeight());
        });
        switchOffSlidersStrikes();

        if (state.isGameOver()) {
            paintGameOverArea(bufferGraphics);
        }

        if (state.isPause()) {
            paintPauseArea(bufferGraphics);
        }

        // Draw the buffer on the screen
        g.drawImage(buffer, 0, 0, this);
        int timePaint = (int) (System.currentTimeMillis() - start);
        if (timePaint > 10) {
            log.debug("time paint: {} ms", timePaint);
        }
    }

    private void switchOnSlidersStrikes() {
        bufferGraphics.setColor(Color.cyan);
        bufferGraphics.setStroke(DASHED_STROKE);
    }

    private void switchOffSlidersStrikes() {
        bufferGraphics.setColor(Color.WHITE);
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
        bufferGraphics.fillRect(200, 120, Config.WIDTH - 400, 550);
        bufferGraphics.setColor(Color.GRAY);
        bufferGraphics.drawRect(200, 120, Config.WIDTH - 400, 550);

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

        switchOnSlidersStrikes();
        bufferGraphics.drawRect(inputX, yOffset + SIZE * i - IMAGE_SHIFT, 20, 20);
        switchOffSlidersStrikes();
        bufferGraphics.drawString("unstoppable", inputX + 30, yOffset + SIZE * i++);

        i += 3;
        bufferGraphics.drawString("Bonuses:", inputX, yOffset + SIZE * i++);

        for (BonusType type: BonusType.values()) {
            bufferGraphics.drawImage(type.image, inputX, yOffset + SIZE * i - IMAGE_SHIFT, null);
            bufferGraphics.drawString(type.name, inputX + 30, yOffset + SIZE * i++);
        }
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
        bufferGraphics.setFont(STATUS_FONT);
        bufferGraphics.setColor(STATUS_COLOR);
        bufferGraphics.drawString("Lifes: " + state.getLifes(), 20, 60);
        bufferGraphics.drawString("Score: " + state.getScore(), 120, 60);
        bufferGraphics.drawString("Progress: " + String.format("%6.2f", state.getProgress() * 100), 450, 60);
        bufferGraphics.drawString("Target: " +
                String.format("%6.2f", state.getCurLevel().getTarget()), 650, 60);

        state.getActiveBonuses().forEach(b -> {
            if (needPaint(b)) {
                bufferGraphics.drawImage(b.type.image, calcX(GRID_SIZE_X - 5 + b.type.ordinal()), calcY(0) - 2 * CELL_SIZE, null);
            }
        });
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
}