package com.ali.dev.xonix;

import java.awt.*;

public class Config {
    public static String LEVELS_PATH = "levels.json";
    public static final int NAME_MIN_LENGTH = 3;
    public static final int SPLASH_SCREEN_DELAY = 3000;
    public static final int TIME_FOR_BONUS_MS = 5000;
    public static final int NEXT_LEVEL_WAIT_MS = 5000;
    public static final int BONUS_LIVE_MS = 10000;
    public static final int TICK_TIME_MS = 16;
    public static final int INIT_LIFES = 3;
    public static final int GRID_SIZE_X = 108;
    public static final int GRID_SIZE_Y = 80;
    public static final int CELL_SIZE = 10;
    public static final int WIDTH = CELL_SIZE * GRID_SIZE_X + CELL_SIZE + 20;
    public static final int HEIGHT = CELL_SIZE * GRID_SIZE_Y + 3 * CELL_SIZE;//600; //900
    public static final int HALF_CELL = CELL_SIZE / 2;
    public static final int MIN_Y = 70; // Adjusted for the new panel position
    public static final int MIN_X = 15;
    public static final int BUTTON_DELAY_MS = 500;
    public static final Font STATUS_FONT = new Font("Monospaced", Font.PLAIN, 16);
    public static final Font TIMER_FONT = new Font("Monospaced", Font.PLAIN, 100);
    public static final Color CLEAR_COLOR = Color.BLACK;
    public static final Color STATUS_COLOR = Color.WHITE;

    public static final String YOU_NAME = "Your name: ";
    public static final float[] DASH_PATTERN = {10, 5}; // 10 пикселей линия, 5 пикселей пробел
    public static final BasicStroke DASHED_STROKE = new BasicStroke(
            1,                      // Толщина линии
            BasicStroke.CAP_BUTT,   // Завершение линии
            BasicStroke.JOIN_BEVEL, // Соединение линий
            10.0f,                  // Мягкость соединения
            DASH_PATTERN,            // Шаблон прерывистой линии
            0.0f);                  // Смещение

}
