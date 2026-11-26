package dev.astatin.util;

import java.util.ArrayDeque;

public final class CpsTracker {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();

    private CpsTracker() {
    }

    public static void press(int button) {
        if (button == 0) {
            LEFT.addLast(System.currentTimeMillis());
        } else if (button == 1) {
            RIGHT.addLast(System.currentTimeMillis());
        }
    }

    public static int left() {
        return count(LEFT);
    }

    public static int right() {
        return count(RIGHT);
    }

    private static int count(ArrayDeque<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
            clicks.removeFirst();
        }
        return clicks.size();
    }
}
