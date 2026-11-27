package dev.astatin.theme;

import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.Typeface;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Inter is bundled with the mod so typography never depends on the player's
 * operating system. Sized fonts are cached because creating a Skia font per
 * frame would be both slow and wasteful.
 */
public final class Fonts {
    public enum Weight {
        REGULAR("Inter-Regular.ttf"),
        MEDIUM("Inter-Medium.ttf"),
        SEMIBOLD("Inter-SemiBold.ttf"),
        BOLD("Inter-Bold.ttf"),
        ITALIC("Inter-Italic.ttf");

        final String file;

        Weight(String file) {
            this.file = file;
        }
    }

    private static final Map<Weight, Typeface> TYPEFACES = new HashMap<>();
    private static final Map<Integer, Font> CACHE = new HashMap<>();
    private static boolean loaded;

    private Fonts() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        for (Weight weight : Weight.values()) {
            TYPEFACES.put(weight, loadTypeface("/assets/astatin/fonts/" + weight.file));
        }
        loaded = true;
    }

    private static Typeface loadTypeface(String path) {
        try (InputStream in = Fonts.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled font " + path);
            }
            byte[] bytes = in.readAllBytes();
            Typeface typeface = FontMgr.getDefault().makeFromData(Data.makeFromBytes(bytes));
            if (typeface == null) {
                throw new IllegalStateException("Could not parse font " + path);
            }
            return typeface;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load bundled font " + path, e);
        }
    }

    public static Font font(Weight weight, float size) {
        load();
        int key = (weight.ordinal() << 16) | (int) (size * 100f);
        Font cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Typeface typeface = TYPEFACES.get(weight);
        if (typeface == null) {
            typeface = TYPEFACES.get(Weight.REGULAR);
        }
        Font font = new Font(typeface, size);
        font.setSubpixel(true);
        font.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
        font.setHinting(FontHinting.NONE);
        CACHE.put(key, font);
        return font;
    }

    public static Font regular(float size) {
        return font(Weight.REGULAR, size);
    }

    public static Font medium(float size) {
        return font(Weight.MEDIUM, size);
    }

    public static Font semibold(float size) {
        return font(Weight.SEMIBOLD, size);
    }

    public static Font bold(float size) {
        return font(Weight.BOLD, size);
    }
}
