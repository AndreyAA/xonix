package com.ali.dev.xonix.model;

public class Bonus {
    public XY pos;
    public BonusType type;
    public XY size;
    public long lastTick;
    public int frame = 0;

    public XY getPos() {
        return pos;
    }

    public BonusType getType() {
        return type;
    }

    public XY getSize() {
        return size;
    }

    public long getLastTick() {
        return lastTick;
    }

    public void incFrame() {
        frame++;
    }

    public int getFrame() {
        return frame;
    }
}
