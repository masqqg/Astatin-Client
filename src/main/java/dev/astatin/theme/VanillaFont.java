package dev.astatin.theme;

import dev.astatin.render.UiFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * Minecraft's own font, scaled to the requested interface size. The vanilla
 * glyphs are nine units tall, so a request for a 9 px label is drawn at their
 * natural size and everything else is scaled around that. Width calibration is
 * tuned so the fallback keeps the same line lengths as Inter.
 */
public final class VanillaFont implements UiFont {
    private static final float ASCENT = 7f;
    private static final float DESCENT = 2f;
    /** Width of one vanilla glyph unit per requested px of type size. */
    private static final float WIDTH_PER_SIZE = 0.0945f;

    private final Fonts.Weight weight;
    private final float size;
    private final float scale;

    public VanillaFont(Fonts.Weight weight, float size) {
        this.weight = weight;
        this.size = size;
        this.scale = Math.max(0.3f, size * WIDTH_PER_SIZE);
    }

    private static Font minecraftFont() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.font;
    }

    @Override
    public float measureWidth(String text) {
        Font font = minecraftFont();
        if (font == null || text == null || text.isEmpty()) {
            return 0f;
        }
        return font.width(text) * scale;
    }

    @Override
    public float ascent() {
        return -ASCENT * scale;
    }

    @Override
    public float descent() {
        return DESCENT * scale;
    }

    /** Horizontal scale applied when the glyphs are drawn. */
    public float scale() {
        return scale;
    }

    public Fonts.Weight weight() {
        return weight;
    }

    /** True for the faces that should be drawn with a faux-bold second pass. */
    public boolean emphasised() {
        return weight == Fonts.Weight.SEMIBOLD || weight == Fonts.Weight.BOLD;
    }

    public float size() {
        return size;
    }

    @Override
    public String toString() {
        return "VanillaFont[" + weight + " " + size + "]";
    }
}
