package dev.astatin.animation;

/**
 * A small spring used everywhere in the interface. It is time based (not frame
 * based), always continues from the current value when retargeted, and never
 * allocates while running.
 */
public final class Anim {
    private float value;
    private float target;
    private float velocity;
    private float stiffness;
    private float damping;

    public Anim(float initial) {
        this(initial, 220f, 26f);
    }

    public Anim(float initial, float stiffness, float damping) {
        this.value = initial;
        this.target = initial;
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public float get() {
        return value;
    }

    public float target() {
        return target;
    }

    public Anim target(float next) {
        this.target = next;
        return this;
    }

    public Anim snap(float next) {
        this.value = next;
        this.target = next;
        this.velocity = 0f;
        return this;
    }

    public Anim velocity(float v) {
        this.velocity = v;
        return this;
    }

    public boolean settled() {
        return Math.abs(target - value) < 0.0015f && Math.abs(velocity) < 0.012f;
    }

    /** @param dt seconds since the previous frame, clamped by the caller */
    public float update(float dt) {
        if (dt <= 0f) {
            return value;
        }
        float step = Math.min(dt, 1f / 20f);
        int loops = Math.max(1, (int) Math.ceil(step / (1f / 120f)));
        float slice = step / loops;
        for (int i = 0; i < loops; i++) {
            float acceleration = (target - value) * stiffness - velocity * damping;
            velocity += acceleration * slice;
            value += velocity * slice;
        }
        if (Math.abs(target - value) < 0.0005f && Math.abs(velocity) < 0.01f) {
            value = target;
            velocity = 0f;
        }
        return value;
    }

    public static float outCubic(float t) {
        float f = 1f - clamp01(t);
        return 1f - f * f * f;
    }

    public static float outQuint(float t) {
        float f = 1f - clamp01(t);
        return 1f - f * f * f * f * f;
    }

    public static float outExpo(float t) {
        float f = clamp01(t);
        return f >= 1f ? 1f : (float) (1f - Math.pow(2f, -10f * f));
    }

    public static float inOutCubic(float t) {
        float f = clamp01(t);
        return f < 0.5f ? 4f * f * f * f : 1f - (float) Math.pow(-2f * f + 2f, 3f) / 2f;
    }

    public static float outBack(float t) {
        float f = clamp01(t) - 1f;
        float c = 1.70158f;
        return 1f + (c + 1f) * f * f * f + c * f * f;
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static float step(float from, float to, float speed, float dt) {
        float delta = to - from;
        float max = speed * dt;
        if (Math.abs(delta) <= max) {
            return to;
        }
        return from + Math.signum(delta) * max;
    }
}
