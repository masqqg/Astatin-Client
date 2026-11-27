package dev.astatin.hud;

import dev.astatin.animation.Anim;

/** Frame-rate independent feedback for the seven keys displayed by the HUD. */
final class KeyFeedback {
    private final Anim[] keys = new Anim[7];

    KeyFeedback() {
        for (int i = 0; i < keys.length; i++) keys[i] = new Anim(0f, 260f, 30f);
    }

    void update(boolean[] pressed, float dt, boolean animations) {
        for (int i = 0; i < keys.length; i++) {
            float target = pressed[i] ? 1f : 0f;
            if (animations) keys[i].target(target).update(dt);
            else keys[i].snap(target);
        }
    }

    float value(int index) {
        return Anim.clamp01(keys[index].get());
    }
}
