package dev.astatin.hud;

import dev.astatin.render.Render2D;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

/**
 * The client's signature element: logo mark, wordmark and live performance
 * readouts in a single frosted capsule.
 */
public final class WatermarkElement extends HudElement {
    public WatermarkElement() {
        super("watermark", "Watermark");
    }

    @Override
    public float baseWidth(HudData data) {
        return 228f;
    }

    @Override
    public float baseHeight(HudData data) {
        return 42f;
    }

    @Override
    public void draw(Render2D r, HudData data) {
        float w = baseWidth(data);
        float h = baseHeight(data);
        r.glass(0f, 0f, w, h, 13f, 0x72000000, 13f);
        r.accentEdge(0f, 0f, w, h, 13f);

        // The symbol is typographic at small sizes so it stays legible.
        r.textTop("Ast", 11f, 8f, Fonts.semibold(16f), Theme.TEXT);
        dev.astatin.render.Icons.draw(r, dev.astatin.render.Icons.Glyph.STAR, 34f, 12f, 5.5f,
                Theme.accentSoft(0.85f));

        r.textTop("Astatin", 46f, 9f, Fonts.semibold(14f), Theme.TEXT);
        r.textTracked("CLIENT", 46.5f, 31f, Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.9f);

        r.line(121f, 10f, 121f, h - 10f, 1f, 0x18FFFFFF);

        float cursor = 130f;
        if (data.showFps) {
            r.textTracked("FPS", cursor, 16f, Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.8f);
            r.textTop(String.valueOf(data.fps), cursor, 19f, Fonts.semibold(14f), Theme.AMBER);
            cursor += 42f;
        }
        if (data.showPing) {
            if (data.showFps) {
                r.line(cursor - 9f, 12f, cursor - 9f, h - 12f, 1f, 0x10FFFFFF);
            }
            r.textTracked("PING", cursor + 1f, 16f, Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.8f);
            r.textTop(data.ping + " ms", cursor + 1f, 19f, Fonts.semibold(13f), Theme.TEXT);
        }
    }
}
