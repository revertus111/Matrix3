package com.matrix3.mobile;

final class MobileInputState {

    static volatile float pointerX;
    static volatile float pointerY;
    static volatile boolean pointerDown;
    static volatile int lastKeyCode;
    static volatile boolean keyDown;

    private MobileInputState() {
    }

    static void updatePointer(float x, float y, boolean down) {
        pointerX = x;
        pointerY = y;
        pointerDown = down;
    }

    static void updateKey(int keyCode, boolean down) {
        lastKeyCode = keyCode;
        keyDown = down;
    }
}
