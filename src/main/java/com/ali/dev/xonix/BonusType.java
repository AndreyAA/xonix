package com.ali.dev.xonix;

import java.awt.*;
import java.util.function.Consumer;

enum BonusType {
    LIFE(s -> s.lifes++, s -> {
    }, Images.bonusLife, false),
    HEAD_SPEED(s -> s.head.velocity = 2, s -> s.head.velocity = 1, Images.speedUp, true),
    FREEZE(s -> s.items.stream().filter(i -> i.area == ItemAreaType.InField).forEach(Item::slowDown),
            s -> s.items.stream().filter(i -> i.area == ItemAreaType.InField).forEach(Item::restore),
            Images.freeze, true);

    final Consumer<State> apply;
    final Consumer<State> reject;
    final Image image;
    final boolean durable;

    BonusType(Consumer<State> apply, Consumer<State> reject, Image image, boolean durable) {
        this.apply = apply;
        this.reject = reject;
        this.image = image;
        this.durable = durable;
    }
}
