package com.ali.dev.towers;

enum EntityType {
    FREE(false), BORDER(true);

    final boolean isBusy;

    EntityType(boolean isBusy) {
        this.isBusy = isBusy;
    }
}
