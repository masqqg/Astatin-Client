package dev.astatin.theme;

import dev.astatin.render.UiFont;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.Typeface;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * The bundled Inter faces. Any missing face, missing native library or parse
 * failure is reported by throwing, which makes {@link Fonts} switch to the
 * Minecraft font instead of leaving a broken typeface behind.
 */
final class SkijaFonts implements Fonts.Provider {
    private static final Map<Fonts.Weight, String> FILES = new EnumMap<>(Fonts.Weight.class);

    static {
        FILES.put(Fonts.Weight.REGULAR, "Inter-Regular.ttf");
        FILES.put(Fonts.Weight.MEDIUM, "Inter-Medium.ttf");
        FILES.put(Fonts.Weight.SEMIBOLD, "Inter-SemiBold.ttf");
        FILES.put(Fonts.Weight.BOLD, "Inter-Bold.ttf");
        FILES.put(Fonts.Weight.ITALIC, "Inter-Italic.ttf");
    }

    private final Map<Fonts.Weight, Typeface> typefaces = new EnumMap<>(Fonts.Weight.class);

    private SkijaFonts() {
    }

    static Fonts.Provider provider() {
        SkijaFonts fonts = new SkijaFonts();
        for (Map.Entry<Fonts.Weight, String> entry : FILES.entrySet()) {
            fonts.typefaces.put(entry.getKey(), loadTypeface("/assets/astatin/fonts/" + entry.getValue()));
        }
        return fonts;
    }

    private static Typeface loadTypeface(String path) {
        try (InputStream in = SkijaFonts.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled font " + path);
            }
            byte[] bytes = in.readAllBytes();
            Typeface typeface = FontMgr.getDefault().makeFromData(Data.makeFromBytes(bytes));
            if (typeface == null) {
                throw new IllegalStateException("Could not parse font " + path);
            }
            return typeface;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load bundled font " + path, e);
        }
    }

    @Override
    public UiFont font(Fonts.Weight weight, float size) {
        Typeface typeface = typefaces.get(weight);
        if (typeface == null) {
            typeface = typefaces.get(Fonts.Weight.REGULAR);
        }
        if (typeface == null) {
            throw new IllegalStateException("Bundled interface font is not available");
        }
        Font font = new Font(typeface, size);
        font.setSubpixel(true);
        font.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
        font.setHinting(FontHinting.NONE);
        return new SkijaFont(font);
    }
}
