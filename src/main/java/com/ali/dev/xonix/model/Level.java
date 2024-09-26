package com.ali.dev.xonix.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;
import java.util.Set;

public class Level {
    int id;
    double target;
    int bonusSpawnSec;
    @JsonSetter(nulls = Nulls.SKIP)
    List<String> availableBonuses = List.of();
    @JsonSetter(nulls = Nulls.SKIP)
    List<ItemModel> items = List.of();
    @JsonSetter(nulls = Nulls.SKIP)
    List<AreaModel> areas = List.of();

    public int getId() {
        return id;
    }

    public List<String> getAvailableBonuses() {
        return availableBonuses;
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
        Set<AreaFeature> features;
        int x, y, width, height;

        public Set<AreaFeature> getFeatures() {
            return features;
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
