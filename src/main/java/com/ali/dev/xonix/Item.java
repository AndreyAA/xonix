package com.ali.dev.xonix;

import com.ali.dev.xonix.State.XY;

import static com.ali.dev.xonix.Config.*;
import static com.ali.dev.xonix.XonixApp.*;

enum ItemArea {
    InField, OutFiled
}

enum ItemType {
    STD, DESTROYER
}

class Item {
    final ItemArea area;
    final ItemType type;
    XY pos;
    double currentX, currentY;
    XY shift;
    double velocity;
    double prevVelocity;

    Item(XY pos, XY shift, ItemType type, ItemArea area, double velocity) {
        this.pos = pos;
        this.currentX = calcX(pos.x);
        this.currentY = calcY(pos.y);
        this.shift = shift;
        this.area = area;
        this.type = type;
        this.velocity = velocity;
        this.prevVelocity = velocity;
    }

    public void slowDown() {
        if (velocity>=prevVelocity) {
            this.prevVelocity = velocity;
            velocity = Math.max(1, velocity / 2);
        }
    }
    public void restore() {
        if (prevVelocity>velocity) {
            this.velocity = prevVelocity;
        }
    }

    void move(State state) {
        double newX = currentX + velocity * shift.x;
        double newY = currentY + velocity * shift.y;
        var curCol = calcCol(currentX + HALF_CELL);
        var curRow = calcRow(currentY + HALF_CELL);

        var newCol = calcCol(newX + HALF_CELL);
        var newRow = calcRow(newY + HALF_CELL);

        if (state.checkCollisions(newCol, newRow)) {
            return;
        }

        if (area == ItemArea.OutFiled) {
            if (newY <= MIN_Y - HALF_CELL || newRow <= -1 || newRow >= state.entityGrid.length || !state.entityGrid[newRow][curCol].isBusy) {
                // Отражение от горизонтали
                shift = new XY(shift.x, -1 * shift.y);
            }

            if (newX <= MIN_X - HALF_CELL || newCol < 0 || newCol >= state.entityGrid[0].length || !state.entityGrid[curRow][newCol].isBusy) {
                // Отражение от вертикали
                shift = new XY(-1 * shift.x, shift.y);
            }
        } else {

            boolean isBusy = state.entityGrid[newRow][curCol].isBusy;
            if (newRow < 0 || newRow >= state.entityGrid.length || isBusy) {
                // Отражение по вертикали
                if (type==ItemType.DESTROYER && state.entityGrid[newRow][curCol].isDestroyable) {
                    state.entityGrid[newRow][curCol]=EntityType.FREE;
                    state.busyCells--;
                }
                shift = new XY(shift.x, -1 * shift.y);
            }

            boolean isBusy2 = state.entityGrid[curRow][newCol].isBusy;
            if (newCol < 0 || newCol >= state.entityGrid[0].length || isBusy2) {
                // Отражение по горизонтали
                if (type==ItemType.DESTROYER && state.entityGrid[curRow][newCol].isDestroyable) {
                    state.entityGrid[curRow][newCol]=EntityType.FREE;
                    state.busyCells--;
                }
                shift = new XY(-1 * shift.x, shift.y);
            }
        }

        // Движение в текущем направлении
        currentX += shift.x * velocity;
        currentY += shift.y * velocity;
        pos = new XY(calcCol(currentX + HALF_CELL), calcRow(currentY + HALF_CELL));
    }

}
