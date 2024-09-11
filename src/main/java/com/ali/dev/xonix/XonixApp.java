package com.ali.dev.xonix;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static com.ali.dev.xonix.Config.*;

public class XonixApp extends JFrame implements Engine.GameOverListener {
    private static JFrame splashFrame;
    private final KeyboardInput keyboard = new KeyboardInput();
    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;
    private final State state;
    private final Engine engine;
    private final Timer timer;
    private final StringBuilder nameInput = new StringBuilder().append(YOU_NAME);

    public XonixApp() throws IOException {
        setTitle("Xonix");
        setSize(Config.WIDTH, Config.HEIGHT + 60); // Adjusted for the new panel position
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        java.util.List<Level> levels = readLevels("levels.json");

        state = new State(new EntityType[GRID_SIZE_Y][GRID_SIZE_X], levels);
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
                if (state.gameOver) {
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

    private java.util.List<Level> readLevels(String path) throws IOException {
        ClassLoader classLoader = Images.class.getClassLoader();

        // Get the resource as an InputStream
        InputStream inputStream = classLoader.getResourceAsStream(path);

        if (inputStream == null) {
            throw new IllegalArgumentException("Image not found: " + path);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        java.util.List<Level> levels = objectMapper.readValue(inputStream,
                objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, Level.class));
        System.out.println("read levels: " + levels.size());
        return levels;
    }

    private void processEscapeKey() {
        state.gameOver = false;
        state.enterName = false;
        state.lifes = INIT_LIFES;
        // open the same level
        state.curLevel--;
        state.nextLevel();
    }

    private void processEnterName() {
        if (nameInput.length() > YOU_NAME.length() + 3) {
            state.addScore(nameInput.substring(YOU_NAME.length()));
            nameInput.delete(0, nameInput.length());
            state.enterName = false;
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

    public static void main(String[] args) throws InterruptedException {

        createSplashScreen();
        Thread.sleep(3000);
        splashFrame.dispose();

        SwingUtilities.invokeLater(() -> {
            XonixApp app = null;
            try {
                app = new XonixApp();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            app.setVisible(true);
        });
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

        state.bonuses.forEach(b -> {
            bufferGraphics.drawImage(b.type.image, calcX(b.pos.x), calcY(b.pos.y), null);
        });

        // Draw the items
        for (Item item : state.items) {
            bufferGraphics.setColor(calcColor(item.area, item.type));
            bufferGraphics.fillOval((int) item.currentX + CELL_SIZE / 4, (int) item.currentY + CELL_SIZE / 4, 3 * CELL_SIZE / 4, 3 * CELL_SIZE / 4);
        }

        // Draw the highlighted cell
        highLightCell(Color.YELLOW, state.head.pos.x, state.head.pos.y);

        printStatus();

        if (state.isPause) {
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.drawString("Paused", Config.WIDTH / 2 - 20, Config.HEIGHT / 2);
        }

        if (state.isReadyForNewLevel) {
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.setFont(TIMER_FONT);
            String mes = "Next Level: ";
            bufferGraphics.drawString(mes + (state.curLevel + 2), Config.WIDTH / 2 - mes.length() * bufferGraphics.getFont().getSize() / 3, Config.HEIGHT / 2);
        }

        bufferGraphics.setColor(Color.cyan);
        state.getCurLevel().sliders.forEach(sl -> {
            // Устанавливаем стиль линии
            bufferGraphics.setStroke(DASHED_STROKE);
            bufferGraphics.drawRect(sl.x, sl.y, sl.width, sl.height);
        });

        if (state.gameOver) {
            paintGameOverArea(bufferGraphics);
        }

        // Draw the buffer on the screen
        g.drawImage(buffer, 0, 0, this);
        int timePaint = (int) (System.currentTimeMillis() - start);
        if (timePaint > 10) {
            System.out.println("time paint: " + timePaint + " ms");
        }
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
        for (int i = 0; i < state.topScores.size(); i++) {
            var score = state.topScores.get(i);
            bufferGraphics.drawString((i + 1) + ". " + score.getName() + ": " + score.getScore(), inputX, yOffset);
            yOffset += 40;
        }


        if (state.enterName) {

            bufferGraphics.setColor(Color.YELLOW);
            bufferGraphics.drawString(nameInput.toString(), inputX, yOffset);
        }
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
        bufferGraphics.drawString("Lifes: " + state.lifes, 20, 60);
        bufferGraphics.drawString("Score: " + state.score, 120, 60);
        bufferGraphics.drawString("Progress: " + String.format("%6.2f", state.progress * 100), 450, 60);
        bufferGraphics.drawString("Target: " +
                String.format("%6.2f", state.getCurLevel().levelThreshold), 650, 60);

        state.activeBonuses.forEach(b -> {
            bufferGraphics.drawImage(b.type.image, calcX(GRID_SIZE_X - 5 + b.type.ordinal()), calcY(0) - 2 * CELL_SIZE, null);
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