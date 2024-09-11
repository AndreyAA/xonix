package com.ali.dev.xonix;

public class Rect {
    int x;
    int y;
    int width;
    int height;

    public Rect() {

    }

    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean contains(int x0, int y0) {
        return (x0 >= x && x0 <= x + width && y0 >= y && y0 <= y + height);
    }

}
