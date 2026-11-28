package dev.astatin.theme;

import dev.astatin.render.UiFont;

import java.util.HashMap;
import java.util.Map;

/**
 * Typography for the interface. Inter is bundled for the Skija renderer; when
 * Skija (or its native library) is unavailable every size falls back to
 * Minecraft's own font, scaled to the requested size, so the HUD and the menu
 * still render on those systems.
 */
public final class Fonts {
    public enum Weight {
        REGULAR,
        MEDIUM,
        SEMIBOLD,
        BOLD,
        ITALIC
    }

    /** Supplies sized fonts for one rendering technology. */
    public interface Provider {
        UiFont font(Weight weight, float size);
    }

    private static final Map<Integer, UiFont> CACHE = new HashMap<>();
    private static Provider provider;
    private static String source = "none";

    private Fonts() {
    }

    public static synchronized void load() {
        if (provider != null) {
            return;
        }
        try {
            provider = SkijaFonts.provider();
            source = "Skija (Inter)";
        } catch (Throwable t) {
            // No Skija classes, no native, or a broken typeface: Minecraft's
            // font keeps the interface usable instead of failing the whole mod.
            provider = VanillaFonts.provider();
            source = "Minecraft font";
        }
        CACHE.clear();
    }

    /** True once a usable font provider exists. */
    public static synchronized boolean available() {
        load();
        return provider != null;
    }

    /** Human readable name of the active provider, for the startup log. */
    public static synchronized String source() {
        load();
        return source;
    }

    public static synchronized boolean usingSkija() {
        load();
        return provider instanceof SkijaFonts;
    }

    public static UiFont font(Weight weight, float size) {
        load();
        int key = (weight.ordinal() << 16) | (int) (size * 100f);
        UiFont cached = CACHE.get(key);
        if (cached == null) {
            cached = provider.font(weight, size);
            CACHE.put(key, cached);
        }
        return cached;
    }

    public static UiFont regular(float size) {
        return font(Weight.REGULAR, size);
    }

    public static UiFont medium(float size) {
        return font(Weight.MEDIUM, size);
    }

    public static UiFont semibold(float size) {
        return font(Weight.SEMIBOLD, size);
    }

    public static UiFont bold(float size) {
        return font(Weight.BOLD, size);
    }
}
