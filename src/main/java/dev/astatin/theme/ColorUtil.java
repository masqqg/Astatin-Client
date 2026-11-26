package dev.astatin.theme;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static int hsv(float hue, float saturation, float value) {
        return hsv(hue, saturation, value, 1f);
    }

    public static int hsv(float hue, float saturation, float value, float alpha) {
        float h = ((hue % 360f) + 360f) % 360f;
        float s = Theme.clamp(saturation, 0f, 1f);
        float v = Theme.clamp(value, 0f, 1f);
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r;
        float g;
        float b;
        if (h < 60f) {
            r = c;
            g = x;
            b = 0f;
        } else if (h < 120f) {
            r = x;
            g = c;
            b = 0f;
        } else if (h < 180f) {
            r = 0f;
            g = c;
            b = x;
        } else if (h < 240f) {
            r = 0f;
            g = x;
            b = c;
        } else if (h < 300f) {
            r = x;
            g = 0f;
            b = c;
        } else {
            r = c;
            g = 0f;
            b = x;
        }
        return Theme.argb((int) (alpha * 255f), (int) ((r + m) * 255f),
                (int) ((g + m) * 255f), (int) ((b + m) * 255f));
    }

    public static float[] toHsv(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta < 0.00001f) {
            hue = 0f;
        } else if (max == r) {
            hue = 60f * (((g - b) / delta) % 6f);
        } else if (max == g) {
            hue = 60f * (((b - r) / delta) + 2f);
        } else {
            hue = 60f * (((r - g) / delta) + 4f);
        }
        if (hue < 0f) {
            hue += 360f;
        }
        float saturation = max <= 0.00001f ? 0f : delta / max;
        return new float[]{hue, saturation, max};
    }

    public static int withAlpha(int color, float alpha) {
        return Theme.withAlpha(color, alpha);
    }
}
