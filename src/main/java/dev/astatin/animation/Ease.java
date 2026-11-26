package dev.astatin.animation;

/**
 * Easing curves named after the motion they are used for. All take a 0..1
 * progress and return an eased 0..1 value.
 */
public final class Ease {
    private Ease() {
    }

    public static float outCubic(float t) {
        float f = 1f - clamp01(t);
        return 1f - f * f * f;
    }

    public static float inOutCubic(float t) {
        float f = clamp01(t);
        return f < 0.5f
                ? 4f * f * f * f
                : 1f - (float) Math.pow(-2f * f + 2f, 3f) / 2f;
    }

    public static float outQuint(float t) {
        float f = 1f - clamp01(t);
        return 1f - f * f * f * f * f;
    }

    public static float outBack(float t) {
        float f = clamp01(t) - 1f;
        float c = 1.70158f;
        return 1f + (c + 1f) * f * f * f + c * f * f;
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * clamp01(t);
    }
}
