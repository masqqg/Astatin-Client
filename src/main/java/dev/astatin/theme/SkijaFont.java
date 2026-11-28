package dev.astatin.theme;

import dev.astatin.render.UiFont;
import io.github.humbleui.skija.Font;

/** A Skia font exposed through the backend-neutral {@link UiFont} handle. */
public final class SkijaFont implements UiFont {
    private final Font font;
    private final float ascent;
    private final float descent;

    SkijaFont(Font font) {
        this.font = font;
        this.ascent = font.getMetrics().getAscent();
        this.descent = font.getMetrics().getDescent();
    }

    /** The underlying Skia font, for the Skija drawing backend. */
    public Font handle() {
        return font;
    }

    @Override
    public float measureWidth(String text) {
        return text == null || text.isEmpty() ? 0f : font.measureTextWidth(text);
    }

    @Override
    public float ascent() {
        return ascent;
    }

    @Override
    public float descent() {
        return descent;
    }
}
