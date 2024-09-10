package com.ali.dev.xonix;

import java.awt.*;

public class Config {
    static final int TICK_TIME_MS = 16;
    public static  final int TIME_FOR_BONUS_MS = 5000;
    public static  final int BONUS_LIVE_MS = 10000;

    static final int INIT_LIFES = 3;
    static final int GRID_SIZE_X = 108;
    static final int GRID_SIZE_Y = 80;
    static final int CELL_SIZE = 10;
    static final int WIDTH = CELL_SIZE * GRID_SIZE_X + CELL_SIZE + 20;
    static final int HEIGHT = CELL_SIZE * GRID_SIZE_Y + 3 * CELL_SIZE;//600; //900
    static final int HALF_CELL = CELL_SIZE / 2;
    static final int MIN_Y = 70; // Adjusted for the new panel position
    static final int MIN_X = 15;
    static final int BUTTON_DELAY_MS = 500;
    static final Font STATUS_FONT = new Font("Monospaced", Font.PLAIN, 16);
    static final Font CELL_FONT = new Font("Monospaced", Font.PLAIN, 30);
    static final Font TIMER_FONT = new Font("Monospaced", Font.PLAIN, 100);
    static final Color CLEAR_COLOR = Color.BLACK;
    static final Color STATUS_COLOR = Color.WHITE;

    static final String YOU_NAME = "Your name: ";
}
