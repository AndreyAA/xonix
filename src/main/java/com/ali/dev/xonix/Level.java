package com.ali.dev.xonix;

import java.util.List;

public class Level {
    int itemInField;
    int itemInFieldDestroyers;
    int itemOutField;
    double velocityInField;
    double velocityOutField;
    double levelThreshold;
    List<Rect> sliders;

    public Level(int itemInField, int itemInFieldDestroyers, int itemOutField,
                 double velocityInField, double velocityOutField,
                 double levelThreshold, List<Rect> sliders) {
        this.itemInField = itemInField;
        this.itemInFieldDestroyers = itemInFieldDestroyers;
        if (itemInField < itemInFieldDestroyers) {
            throw new IllegalArgumentException("itemInFieldDestroyers");
        }
        this.itemOutField = itemOutField;
        this.velocityInField = velocityInField;
        this.velocityOutField = velocityOutField;
        this.levelThreshold = levelThreshold;
        this.sliders = sliders;
    }
}
