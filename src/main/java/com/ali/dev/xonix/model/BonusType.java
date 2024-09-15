package com.ali.dev.xonix.model;

import com.ali.dev.xonix.Config;
import com.ali.dev.xonix.Images;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum BonusType {
    LIFE("increase life",
            s -> s.lifes++,
            s -> {},
            Images.bonusLife,
            false,
            (s, b) -> {},
            true, true),
    HEAD_SPEED_UP("speed up head",
            s -> s.head.velocity = 2,
            s -> s.head.velocity = 1,
            Images.speedUp,
            true,
            (s, b) -> {},
            true, false),
    SLOW_DOWN("slow down enemies",
            s -> s.items.stream().filter(i -> i.area == ItemAreaType.InField).forEach(Item::slowDown),
            s -> s.items.stream().filter(i -> i.area == ItemAreaType.InField).forEach(Item::restore),
            Images.freeze,
            true,
            (s, b) -> {},
            true, false),
    BOMB("bomb",
            s -> {},
            s -> {},
            Images.bomb,
            false,
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
            },
            false, true);

    public final String name;
    // invoked when head touched the bonus
    public final Consumer<State> apply;
    // invoked when *active* bonus { expired or head failed}
    public final Consumer<State> restore;
    // invoked when bonus expired (was not toched)
    public final BiConsumer<State, Bonus> onExpire;
    // image of bonus
    public final Image image;
    // if true then bonus become active for some period
    public final boolean canBeActive;
    // does it help head
    public final boolean isHeadHelp;
    public final boolean canBeSeveral;

    BonusType(String name, Consumer<State> apply, Consumer<State> restore, Image image,
              boolean canBeActive, BiConsumer<State, Bonus> onExpire,
              boolean isHeadHelp, boolean canBeSeveral) {
        this.name = name;
        this.apply = apply;
        this.restore = restore;
        this.image = image;
        this.canBeActive = canBeActive;
        this.onExpire = onExpire;
        this.isHeadHelp = isHeadHelp;
        this.canBeSeveral = canBeSeveral;
    }
}
