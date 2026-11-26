package dev.astatin.modules;

public final class ZoomState {
    private float current = 1f;
    private float amount = 4f;
    private float smoothness = 0.55f;
    private boolean held;

    public void hold(boolean held, float amount, float smoothness) {
        this.held = held;
        this.amount = Math.max(1f, amount);
        this.smoothness = Math.max(0f, Math.min(1f, smoothness));
    }

    public void update(float dt) {
        float target = held ? amount : 1f;
        float rate = 5f + (1f - smoothness) * 18f;
        current += (target - current) * (1f - (float) Math.exp(-rate * dt));
        if (Math.abs(target - current) < 0.0015f) {
            current = target;
        }
    }

    public float fovMultiplier() {
        return 1f / Math.max(1f, current);
    }

    public float current() {
        return current;
    }

    public boolean isHeld() {
        return held;
    }
}
