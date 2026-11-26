package dev.astatin.util;

public final class FrameClock {
    private static long last = System.nanoTime();

    private FrameClock() {
    }

    public static float delta() {
        long now = System.nanoTime();
        float dt = (now - last) / 1_000_000_000f;
        last = now;
        if (dt < 0f) {
            dt = 0f;
        }
        return Math.min(dt, 0.1f);
    }

    public static void reset() {
        last = System.nanoTime();
    }
}
