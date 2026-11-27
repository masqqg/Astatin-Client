package dev.astatin.hud;

import dev.astatin.render.Render2D;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

/** Clicks per second, with a small history graph. */
public final class CpsElement extends HudElement {
    public CpsElement() {
        super("cps", "CPS");
    }

    @Override
    public float baseWidth(HudData data) {
        return 100f;
    }

    @Override
    public float baseHeight(HudData data) {
        return 52f;
    }

    @Override
    public void draw(Render2D r, HudData data) {
        float w = baseWidth(data);
        float h = baseHeight(data);
        r.glass(0f, 0f, w, h, 12f, 0x5C000000, 10f);

        r.textTracked("CPS", 10f, 15f, Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.9f);
        r.textTop(String.valueOf(data.cpsLeft + data.cpsRight), 10f, 16f, Fonts.semibold(18f), Theme.TEXT);

        if (data.splitCps) {
            r.textTracked("L", 64f, 26f, Fonts.medium(6f), Theme.TEXT_MUTED, 0.5f);
            r.textTop(String.valueOf(data.cpsLeft), 71f, 19f, Fonts.semibold(9.5f), Theme.TEXT_SOFT);
            r.textTracked("R", 64f, 40f, Fonts.medium(6f), Theme.TEXT_MUTED, 0.5f);
            r.textTop(String.valueOf(data.cpsRight), 71f, 33f, Fonts.semibold(9.5f), Theme.TEXT_SOFT);
        } else {
            r.textRight(String.valueOf(data.cpsLeft + data.cpsRight), 90f, 40f, Fonts.medium(8f), Theme.TEXT_MUTED);
        }

        float peak = 5f;
        for (float value : data.cpsHistory) peak = Math.max(peak, value);
        for (int i = 0; i < data.cpsHistory.length; i++) {
            float x = 10f + i * 3.1f;
            float barHeight = 5f * Math.max(0f, data.cpsHistory[i]) / peak;
            if (barHeight > 0f) {
                r.roundRect(x, 48f - barHeight, x + 2f, 48f, 1f, Theme.accentSoft(.7f));
            }
        }
    }
}
