package dev.astatin.theme;

/**
 * Design tokens. Everything the client draws is derived from this palette and
 * the spacing scale, which keeps the HUD and the menus visually consistent.
 */
public final class Theme {
    private Theme() {
    }

    // Surfaces
    public static final int INK = 0xFF07080A;
    public static final int PANEL = 0xE6141519;
    public static final int PANEL_DEEP = 0xF20B0C0F;
    public static final int CARD = 0x0DFFFFFF;
    public static final int CARD_HOVER = 0x16FFFFFF;
    public static final int STROKE = 0x1FFFFFFF;
    public static final int STROKE_SOFT = 0x12FFFFFF;

    // Text
    public static final int TEXT = 0xFFF5F2EC;
    public static final int TEXT_SOFT = 0xFFB9BDC6;
    public static final int TEXT_DIM = 0xFF8C919C;
    public static final int TEXT_MUTED = 0xFF5E6470;
    public static final int TEXT_ON_ACCENT = 0xFF1B1206;

    // Accents
    public static final int AMBER = 0xFFFFA23A;
    public static final int AMBER_HI = 0xFFFFC873;
    public static final int AMBER_LO = 0xFFFF7A18;
    public static final int GREEN = 0xFF67E39B;
    public static final int RED = 0xFFFF6E6E;
    public static final int BLUE = 0xFF6EA8FF;

    // Metrics
    public static final float RADIUS_PANEL = 16f;
    public static final float RADIUS_CARD = 12f;
    public static final float RADIUS_CONTROL = 9f;
    public static final float GAP = 12f;
    public static final float PAD = 16f;

    private static int accent = AMBER;

    public static int accent() {
        return accent;
    }

    public static void setAccent(int argb) {
        accent = 0xFF000000 | (argb & 0xFFFFFF);
    }

    public static int accentSoft(float alpha) {
        return withAlpha(accent, alpha);
    }

    public static int accentLight(float t) {
        return mix(accent, 0xFFFFFFFF, t);
    }

    public static int accentDark(float t) {
        return mix(accent, 0xFF000000, t);
    }

    /** Multiplies the alpha channel of an ARGB colour. */
    public static int withAlpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * clamp(alpha, 0f, 1f));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int from, int to, float t) {
        return mix(from, to, t);
    }

    public static int mix(int from, int to, float t) {
        float f = clamp(t, 0f, 1f);
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * f);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static float[] rgb(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f
        };
    }

    public static int fromRgb(float r, float g, float b, float alpha) {
        return argb((int) (clamp(alpha, 0f, 1f) * 255f),
                (int) (clamp(r, 0f, 1f) * 255f),
                (int) (clamp(g, 0f, 1f) * 255f),
                (int) (clamp(b, 0f, 1f) * 255f));
    }

    public static String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
