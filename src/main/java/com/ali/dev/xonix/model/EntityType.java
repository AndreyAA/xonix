package com.ali.dev.xonix.model;

public enum EntityType {
    FREE(false, false),
    FREE_AFTER_BOMB(false, false),
    BORDER(true, false),
    BLOCK(true, true);

    public final boolean isBusy;
    public final boolean isDestroyable;

    EntityType(boolean isBusy, boolean isDestroyable) {
        this.isBusy = isBusy;
        this.isDestroyable = isDestroyable;
    }
}
