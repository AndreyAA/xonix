package com.ali.dev.xonix;

import com.ali.dev.xonix.model.State;

public class ScoreCalculator {

    private long prevProgressTime = System.currentTimeMillis();

    public int calcScore(double prevProgress, double currentProgress, State state) {
        double delta = currentProgress - prevProgress;
        long curTime = System.currentTimeMillis();
        int time = (int) (curTime - prevProgressTime);
        prevProgressTime = curTime;

        //1% - 100 score points
        int base = (int) (10000 * delta);
        double complexity = state.getCurLevel().getItemInField() * Math.pow(state.getCurLevel().getVelocityInField(), 2);
        int complexityBonus = (int) (complexity * delta * 1000);
        int total = base + complexityBonus;
        System.out.println("score total:" + total + ",  base:" + base + ", complexity:" + complexityBonus);
        return total;
    }
}
