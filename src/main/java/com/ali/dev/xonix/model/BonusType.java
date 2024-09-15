package com.ali.dev.xonix.model;

import com.ali.dev.xonix.Config;
import com.ali.dev.xonix.Images;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum BonusType {
    LIFE(s -> s.lifes++, s -> {
    }, Images.bonusLife, false, (s, b) -> {}),
    HEAD_SPEED_UP(s -> s.head.velocity = 2, s -> s.head.velocity = 1, Images.speedUp, true, (s, b) -> {}),
    FREEZE(s -> s.items.stream().filter(i -> i.area == ItemAreaType.InField).forEach(Item::slowDown),
            s -> s.items.stream().filter(i -> i.area == ItemAreaType.InField).forEach(Item::restore),
            Images.freeze, true, (s, b) -> {}),
    // todo: incompleted implementation
    BOMB(s -> {},
            s -> {},
            Images.bomb, false,
            (s, b) -> {
                for (int y = 0; y < Config.GRID_SIZE_Y; y++) {
                    for (int x = 0; x < Config.GRID_SIZE_X; x++) {
                        if (s.entityGrid[y][x] == EntityType.BLOCK) {
                            int r = (int) (Math.pow(b.pos.x - x, 2) + Math.pow(b.pos.y - y, 2));
                            if (r <= 100) {
                                s.entityGrid[y][x] = EntityType.FREE;
                            }
                        }
                    }
                }
                s.updateProgress();
            });

    public final Consumer<State> apply;
    public final Consumer<State> reject;
    public final BiConsumer<State, Bonus> onExpire;
    public final Image image;
    public final boolean durable;

    BonusType(Consumer<State> apply, Consumer<State> reject, Image image, boolean durable, BiConsumer<State, Bonus> onExpire) {
        this.apply = apply;
        this.reject = reject;
        this.image = image;
        this.durable = durable;
        this.onExpire = onExpire;
    }
}
