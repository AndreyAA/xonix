package com.ali.dev.xonix;

import com.ali.dev.xonix.State.XY;

import static com.ali.dev.xonix.Config.*;
import static com.ali.dev.xonix.XonixApp.*;

enum ItemType {
    InField, OutFiled
}

class Item {
    final ItemType type;
    XY pos;
    double currentX, currentY;
    XY shift;
    double velocity;

    Item(XY pos, XY shift, ItemType type, double velocity) {
        this.pos = pos;
        this.currentX = calcX(pos.x);
        this.currentY = calcY(pos.y);
        this.shift = shift;
        this.type = type;
        this.velocity = velocity;
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

        if (type == ItemType.OutFiled) {
            if (newY <= MIN_Y - HALF_CELL || newRow <= -1 || newRow >= state.entityGrid.length || !state.entityGrid[newRow][curCol].isBusy) {
                // Отражение от горизонтали
                shift = new XY(shift.x, -1 * shift.y);
            }

            if (newX <= MIN_X - HALF_CELL || newCol < 0 || newCol >= state.entityGrid[0].length || !state.entityGrid[curRow][newCol].isBusy) {
                // Отражение от вертикали
                shift = new XY(-1 * shift.x, shift.y);
            }
        } else {

            if (newRow < 0 || newRow >= state.entityGrid.length || state.entityGrid[newRow][curCol].isBusy) {
                // Отражение по вертикали
                shift = new XY(shift.x, -1 * shift.y);
            }

            if (newCol < 0 || newCol >= state.entityGrid[0].length || state.entityGrid[curRow][newCol].isBusy) {
                // Отражение по горизонтали
                shift = new XY(-1 * shift.x, shift.y);
            }
        }

        // Движение в текущем направлении
        currentX += shift.x * velocity;
        currentY += shift.y * velocity;
        pos = new XY(calcCol(currentX + HALF_CELL), calcRow(currentY + HALF_CELL));
    }

}
