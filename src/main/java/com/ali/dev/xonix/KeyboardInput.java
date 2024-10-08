package com.ali.dev.xonix;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import static com.ali.dev.xonix.Config.BUTTON_DELAY_MS;

public class KeyboardInput implements KeyListener {

    private static final int KEY_COUNT = 256;
    private static final Map<Integer, Long> lastPressedKey = new HashMap<>();
    // Current state of the keyboard
    private boolean[] currentKeys = null;
    // Polled keyboard state
    private KeyState[] keys = null;

    public KeyboardInput() {
        currentKeys = new boolean[KEY_COUNT];
        keys = new KeyState[KEY_COUNT];
        for (int i = 0; i < KEY_COUNT; ++i) {
            keys[i] = KeyState.RELEASED;
        }
    }

    public boolean isPressedOnce(int code) {
        if (keyDownOnce(code)
                && System.currentTimeMillis() - lastPressedKey.getOrDefault(code, 0L) > BUTTON_DELAY_MS) {
            lastPressedKey.put(code, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public synchronized void poll() {
        for (int i = 0; i < KEY_COUNT; ++i) {
            // Set the key state
            if (currentKeys[i]) {
                // If the key is down now, but was not
                // down last frame, set it to ONCE,
                // otherwise, set it to PRESSED
                if (keys[i] == KeyState.RELEASED)
                    keys[i] = KeyState.ONCE;
                else
                    keys[i] = KeyState.PRESSED;
            } else {
                keys[i] = KeyState.RELEASED;
            }
        }
    }

    public boolean keyDown(int keyCode) {
        return keys[keyCode] == KeyState.ONCE ||
                keys[keyCode] == KeyState.PRESSED;
    }

    public boolean keyDownOnce(int keyCode) {
        return keys[keyCode] == KeyState.ONCE;
    }

    public synchronized void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
//        System.out.println(keyCode);
        if (keyCode >= 0 && keyCode < KEY_COUNT) {
            currentKeys[keyCode] = true;
        }
    }

    public synchronized void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode >= 0 && keyCode < KEY_COUNT) {
            currentKeys[keyCode] = false;
        }
    }

    public void keyTyped(KeyEvent e) {
        // Not needed
    }

    private enum KeyState {
        RELEASED, // Not down
        PRESSED,  // Down, but not the first time
        ONCE      // Down for the first time
    }
}