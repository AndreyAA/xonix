package com.ali.dev.xonix;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static com.ali.dev.xonix.Config.*;

public class XonixApp extends JFrame {

    private final KeyboardInput keyboard = new KeyboardInput();
    private final BufferedImage buffer;
    private final Graphics2D bufferGraphics;
    private final State state;
    private final Engine engine;
    private final Timer timer;

    public XonixApp() throws IOException {
        setTitle("Xonix");
        setSize(Config.WIDTH, Config.HEIGHT + 60); // Adjusted for the new panel position
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
//        this.state = Serializator.load("towersStateProd");
        state = new State(null, new EntityType[GRID_SIZE_Y][GRID_SIZE_X]);
        state.initData();
        buffer = new BufferedImage(Config.WIDTH, Config.HEIGHT + 60, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();
        bufferGraphics.setBackground(CLEAR_COLOR);

        engine = new Engine(state, keyboard);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                state.mouseX = e.getX();
                state.mouseY = e.getY();
                state.highlightedRow = (e.getY() - MIN_Y) / CELL_SIZE;
                state.highlightedCol = (e.getX() - MIN_X) / CELL_SIZE;
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    state.mouseEvent = e;
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    state.mouseEvent2 = e;
                }
            }
        });
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

        addKeyListener(keyboard);

        timer = new Timer(Config.TICK_TIME_MS, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                engine.tick();
                repaint();
            }
        });
        timer.start();

        setFocusable(true);
        requestFocus();

    }


    private static void paintRectSize(Graphics2D g2d, int x, int y, int size) {
        g2d.fillRect(x, y, CELL_SIZE * size, CELL_SIZE * size);
    }

    private static void paintRect(Graphics2D g2d, int x, int y, int character) {
        paintRect(g2d, x, y, character, CELL_SIZE - 4);
    }

    private static void paintRect(Graphics2D g2d, int x, int y, int character, int height) {

        g2d.fillRect(x + 2, y + 2, CELL_SIZE - 4, height);
        if (character > 0) {
            Color c = g2d.getColor();
            g2d.setColor(Color.BLACK);
            g2d.setFont(CELL_FONT);
            g2d.drawString(Character.toString(character), x + CELL_SIZE / 4, y + 3 * CELL_SIZE / 4);
            g2d.setColor(c);
        }
    }

    public static void main(String[] args) {
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

    public static int calcCenterX(int col) {
        return col * CELL_SIZE + MIN_X + HALF_CELL;
    }

    public static int calcY(int row) {
        return row * CELL_SIZE + MIN_Y;
    }

    public static int calcCenterY(int row) {
        return row * CELL_SIZE + MIN_Y + HALF_CELL;
    }

    @Override
    public void paint(Graphics g) {
        long start = System.currentTimeMillis();
        // Clear the buffer
//        bufferGraphics.setBackground(new Color(0, 0, 0, 0));
        bufferGraphics.clearRect(0, 0, Config.WIDTH, Config.HEIGHT + 120);

        // Draw the grid
        for (int row = 0; row < GRID_SIZE_Y; row++) {
            for (int col = 0; col < GRID_SIZE_X; col++) {
                if (state.entityGrid[row][col] != null) {
                    bufferGraphics.setColor(Color.WHITE);
                    drawShape(bufferGraphics, row, col, state.entityGrid[row][col]);
                }
                //highLightCell(Color.GRAY, col, row);
            }
        }


        //draw highlight for button
        // highLightCell(Color.YELLOW, GRID_SIZE_X / 2 + state.activeButton, GRID_SIZE_Y);


        // Draw the start and end points
        bufferGraphics.setColor(Color.BLUE);
        state.bonuses.forEach(b -> {
            bufferGraphics.drawImage(b.type.image, calcX(b.pos.x), calcY(b.pos.y), null);
        });


/*        bufferGraphics.setColor(Color.RED);
        paintRect(bufferGraphics, calcX(state.endCol), calcY(state.endRow), 0);*/

        // Draw the items

        for (Item item : state.items) {
            bufferGraphics.setColor(calcColor(item.type));
            bufferGraphics.fillOval((int) item.currentX + CELL_SIZE / 4, (int) item.currentY + CELL_SIZE / 4, 3 * CELL_SIZE / 4, 3 * CELL_SIZE / 4);
        }

        // Draw the highlighted cell
        highLightCell(Color.YELLOW, state.head.pos.x, state.head.pos.y);

        // Draw the score

        printStatus();
//        printWaitWave();
        // active buttons
//        printActiveButtons();

        if (state.isPause) {
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.drawString("Paused", Config.WIDTH / 2 - 20, Config.HEIGHT / 2);
        }

        if (state.isReadyForNewLevel) {
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.setFont(TIMER_FONT);
            String mes = "Next Level: ";
            bufferGraphics.drawString(mes + (state.curLevel+2), Config.WIDTH / 2 - mes.length()*bufferGraphics.getFont().getSize()/3, Config.HEIGHT / 2);
        }

        // Draw the buffer on the screen
        g.drawImage(buffer, 0, 0, this);
        int timePaint = (int) (System.currentTimeMillis() - start);
        if (timePaint > 10) {
            System.out.println("time paint: " + timePaint + " ms");
        }
    }

    private Color calcColor(ItemType type) {
        return type == ItemType.InField ? Color.ORANGE : Color.RED;
    }

    private void highLightCell(Color color, int col, int row) {
        bufferGraphics.setColor(color);
        bufferGraphics.drawRect(calcX(col), calcY(row), CELL_SIZE, CELL_SIZE);
    }

    private void printStatus() {
        bufferGraphics.setFont(STATUS_FONT);
        bufferGraphics.setColor(STATUS_COLOR);
        bufferGraphics.drawString("Lifes: " + state.lifes, 20, 60);
        bufferGraphics.drawString("Remain: " + state.breakItems, 120, 60);
//        bufferGraphics.drawString("Money: " + state.money, 240, 60);
        bufferGraphics.drawString("Score: " + state.score, 240, 60);
        bufferGraphics.drawString("Progress: " + String.format("%6.2f", state.progress * 100), 450, 60);
        bufferGraphics.drawString("Target: " +
                String.format("%6.2f", state.getCurLevel().levelThreshold), 650, 60);

        state.activeBonuses.forEach(b->{
            bufferGraphics.drawImage(b.type.image, calcX(GRID_SIZE_X-5 + b.type.ordinal()), calcY(0)-2*CELL_SIZE, null);
        });
    }

    private void drawShape(Graphics2D g2d, int row, int col, EntityType entityType) {
        int x = calcX(col);
        int y = calcY(row);
        g2d.setColor(Color.WHITE);

        switch (entityType) {
            case BORDER:
                g2d.setColor(Color.GRAY);
                paintRect(g2d, x, y, 0);
                break;
        }
    }

}