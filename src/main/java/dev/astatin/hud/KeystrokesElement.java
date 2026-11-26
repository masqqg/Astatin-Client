package dev.astatin.hud;

import dev.astatin.render.Render2D;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

/**
 * WASD plus mouse buttons. Pressed keys light up with the accent gradient and
 * a soft glow, which makes the display readable at a glance.
 */
public final class KeystrokesElement extends HudElement {
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
        float w = baseWidth(data);
        float h = baseHeight(data);
        r.glass(0f, 0f, w, h, 12f, 0x5E000000, 10f);

        float pad = 7f;
        float keyW = 27f;
        float keyH = 23f;
        float gap = 4f;
        float left = pad;
        float top = pad;

        key(r, left + keyW + gap, top, keyW, keyH, "W", data.keys[0], 10f);
        key(r, left, top + keyH + gap, keyW, keyH, "A", data.keys[1], 10f);
        key(r, left + keyW + gap, top + keyH + gap, keyW, keyH, "S", data.keys[2], 10f);
        key(r, left + (keyW + gap) * 2f, top + keyH + gap, keyW, keyH, "D", data.keys[3], 10f);

        float bottom = top + (keyH + gap) * 2f;
        float mouseW = (w - pad * 2f - gap) * 0.5f;
        if (data.showMouse) {
            mouseKey(r, left, bottom, mouseW, 20f, "LMB", data.keys[4]);
            mouseKey(r, left + mouseW + gap, bottom, mouseW, 20f, "RMB", data.keys[5]);
        } else {
            key(r, left, bottom, w - pad * 2f, 20f, "SPACE", data.keys[0], 8f);
        }
    }

    private void key(Render2D r, float x, float y, float w, float h, String label, boolean down, float size) {
        if (down) {
            r.shadow(x + 1f, y + 2f, x + w - 1f, y + h + 2f, 6f, 9f, Theme.accentSoft(0.35f));
            r.gradientRect(x, y, x + w, y + h, 6.5f, Theme.accentLight(0.14f), Theme.accentDark(0.06f));
            r.roundRectStroke(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, 6.5f, 1f, Theme.accentLight(0.55f));
        } else {
            r.roundRect(x, y, x + w, y + h, 6.5f, 0x0DFFFFFF);
            r.roundRectStroke(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, 6.5f, 1f, 0x14FFFFFF);
        }
        int color = down ? Theme.TEXT_ON_ACCENT : Theme.TEXT_SOFT;
        r.textCenteredTop(label, x + w * 0.5f, y + (h - size) * 0.5f - 0.5f, Fonts.semibold(size), color);
    }

    private void mouseKey(Render2D r, float x, float y, float w, float h, String label, boolean down) {
        if (down) {
            r.shadow(x + 1f, y + 2f, x + w - 1f, y + h + 2f, 5f, 8f, Theme.accentSoft(0.32f));
            r.gradientRect(x, y, x + w, y + h, 5.5f, Theme.accentLight(0.14f), Theme.accentDark(0.06f));
            r.roundRectStroke(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, 5.5f, 1f, Theme.accentLight(0.5f));
        } else {
            r.roundRect(x, y, x + w, y + h, 5.5f, 0x0AFFFFFF);
            r.roundRectStroke(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, 5.5f, 1f, 0x12FFFFFF);
        }
        r.textCenteredTop(label, x + w * 0.5f, y + 5.5f, Fonts.medium(7.5f),
                down ? Theme.TEXT_ON_ACCENT : Theme.TEXT_MUTED);
    }
}
