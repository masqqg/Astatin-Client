package dev.astatin.render;

import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The single drawing surface used by the HUD, the menus and the title screen.
 * Every primitive is designed to be cheap enough to run at frame rate: paints
 * and fonts are reused, gradients and blur filters are cached, and only a handful
 * of native objects are created on a normal frame.
 */
public final class Render2D {
    private final Canvas canvas;

    private final Paint fill = new Paint().setAntiAlias(true);
    private final Paint stroke = new Paint().setAntiAlias(true).setStroke(true);
    private final Paint textPaint = new Paint().setAntiAlias(true);
    private final Paint imagePaint = new Paint().setAntiAlias(true);

    private final Map<String, Shader> shaders = new LinkedHashMap<>();
    private final Map<Float, ImageFilter> blurFilters = new HashMap<>();
    private final Map<String, Image> images = new HashMap<>();
    private final IdentityHashMap<Font, Map<String, Float>> measurements = new IdentityHashMap<>();

    private float width;
    private float height;

    public Render2D(Canvas canvas) {
        this.canvas = canvas;
        stroke.setStrokeCap(PaintStrokeCap.ROUND).setStrokeJoin(PaintStrokeJoin.ROUND);
    }

    public Canvas canvas() {
        return canvas;
    }

    public void beginFrame(float width, float height) {
        this.width = width;
        this.height = height;
        if (shaders.size() > 600) {
            for (Shader shader : shaders.values()) {
                shader.close();
            }
            shaders.clear();
        }
        for (Map<String, Float> values : measurements.values()) {
            if (values.size() > 800) {
                values.clear();
            }
        }
    }

    public void endFrame() {
        fill.setShader(null).setImageFilter(null).setAlpha(255);
        stroke.setShader(null).setImageFilter(null).setAlpha(255);
        textPaint.setShader(null).setImageFilter(null).setAlpha(255);
        imagePaint.setShader(null).setImageFilter(null).setAlpha(255);
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    // ---------------------------------------------------------------- state

    public void save() {
        canvas.save();
    }

    public void restore() {
        canvas.restore();
    }

    public void translate(float x, float y) {
        canvas.translate(x, y);
    }

    public void scale(float sx, float sy) {
        canvas.scale(sx, sy);
    }

    public void clipRect(float l, float t, float r, float b) {
        canvas.clipRect(Rect.makeLTRB(l, t, r, b));
    }

    public void clipRoundRect(float l, float t, float r, float b, float radius) {
        canvas.clipRRect(RRect.makeLTRB(l, t, r, b, radius));
    }

    // ----------------------------------------------------------- primitives

    public void rect(float l, float t, float r, float b, int color) {
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawRect(Rect.makeLTRB(l, t, r, b), fill);
    }

    public void roundRect(float l, float t, float r, float b, float radius, int color) {
        if (r - l <= 0.01f || b - t <= 0.01f) {
            return;
        }
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), fill);
    }

    public void roundRectStroke(float l, float t, float r, float b, float radius, float lineWidth, int color) {
        if (r - l <= 0.01f || b - t <= 0.01f) {
            return;
        }
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), stroke);
    }

    public void line(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawLine(x1, y1, x2, y2, stroke);
    }

    public void circle(float cx, float cy, float radius, int color) {
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawCircle(cx, cy, radius, fill);
    }

    public void circleStroke(float cx, float cy, float radius, float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawCircle(cx, cy, radius, stroke);
    }

    public void oval(float l, float t, float r, float b, int color) {
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawOval(Rect.makeLTRB(l, t, r, b), fill);
    }

    public void ovalStroke(float l, float t, float r, float b, float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawOval(Rect.makeLTRB(l, t, r, b), stroke);
    }

    public void arc(float cx, float cy, float radius, float start, float sweep, boolean pie, float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, start, sweep, pie, stroke);
    }

    public void gradientRect(float l, float t, float r, float b, float radius, int top, int bottom) {
        Shader shader = linear("g:" + key(l, t, r, b, radius) + top + ":" + bottom,
                l, t, l, b, new int[]{top, bottom});
        fill.setImageFilter(null).setShader(shader).setColor(0xFFFFFFFF);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), fill);
    }

    public void gradientRectHorizontal(float l, float t, float r, float b, float radius, int left, int right) {
        Shader shader = linear("gh:" + key(l, t, r, b, radius) + left + ":" + right,
                l, t, r, t, new int[]{left, right});
        fill.setImageFilter(null).setShader(shader).setColor(0xFFFFFFFF);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), fill);
    }

    public void radialGlow(float cx, float cy, float radius, int center, int edge) {
        Shader shader = radial("r:" + key(cx, cy, radius) + center + ":" + edge, cx, cy, radius, new int[]{center, edge});
        fill.setImageFilter(null).setShader(shader).setColor(0xFFFFFFFF);
        canvas.drawRect(Rect.makeLTRB(cx - radius, cy - radius, cx + radius, cy + radius), fill);
    }

    private Shader linear(String key, float x0, float y0, float x1, float y1, int[] colors) {
        Shader cached = shaders.get(key);
        if (cached == null) {
            cached = Shader.makeLinearGradient(x0, y0, x1, y1, colors);
            shaders.put(key, cached);
        }
        return cached;
    }

    private Shader radial(String key, float cx, float cy, float radius, int[] colors) {
        Shader cached = shaders.get(key);
        if (cached == null) {
            cached = Shader.makeRadialGradient(cx, cy, radius, colors);
            shaders.put(key, cached);
        }
        return cached;
    }

    private static String key(float... values) {
        StringBuilder builder = new StringBuilder(values.length * 7);
        for (float value : values) {
            builder.append((int) (value * 10f)).append('.');
        }
        return builder.toString();
    }

    /** Expands a rounded rect with a gaussian shadow. */
    public void shadow(float l, float t, float r, float b, float radius, float sigma, int color) {
        if (((color >>> 24) & 0xFF) == 0) {
            return;
        }
        ImageFilter filter = blurFilters.computeIfAbsent(Math.max(1f, sigma),
                s -> ImageFilter.makeBlur(s, s, FilterTileMode.DECAL));
        fill.setShader(null).setColor(color).setImageFilter(filter);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), fill);
        fill.setImageFilter(null);
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

    public void text(String value, float x, float baselineY, Font font, int color) {
        textPaint.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawString(value, x, baselineY, font, textPaint);
    }

    public void textTop(String value, float x, float topY, Font font, int color) {
        float baseline = topY - font.getMetrics().getAscent();
        text(value, x, baseline, font, color);
    }

    public void textCenterY(String value, float x, float centerY, Font font, int color) {
        float baseline = centerY - (font.getMetrics().getAscent() + font.getMetrics().getDescent()) * 0.5f;
        text(value, x, baseline, font, color);
    }

    public void textCentered(String value, float centerX, float baselineY, Font font, int color) {
        text(value, centerX - width(value, font) * 0.5f, baselineY, font, color);
    }

    public void textCenteredTop(String value, float centerX, float topY, Font font, int color) {
        text(value, centerX - width(value, font) * 0.5f, topY - font.getMetrics().getAscent(), font, color);
    }

    public void textRight(String value, float rightX, float baselineY, Font font, int color) {
        text(value, rightX - width(value, font), baselineY, font, color);
    }

    public void textCenterYRight(String value, float rightX, float centerY, Font font, int color) {
        float baseline = centerY - (font.getMetrics().getAscent() + font.getMetrics().getDescent()) * 0.5f;
        text(value, rightX - width(value, font), baseline, font, color);
    }

    /** Letter-spaced upper-case micro labels. */
    public void textTracked(String value, float x, float baselineY, Font font, int color, float tracking) {
        float cursor = x;
        for (int i = 0; i < value.length(); i++) {
            String ch = String.valueOf(value.charAt(i));
            text(ch, cursor, baselineY, font, color);
            cursor += width(ch, font) + tracking;
        }
    }

    public float trackedWidth(String value, Font font, float tracking) {
        float result = 0f;
        for (int i = 0; i < value.length(); i++) {
            result += width(String.valueOf(value.charAt(i)), font) + tracking;
        }
        return result;
    }

    public float width(String value, Font font) {
        return measurements.computeIfAbsent(font, f -> new HashMap<>())
                .computeIfAbsent(value, v -> font.measureTextWidth(v));
    }

    public float lineHeight(Font font) {
        return font.getMetrics().getHeight();
    }

    // --------------------------------------------------------------- images

    public Image image(String resourcePath) {
        return images.computeIfAbsent(resourcePath, path -> {
            try (InputStream in = Render2D.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("Missing image " + path);
                }
                Image image = Image.makeFromEncoded(in.readAllBytes());
                if (image == null) {
                    throw new IllegalStateException("Could not decode image " + path);
                }
                return image;
            } catch (Exception e) {
                throw new IllegalStateException("Could not load image " + path, e);
            }
        });
    }

    public void image(String resourcePath, float l, float t, float r, float b) {
        Image image = image(resourcePath);
        imagePaint.setShader(null).setImageFilter(null).setColor(0xFFFFFFFF);
        canvas.drawImageRect(image, Rect.makeLTRB(l, t, r, b), imagePaint);
    }

    /** Draws an image contained (not cropped) inside the rect, centred. */
    public void imageFit(String resourcePath, float l, float t, float r, float b) {
        Image image = image(resourcePath);
        float dstW = r - l;
        float dstH = b - t;
        float scale = Math.min(dstW / image.getWidth(), dstH / image.getHeight());
        float drawW = image.getWidth() * scale;
        float drawH = image.getHeight() * scale;
        float x = l + (dstW - drawW) * 0.5f;
        float y = t + (dstH - drawH) * 0.5f;
        image(resourcePath, x, y, x + drawW, y + drawH);
    }

    public void imageCover(String resourcePath, float l, float t, float r, float b) {
        Image image = image(resourcePath);
        float dstW = r - l;
        float dstH = b - t;
        float srcW = image.getWidth();
        float srcH = image.getHeight();
        float scale = Math.max(dstW / srcW, dstH / srcH);
        float cropW = dstW / scale;
        float cropH = dstH / scale;
        float cropX = (srcW - cropW) * 0.5f;
        float cropY = (srcH - cropH) * 0.5f;
        imagePaint.setShader(null).setImageFilter(null).setColor(0xFFFFFFFF);
        canvas.drawImageRect(image, Rect.makeLTRB(cropX, cropY, cropX + cropW, cropY + cropH),
                Rect.makeLTRB(l, t, r, b), SamplingMode.MITCHELL, imagePaint, true);
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

    // --------------------------------------------------------------- helpers

    public Fonts.Weight weightFor(String name) {
        return switch (name) {
            case "medium" -> Fonts.Weight.MEDIUM;
            case "semibold" -> Fonts.Weight.SEMIBOLD;
            case "bold" -> Fonts.Weight.BOLD;
            case "italic" -> Fonts.Weight.ITALIC;
            default -> Fonts.Weight.REGULAR;
        };
    }
}
