package dev.astatin.hud;

import dev.astatin.render.Render2D;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

/**
 * WASD plus mouse buttons. Pressed keys light up with the accent gradient and
 * a soft glow, which makes the display readable at a glance.
 */
public final class KeystrokesElement extends HudElement {
    private final KeyFeedback feedback = new KeyFeedback();
    public KeystrokesElement() {
        super("keystrokes", "Keystrokes");
    }

    @Override
    public float baseWidth(HudData data) {
        return 104f;
    }

    @Override
    public float baseHeight(HudData data) {
        return 92f;
    }

    @Override
    public void draw(Render2D r, HudData data) {
        feedback.update(data.keys, data.frameSeconds, data.animations);
        float w = baseWidth(data);
        float h = baseHeight(data);
        r.glass(0f, 0f, w, h, 12f, 0x5E000000, 10f);

        float pad = 7f;
        float keyW = 27f;
        float keyH = 23f;
        float gap = 4f;
        float left = pad;
        float top = pad;

        key(r, left + keyW + gap, top, keyW, keyH, "W", feedback.value(0), 10f);
        key(r, left, top + keyH + gap, keyW, keyH, "A", feedback.value(1), 10f);
        key(r, left + keyW + gap, top + keyH + gap, keyW, keyH, "S", feedback.value(2), 10f);
        key(r, left + (keyW + gap) * 2f, top + keyH + gap, keyW, keyH, "D", feedback.value(3), 10f);

        float bottom = top + (keyH + gap) * 2f;
        float mouseW = (w - pad * 2f - gap) * 0.5f;
        if (data.showMouse) {
            mouseKey(r, left, bottom, mouseW, 20f, "LMB", feedback.value(4));
            mouseKey(r, left + mouseW + gap, bottom, mouseW, 20f, "RMB", feedback.value(5));
        } else {
            key(r, left, bottom, w - pad * 2f, 20f, "SPACE", feedback.value(6), 8f);
        }
    }

    private void key(Render2D r, float x, float y, float w, float h, String label, float down, float size) {
        if (down > .02f) {
            r.shadow(x + 1f, y + 2f, x + w - 1f, y + h + 2f, 6f, 9f, Theme.accentSoft(0.35f * down));
        }
        r.roundRect(x, y, x + w, y + h, 6.5f, Theme.mix(0x0DFFFFFF, Theme.accentLight(0.10f), down));
        r.roundRectStroke(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, 6.5f, 1f,
                Theme.mix(0x14FFFFFF, Theme.accentLight(0.55f), down));
        int color = Theme.mix(Theme.TEXT_SOFT, Theme.TEXT_ON_ACCENT, down);
        r.textCenteredTop(label, x + w * 0.5f, y + (h - size) * 0.5f - 0.5f, Fonts.semibold(size), color);
    }

    private void mouseKey(Render2D r, float x, float y, float w, float h, String label, float down) {
        if (down > .02f) {
            r.shadow(x + 1f, y + 2f, x + w - 1f, y + h + 2f, 5f, 8f, Theme.accentSoft(0.32f * down));
        }
        r.roundRect(x, y, x + w, y + h, 5.5f, Theme.mix(0x0AFFFFFF, Theme.accentLight(0.10f), down));
        r.roundRectStroke(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, 5.5f, 1f,
                Theme.mix(0x12FFFFFF, Theme.accentLight(0.5f), down));
        r.textCenteredTop(label, x + w * 0.5f, y + 5.5f, Fonts.medium(7.5f),
                Theme.mix(Theme.TEXT_MUTED, Theme.TEXT_ON_ACCENT, down));
    }
}
