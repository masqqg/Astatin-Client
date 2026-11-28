package dev.astatin.render;

import dev.astatin.theme.SkijaFont;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The original Skija drawing path. It is only instantiated when the native
 * library loads; every primitive maps straight onto a Skia canvas so the menu,
 * the HUD and the title screen keep the exact same pixels as before.
 */
public final class SkiaBackend implements UiBackend {
    private final Canvas canvas;

    private final Paint fill = new Paint().setAntiAlias(true);
    private final Paint stroke = new Paint().setAntiAlias(true).setStroke(true);
    private final Paint textPaint = new Paint().setAntiAlias(true);
    private final Paint imagePaint = new Paint().setAntiAlias(true);
    private final Paint layerPaint = new Paint().setAntiAlias(true);

    private final Map<String, Shader> shaders = new LinkedHashMap<>();
    private final Map<Float, ImageFilter> blurFilters = new HashMap<>();
    private final Map<String, Image> images = new HashMap<>();

    public SkiaBackend(Canvas canvas) {
        this.canvas = canvas;
        stroke.setStrokeCap(PaintStrokeCap.ROUND).setStrokeJoin(PaintStrokeJoin.ROUND);
    }

    public Canvas canvas() {
        return canvas;
    }

    // ------------------------------------------------------------ images

    @Override
    public float imageWidth(String path) {
        return image(path).getWidth();
    }

    @Override
    public float imageHeight(String path) {
        return image(path).getHeight();
    }

    private Image image(String path) {
        return images.computeIfAbsent(path, key -> {
            try (InputStream in = SkiaBackend.class.getResourceAsStream(key)) {
                if (in == null) {
                    throw new IllegalStateException("Missing image " + key);
                }
                Image image = Image.makeFromEncoded(in.readAllBytes());
                if (image == null) {
                    throw new IllegalStateException("Could not decode image " + key);
                }
                return image;
            } catch (Exception e) {
                throw new IllegalStateException("Could not load image " + key, e);
            }
        });
    }

    @Override
    public void image(String path, float l, float t, float r, float b,
                      float u0, float v0, float u1, float v1) {
        Image image = image(path);
        float srcW = image.getWidth();
        float srcH = image.getHeight();
        imagePaint.setShader(null).setImageFilter(null).setColor(0xFFFFFFFF);
        canvas.drawImageRect(image,
                Rect.makeLTRB(srcW * u0, srcH * v0, srcW * u1, srcH * v1),
                Rect.makeLTRB(l, t, r, b), imagePaint, true);
    }

    // ------------------------------------------------------------- state

    @Override
    public void save() {
        canvas.save();
    }

    @Override
    public void restore() {
        canvas.restore();
    }

    @Override
    public void translate(float x, float y) {
        canvas.translate(x, y);
    }

    @Override
    public void scale(float sx, float sy) {
        canvas.scale(sx, sy);
    }

    @Override
    public void clipRect(float l, float t, float r, float b) {
        canvas.clipRect(Rect.makeLTRB(l, t, r, b));
    }

    @Override
    public void clipRoundRect(float l, float t, float r, float b, float radius) {
        canvas.clipRRect(RRect.makeLTRB(l, t, r, b, radius));
    }

    @Override
    public void pushAlpha(float alpha, float l, float t, float r, float b) {
        layerPaint.setShader(null).setImageFilter(null).setAlphaf(Math.max(0f, Math.min(1f, alpha)));
        canvas.saveLayer(Rect.makeLTRB(l, t, r, b), layerPaint);
    }

    @Override
    public void popAlpha() {
        canvas.restore();
    }

    // -------------------------------------------------------- primitives

    @Override
    public void rect(float l, float t, float r, float b, int color) {
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawRect(Rect.makeLTRB(l, t, r, b), fill);
    }

    @Override
    public void roundRect(float l, float t, float r, float b, float radius, int color) {
        if (r - l <= 0.01f || b - t <= 0.01f) {
            return;
        }
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), fill);
    }

    @Override
    public void roundRectStroke(float l, float t, float r, float b, float radius, float lineWidth, int color) {
        if (r - l <= 0.01f || b - t <= 0.01f) {
            return;
        }
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), stroke);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawLine(x1, y1, x2, y2, stroke);
    }

    @Override
    public void circle(float cx, float cy, float radius, int color) {
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawCircle(cx, cy, radius, fill);
    }

    @Override
    public void circleStroke(float cx, float cy, float radius, float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawCircle(cx, cy, radius, stroke);
    }

    @Override
    public void oval(float l, float t, float r, float b, int color) {
        fill.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawOval(Rect.makeLTRB(l, t, r, b), fill);
    }

    @Override
    public void ovalStroke(float l, float t, float r, float b, float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawOval(Rect.makeLTRB(l, t, r, b), stroke);
    }

    @Override
    public void arc(float cx, float cy, float radius, float start, float sweep, boolean pie,
                    float lineWidth, int color) {
        stroke.setShader(null).setImageFilter(null).setColor(color).setStrokeWidth(lineWidth);
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, start, sweep, pie, stroke);
    }

    @Override
    public void linearGradient(float l, float t, float r, float b, float radius,
                               float x0, float y0, float x1, float y1, int from, int to) {
        String key = "g:" + key(l, t, r, b, radius, x0, y0, x1, y1) + from + ":" + to;
        Shader shader = shaders.get(key);
        if (shader == null) {
            shader = Shader.makeLinearGradient(x0, y0, x1, y1, new int[]{from, to});
            shaders.put(key, shader);
        }
        if (shaders.size() > 600) {
            fill.setShader(null);
            stroke.setShader(null);
            textPaint.setShader(null);
            imagePaint.setShader(null);
            for (Shader cached : shaders.values()) {
                cached.close();
            }
            shaders.clear();
            return;
        }
        fill.setImageFilter(null).setShader(shader).setColor(0xFFFFFFFF);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), fill);
    }

    @Override
    public void radialGradient(float cx, float cy, float radius, int center, int edge) {
        String key = "r:" + key(cx, cy, radius) + center + ":" + edge;
        Shader shader = shaders.get(key);
        if (shader == null) {
            shader = Shader.makeRadialGradient(cx, cy, radius, new int[]{center, edge});
            shaders.put(key, shader);
        }
        fill.setImageFilter(null).setShader(shader).setColor(0xFFFFFFFF);
        canvas.drawRect(Rect.makeLTRB(cx - radius, cy - radius, cx + radius, cy + radius), fill);
    }

    @Override
    public void shadow(float l, float t, float r, float b, float radius, float sigma, int color) {
        ImageFilter filter = blurFilters.computeIfAbsent(Math.max(1f, sigma),
                s -> ImageFilter.makeBlur(s, s, FilterTileMode.DECAL));
        fill.setShader(null).setColor(color).setImageFilter(filter);
        canvas.drawRRect(RRect.makeLTRB(l, t, r, b, radius), fill);
        fill.setImageFilter(null);
    }

    private static String key(float... values) {
        StringBuilder builder = new StringBuilder(values.length * 7);
        for (float value : values) {
            builder.append((int) (value * 10f)).append('.');
        }
        return builder.toString();
    }

    // -------------------------------------------------------------- text

    @Override
    public void text(String value, float x, float baselineY, UiFont font, int color) {
        if (value == null || value.isEmpty() || !(font instanceof SkijaFont skija)) {
            return;
        }
        textPaint.setShader(null).setImageFilter(null).setColor(color);
        canvas.drawString(value, x, baselineY, skija.handle(), textPaint);
    }

    // -------------------------------------------------------------- close

    @Override
    public void close() {
        for (Shader shader : shaders.values()) {
            shader.close();
        }
        shaders.clear();
        for (ImageFilter filter : blurFilters.values()) {
            filter.close();
        }
        blurFilters.clear();
        for (Image image : images.values()) {
            image.close();
        }
        images.clear();
        fill.close();
        stroke.close();
        textPaint.close();
        imagePaint.close();
        layerPaint.close();
    }
}
