package com.ali.dev.xonix.model;

import java.util.List;

public class Level {
    int id;
    double target;
    int bonusSpawnSec;
    List<ItemModel> items;
    List<AreaModel> areas;

    public int getId() {
        return id;
    }

    public int getBonusSpawnSec() {
        return bonusSpawnSec;
    }

    public double getTarget() {
        return target;
    }

    public List<ItemModel> getItems() {
        return items;
    }

    public List<AreaModel> getAreas() {
        return areas;
    }

    public static class ItemModel {
        String type;
        int count;
        double velocity;

        public String getType() {
            return type;
        }

        public int getCount() {
            return count;
        }

        public double getVelocity() {
            return velocity;
        }
    }

    public static class AreaModel {
        String type;
        int x, y, width, height;

        public String getType() {
            return type;
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

}
