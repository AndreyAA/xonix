# Bugs

## High

### 1. `BUG-001`: `ArrayIndexOutOfBoundsException` for in-field enemies at borders
File: [src/main/java/com/ali/dev/xonix/model/Item.java](/home/bob/IdeaProjects/xonix/src/main/java/com/ali/dev/xonix/model/Item.java)

In-field enemy movement reads `state.entityGrid[newRow][curCol]` and `state.entityGrid[curRow][newCol]` before validating that `newRow` and `newCol` are within bounds. When an enemy steps beyond a border, the game can crash before bounce logic runs.

### 2. Crash after completing the last level
File: [src/main/java/com/ali/dev/xonix/model/State.java](/home/bob/IdeaProjects/xonix/src/main/java/com/ali/dev/xonix/model/State.java)

`nextLevel()` increments `curLevel` and immediately calls `thisLevel()`. On the final level, the next access to `levels.get(curLevel)` throws `IndexOutOfBoundsException` instead of ending the game gracefully.

## Medium

### 3. Corrupted or empty `scores.txt` breaks startup
File: [src/main/java/com/ali/dev/xonix/model/State.java](/home/bob/IdeaProjects/xonix/src/main/java/com/ali/dev/xonix/model/State.java)

`readScoreFile()` assumes every line has the format `name;score` and parses without validation. Empty lines, partial writes, or manual edits can cause `ArrayIndexOutOfBoundsException` or `NumberFormatException` during startup.

### 4. Bonuses can spawn on the border area
File: [src/main/java/com/ali/dev/xonix/model/Engine.java](/home/bob/IdeaProjects/xonix/src/main/java/com/ali/dev/xonix/model/Engine.java)

Bonus coordinates are generated in `0..GRID_SIZE_X-3` and `0..GRID_SIZE_Y-3`, which includes the occupied border rows and columns. This allows bonuses to appear in invalid or inaccessible cells.

### 5. Multiple lives can be lost in a single tick
Files:
- [src/main/java/com/ali/dev/xonix/model/State.java](/home/bob/IdeaProjects/xonix/src/main/java/com/ali/dev/xonix/model/State.java)
- [src/main/java/com/ali/dev/xonix/model/Engine.java](/home/bob/IdeaProjects/xonix/src/main/java/com/ali/dev/xonix/model/Engine.java)

After `failHead()` is triggered, `moveItems()` continues iterating through the remaining enemies in the same tick. Additional enemies can collide with the respawned head immediately and decrement lives again before the frame ends.

## Assumptions

- Completing the last configured level should not crash the game; it should end gracefully or restart by explicit design.
- Bonuses are expected to spawn only in playable cells, not on the permanent border frame.
- A single collision event should cost at most one life per tick/frame.
