package com.ali.dev.xonix;

public class Rect {
    final int x;
    final int y;
    final int width;
    final int height;

    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(int x0, int y0) {
        return (x0 >= x && x0 <= x + width && y0 >= y && y0 <= y + height);
    }

}
