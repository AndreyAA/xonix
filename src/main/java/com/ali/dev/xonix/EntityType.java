package com.ali.dev.xonix;

enum EntityType {
    FREE(false), BORDER(true);

    final boolean isBusy;

    EntityType(boolean isBusy) {
        this.isBusy = isBusy;
    }
}
