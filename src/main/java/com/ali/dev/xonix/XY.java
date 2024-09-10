package com.ali.dev.xonix;

import java.util.Objects;

public class XY {
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
