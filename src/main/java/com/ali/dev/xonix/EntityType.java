package com.ali.dev.xonix;

enum EntityType {
    FREE(false, false),
    BORDER(true, false),
    BLOCK(true, true);

    final boolean isBusy;
    final boolean isDestroyable;

    EntityType(boolean isBusy, boolean isDestroyable) {
        this.isBusy = isBusy;
        this.isDestroyable = isDestroyable;
    }
}
