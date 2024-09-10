package com.ali.dev.xonix;

import java.util.HashSet;
import java.util.Set;

import static com.ali.dev.xonix.Config.GRID_SIZE_X;

public class Head {
    XY pos;
    XY shift;
    Set<XY> curPath;
    XY startPoint;
    int velocity;

    public void init() {
        pos = new XY(GRID_SIZE_X / 2, 1);
        shift = new XY(0, 0);
        curPath = new HashSet<>();
        startPoint = null;
        velocity = 1;
    }
}
