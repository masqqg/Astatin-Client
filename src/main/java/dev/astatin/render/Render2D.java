package dev.astatin.render;

import dev.astatin.theme.Theme;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The single drawing surface used by the HUD, the menus and the title screen.
 * It owns the client's visual language (glass panels, bars, tracked labels) and
 * forwards every primitive to a {@link UiBackend}, so the exact same layout runs
 * on Skija and on Minecraft's own renderer.
 *
 * Paints, fonts and measurements are reused, and only a handful of objects are
 * created on a normal frame.
 */
public final class Render2D {
    private final UiBackend backend;
    private final IdentityHashMap<UiFont, Map<String, Float>> measurements = new IdentityHashMap<>();

    private float width;
    private float height;

    public Render2D(UiBackend backend) {
        this.backend = backend;
    }

    public UiBackend backend() {
        return backend;
    }

    public void beginFrame(float width, float height) {
        this.width = width;
        this.height = height;
        for (Map<String, Float> values : measurements.values()) {
            if (values.size() > 800) {
                values.clear();
            }
        }
    }

    public void endFrame() {
        // Backends restore their own state in end()/close().
    }

    public void close() {
        backend.close();
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    // ---------------------------------------------------------------- state

    public void save() {
        backend.save();
    }

    public void restore() {
        backend.restore();
    }

    public void translate(float x, float y) {
        backend.translate(x, y);
    }

    public void scale(float sx, float sy) {
        backend.scale(sx, sy);
    }

    public void clipRect(float l, float t, float r, float b) {
        backend.clipRect(l, t, r, b);
    }

    public void clipRoundRect(float l, float t, float r, float b, float radius) {
        backend.clipRoundRect(l, t, r, b, radius);
    }

    /**
     * Groups everything drawn until {@link #popLayer()} behind one opacity, used
     * for the menu's open and inspector animations.
     */
    public void pushLayer(float alpha) {
        backend.pushAlpha(alpha, 0f, 0f, width, height);
    }

    public void popLayer() {
        backend.popAlpha();
    }

    // ----------------------------------------------------------- primitives

    public void rect(float l, float t, float r, float b, int color) {
        backend.rect(l, t, r, b, color);
    }

    public void roundRect(float l, float t, float r, float b, float radius, int color) {
        backend.roundRect(l, t, r, b, radius, color);
    }

    public void roundRectStroke(float l, float t, float r, float b, float radius, float lineWidth, int color) {
        backend.roundRectStroke(l, t, r, b, radius, lineWidth, color);
    }

    public void line(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        backend.line(x1, y1, x2, y2, lineWidth, color);
    }

    public void circle(float cx, float cy, float radius, int color) {
        backend.circle(cx, cy, radius, color);
    }

    public void circleStroke(float cx, float cy, float radius, float lineWidth, int color) {
        backend.circleStroke(cx, cy, radius, lineWidth, color);
    }

    public void oval(float l, float t, float r, float b, int color) {
        backend.oval(l, t, r, b, color);
    }

    public void ovalStroke(float l, float t, float r, float b, float lineWidth, int color) {
        backend.ovalStroke(l, t, r, b, lineWidth, color);
    }

    public void arc(float cx, float cy, float radius, float start, float sweep, boolean pie, float lineWidth, int color) {
        backend.arc(cx, cy, radius, start, sweep, pie, lineWidth, color);
    }

    public void gradientRect(float l, float t, float r, float b, float radius, int top, int bottom) {
        backend.linearGradient(l, t, r, b, radius, l, t, l, b, top, bottom);
    }

    public void gradientRectHorizontal(float l, float t, float r, float b, float radius, int left, int right) {
        backend.linearGradient(l, t, r, b, radius, l, t, r, t, left, right);
    }

    public void radialGlow(float cx, float cy, float radius, int center, int edge) {
        backend.radialGradient(cx, cy, radius, center, edge);
    }

    /** Expands a rounded rect with a gaussian shadow. */
    public void shadow(float l, float t, float r, float b, float radius, float sigma, int color) {
        if (((color >>> 24) & 0xFF) == 0) {
            return;
        }
        backend.shadow(l, t, r, b, radius, Math.max(1f, sigma), color);
    }

    // ---------------------------------------------------------------- glass

    /**
     * Frosted glass panel used all over the client. It layers a soft shadow, a
     * dark vertical gradient, a top light line and a hairline border; the result
     * reads as glass without ever sampling the framebuffer.
     */
    public void glass(float l, float t, float r, float b, float radius) {
        glass(l, t, r, b, radius, 0x66000000, 12f);
    }

    public void glass(float l, float t, float r, float b, float radius, int shadowColor, float sigma) {
        if (r - l <= 0.01f || b - t <= 0.01f) {
            return;
        }
        shadow(l + 1f, t + 4f, r - 1f, b + 6f, radius, sigma, shadowColor);
        int top = 0xE61A1B20;
        int bottom = 0xED0D0E11;
        gradientRect(l, t, r, b, radius, top, bottom);

        // subtle warm bloom in the top-left corner
        save();
        clipRoundRect(l, t, r, b, radius);
        radialGlow(l + (r - l) * 0.18f, t + 2f, Math.min(r - l, b - t) * 0.85f,
                Theme.accentSoft(0.055f), Theme.accentSoft(0f));
        restore();

        // top specular line
        save();
        clipRoundRect(l, t, r, b, radius);
        gradientRectHorizontal(l, t, r, t + 1.2f, 0f,
                0x00FFFFFF, 0x1AFFFFFF);
        restore();

        roundRectStroke(l + 0.5f, t + 0.5f, r - 0.5f, b - 0.5f, radius, 1f, Theme.STROKE);
        roundRectStroke(l + 1.5f, t + 1.5f, r - 1.5f, b - 1.5f, Math.max(1f, radius - 1f), 1f, Theme.STROKE_SOFT);
    }

    public void accentEdge(float l, float t, float r, float b, float radius) {
        save();
        clipRoundRect(l, t, r, b, radius);
        gradientRectHorizontal(l, t, r, t + 1.4f, 0f,
                Theme.accentSoft(0f), Theme.accentSoft(0.55f));
        restore();
    }

    // ---------------------------------------------------------------- text

    public void text(String value, float x, float baselineY, UiFont font, int color) {
        backend.text(value, x, baselineY, font, color);
    }

    public void textTop(String value, float x, float topY, UiFont font, int color) {
        float baseline = topY - font.ascent();
        text(value, x, baseline, font, color);
    }

    public void textCenterY(String value, float x, float centerY, UiFont font, int color) {
        float baseline = centerY - (font.ascent() + font.descent()) * 0.5f;
        text(value, x, baseline, font, color);
    }

    public void textCentered(String value, float centerX, float baselineY, UiFont font, int color) {
        text(value, centerX - width(value, font) * 0.5f, baselineY, font, color);
    }

    public void textCenteredTop(String value, float centerX, float topY, UiFont font, int color) {
        text(value, centerX - width(value, font) * 0.5f, topY - font.ascent(), font, color);
    }

    public void textRight(String value, float rightX, float baselineY, UiFont font, int color) {
        text(value, rightX - width(value, font), baselineY, font, color);
    }

    public void textCenterYRight(String value, float rightX, float centerY, UiFont font, int color) {
        float baseline = centerY - (font.ascent() + font.descent()) * 0.5f;
        text(value, rightX - width(value, font), baseline, font, color);
    }

    /** Letter-spaced upper-case micro labels. */
    public void textTracked(String value, float x, float baselineY, UiFont font, int color, float tracking) {
        float cursor = x;
        for (int i = 0; i < value.length(); i++) {
            String ch = String.valueOf(value.charAt(i));
            text(ch, cursor, baselineY, font, color);
            cursor += width(ch, font) + tracking;
        }
    }

    public float trackedWidth(String value, UiFont font, float tracking) {
        float result = 0f;
        for (int i = 0; i < value.length(); i++) {
            result += width(String.valueOf(value.charAt(i)), font) + tracking;
        }
        return result;
    }

    public float width(String value, UiFont font) {
        if (value == null || value.isEmpty()) {
            return 0f;
        }
        return measurements.computeIfAbsent(font, f -> new HashMap<>())
                .computeIfAbsent(value, font::measureWidth);
    }

    public float lineHeight(UiFont font) {
        return font.height();
    }

    // --------------------------------------------------------------- images

    public void image(String resourcePath, float l, float t, float r, float b) {
        backend.image(resourcePath, l, t, r, b, 0f, 0f, 1f, 1f);
    }

    /** Draws an image contained (not cropped) inside the rect, centred. */
    public void imageFit(String resourcePath, float l, float t, float r, float b) {
        float srcW = backend.imageWidth(resourcePath);
        float srcH = backend.imageHeight(resourcePath);
        if (srcW <= 0f || srcH <= 0f) {
            return;
        }
        float dstW = r - l;
        float dstH = b - t;
        float scale = Math.min(dstW / srcW, dstH / srcH);
        float drawW = srcW * scale;
        float drawH = srcH * scale;
        float x = l + (dstW - drawW) * 0.5f;
        float y = t + (dstH - drawH) * 0.5f;
        backend.image(resourcePath, x, y, x + drawW, y + drawH, 0f, 0f, 1f, 1f);
    }

    public void imageCover(String resourcePath, float l, float t, float r, float b) {
        float srcW = backend.imageWidth(resourcePath);
        float srcH = backend.imageHeight(resourcePath);
        if (srcW <= 0f || srcH <= 0f) {
            return;
        }
        float dstW = r - l;
        float dstH = b - t;
        if (dstW <= 0f || dstH <= 0f) {
            return;
        }
        float scale = Math.max(dstW / srcW, dstH / srcH);
        float cropW = dstW / scale;
        float cropH = dstH / scale;
        float cropX = (srcW - cropW) * 0.5f;
        float cropY = (srcH - cropH) * 0.5f;
        backend.image(resourcePath, l, t, r, b,
                cropX / srcW, cropY / srcH, (cropX + cropW) / srcW, (cropY + cropH) / srcH);
    }

    // ----------------------------------------------------------------- bars

    public void bar(float l, float t, float r, float b, float radius, float progress, int background, int foreground) {
        roundRect(l, t, r, b, radius, background);
        float clamped = Math.max(0f, Math.min(1f, progress));
        if (clamped > 0.001f) {
            float end = l + (r - l) * clamped;
            if (end - l < radius * 2f) {
                roundRect(l, t, Math.max(end, l + 0.5f), b, Math.min(radius, (end - l) * 0.5f), foreground);
            } else {
                roundRect(l, t, end, b, radius, foreground);
            }
        }
    }

    public void barGradient(float l, float t, float r, float b, float radius, float progress) {
        roundRect(l, t, r, b, radius, 0x24FFFFFF);
        float clamped = Math.max(0f, Math.min(1f, progress));
        if (clamped > 0.001f) {
            float end = l + (r - l) * clamped;
            gradientRectHorizontal(l, t, end, b, Math.min(radius, (end - l) * 0.5f),
                    Theme.accentLight(0.25f), Theme.accentDark(0.1f));
        }
    }
}
