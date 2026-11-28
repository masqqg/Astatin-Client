package dev.astatin.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.VanillaFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws the client with Minecraft's own {@link GuiGraphics}. This is the path
 * used on systems where the Skija native library cannot be loaded (Android
 * launchers in particular), so the HUD, the menu and the title screen still work
 * there. The layout, colours and timings are the shared ones; only the
 * primitives differ: rounded shapes become scanline spans, gradients become
 * bands and Skia's blur filters become stacked translucent layers.
 */
public final class VanillaBackend implements UiBackend {
    private static final int GRADIENT_STEPS = 96;
    private static final int MAX_CACHED_TEXTURES = 24;
    private static final float VANILLA_ASCENT = 7f;

    private static final Map<String, NativeImage> RAW_IMAGES = new HashMap<>();
    private static final Map<String, Tex> TEXTURES = new HashMap<>();

    /** A registered GUI texture together with the size of its pixels. */
    private record Tex(Identifier id, int width, int height) {
    }

    private final GuiGraphics gui;

    private final Deque<float[]> states = new ArrayDeque<>();
    private final Deque<Float> alphas = new ArrayDeque<>();

    private float tx;
    private float ty;
    private float sx = 1f;
    private float sy = 1f;
    private float alpha = 1f;
    private int clipDepth;

    public VanillaBackend(GuiGraphics gui) {
        this.gui = gui;
    }

    // ------------------------------------------------------------ images

    private static NativeImage raw(String path) {
        NativeImage cached = RAW_IMAGES.get(path);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = VanillaBackend.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            NativeImage image = NativeImage.read(in);
            RAW_IMAGES.put(path, image);
            return image;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public float imageWidth(String path) {
        NativeImage image = raw(path);
        return image == null ? 0f : image.getWidth();
    }

    @Override
    public float imageHeight(String path) {
        NativeImage image = raw(path);
        return image == null ? 0f : image.getHeight();
    }

    /**
     * Registers a cropped copy of the bundled artwork as a GUI texture. The
     * fallback path only needs whole-texture quads, so the cover crop is baked
     * into its own texture and cached by crop.
     */
    private Tex texture(String path, float u0, float v0, float u1, float v1) {
        NativeImage source = raw(path);
        if (source == null) {
            return null;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        int x0 = clamp(Math.round(u0 * w), 0, w);
        int y0 = clamp(Math.round(v0 * h), 0, h);
        int x1 = clamp(Math.round(u1 * w), 0, w);
        int y1 = clamp(Math.round(v1 * h), 0, h);
        boolean whole = x0 == 0 && y0 == 0 && x1 == w && y1 == h;
        String key = path + "@" + (whole ? "full" : x0 + "," + y0 + "," + x1 + "," + y1);
        Tex cached = TEXTURES.get(key);
        if (cached != null) {
            return cached;
        }
        if (TEXTURES.size() > MAX_CACHED_TEXTURES) {
            TEXTURES.clear();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        try {
            int cw = Math.max(1, x1 - x0);
            int ch = Math.max(1, y1 - y0);
            NativeImage pixels;
            if (whole) {
                pixels = source;
            } else {
                pixels = new NativeImage(NativeImage.Format.RGBA, cw, ch, false);
                for (int y = 0; y < ch; y++) {
                    for (int x = 0; x < cw; x++) {
                        pixels.setPixel(x, y, source.getPixel(x0 + x, y0 + y));
                    }
                }
            }
            Identifier id = Identifier.fromNamespaceAndPath("astatin",
                    "generated/ui_" + Integer.toHexString(key.hashCode()));
            DynamicTexture texture = new DynamicTexture(() -> "astatin ui " + path, pixels);
            mc.getTextureManager().register(id, texture);
            if (texture.getTextureView() == null) {
                texture.upload();
            }
            // The DynamicTexture owns its pixels from here on; the cached source
            // image of a whole-texture draw must stay alive, which it does.
            Tex tex = new Tex(id, cw, ch);
            TEXTURES.put(key, tex);
            return tex;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void image(String path, float l, float t, float r, float b,
                      float u0, float v0, float u1, float v1) {
        Tex tex = texture(path, u0, v0, u1, v1);
        if (tex == null) {
            return;
        }
        int x0 = ix(l);
        int y0 = iy(t);
        int x1 = ix(r);
        int y1 = iy(b);
        if (x1 - x0 < 1 || y1 - y0 < 1) {
            return;
        }
        int tint = (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | 0xFFFFFF;
        // A textured blit, not fill(): a coloured rectangle carries no UVs and
        // the GUI_TEXTURED pipeline would reject it.
        gui.blit(RenderPipelines.GUI_TEXTURED, tex.id(), x0, y0, 0f, 0f,
                x1 - x0, y1 - y0, tex.width(), tex.height(),
                tex.width(), tex.height(), tint);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    // ------------------------------------------------------------- state

    private int ix(float x) {
        return Math.round(tx + x * sx);
    }

    private int iy(float y) {
        return Math.round(ty + y * sy);
    }

    private float fx(float x) {
        return tx + x * sx;
    }

    private float fy(float y) {
        return ty + y * sy;
    }

    @Override
    public void save() {
        states.push(new float[]{tx, ty, sx, sy, clipDepth});
    }

    @Override
    public void restore() {
        float[] state = states.poll();
        if (state == null) {
            return;
        }
        tx = state[0];
        ty = state[1];
        sx = state[2];
        sy = state[3];
        int depth = (int) state[4];
        while (clipDepth > depth) {
            gui.disableScissor();
            clipDepth--;
        }
    }

    @Override
    public void translate(float x, float y) {
        tx += x * sx;
        ty += y * sy;
    }

    @Override
    public void scale(float scaleX, float scaleY) {
        sx *= scaleX;
        sy *= scaleY;
    }

    @Override
    public void clipRect(float l, float t, float r, float b) {
        int x0 = ix(l);
        int y0 = iy(t);
        int x1 = ix(r);
        int y1 = iy(b);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        gui.enableScissor(x0, y0, x1, y1);
        clipDepth++;
    }

    @Override
    public void clipRoundRect(float l, float t, float r, float b, float radius) {
        // Scissor rectangles cannot be rounded; the bounding box is close
        // enough for the few places the client clips a rounded panel.
        clipRect(l, t, r, b);
    }

    @Override
    public void pushAlpha(float value, float l, float t, float r, float b) {
        alphas.push(alpha);
        alpha *= Math.max(0f, Math.min(1f, value));
    }

    @Override
    public void popAlpha() {
        Float previous = alphas.poll();
        alpha = previous == null ? 1f : previous;
    }

    private int color(int argb) {
        if (alpha >= 0.999f) {
            return argb;
        }
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0f, alpha));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    // -------------------------------------------------------- primitives

    @Override
    public void rect(float l, float t, float r, float b, int argb) {
        int x0 = ix(l);
        int y0 = iy(t);
        int x1 = ix(r);
        int y1 = iy(b);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        gui.fill(x0, y0, x1, y1, color(argb));
    }

    /** How far a rounded edge has receded at position {@code pos}. */
    private static float inset(float pos, float lo, float hi, float radius) {
        if (radius <= 0.01f || hi - lo <= 0.01f) {
            return 0f;
        }
        float flatLo = lo + radius;
        float flatHi = hi - radius;
        if (pos >= flatLo && pos <= flatHi) {
            return 0f;
        }
        float distance = pos < flatLo ? flatLo - pos : pos - flatHi;
        distance = Math.min(distance, radius);
        return radius - (float) Math.sqrt(Math.max(0f, radius * radius - distance * distance));
    }

    private float radiusPx(float radius) {
        return Math.max(0f, radius * Math.min(sx, sy));
    }

    @Override
    public void roundRect(float l, float t, float r, float b, float radius, int argb) {
        int x0 = ix(l);
        int y0 = iy(t);
        int x1 = ix(r);
        int y1 = iy(b);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }
        int c = color(argb);
        float rad = Math.min(radiusPx(radius), Math.min((x1 - x0), (y1 - y0)) * 0.5f);
        if (rad < 0.75f) {
            gui.fill(x0, y0, x1, y1, c);
            return;
        }
        int flatTop = Math.round(y0 + rad);
        int flatBottom = Math.round(y1 - rad);
        if (flatBottom > flatTop) {
            gui.fill(x0, flatTop, x1, flatBottom, c);
        }
        for (int y = y0; y < Math.min(flatTop, y1); y++) {
            float ins = inset(y + 0.5f, y0, y1, rad);
            int left = Math.round(x0 + ins);
            int right = Math.round(x1 - ins);
            if (right > left) {
                gui.fill(left, y, right, y + 1, c);
            }
        }
        for (int y = Math.max(flatBottom, y0); y < y1; y++) {
            float ins = inset(y + 0.5f, y0, y1, rad);
            int left = Math.round(x0 + ins);
            int right = Math.round(x1 - ins);
            if (right > left) {
                gui.fill(left, y, right, y + 1, c);
            }
        }
    }

    @Override
    public void roundRectStroke(float l, float t, float r, float b, float radius,
                                float lineWidth, int argb) {
        int x0 = ix(l);
        int y0 = iy(t);
        int x1 = ix(r);
        int y1 = iy(b);
        int w = Math.max(1, Math.round(lineWidth * Math.min(sx, sy)));
        if (x1 - x0 <= w * 2 || y1 - y0 <= w * 2) {
            roundRect(l, t, r, b, radius, argb);
            return;
        }
        int c = color(argb);
        float rad = Math.min(radiusPx(radius), Math.min((x1 - x0), (y1 - y0)) * 0.5f);
        float innerRad = Math.max(0f, rad - w);
        if (rad < 0.75f) {
            gui.fill(x0, y0, x1, y0 + w, c);
            gui.fill(x0, y1 - w, x1, y1, c);
            gui.fill(x0, y0, x0 + w, y1, c);
            gui.fill(x1 - w, y0, x1, y1, c);
            return;
        }
        int flatTop = Math.round(y0 + rad);
        int flatBottom = Math.round(y1 - rad);
        // straight vertical edges
        if (flatBottom > flatTop) {
            gui.fill(x0, flatTop, x0 + w, flatBottom, c);
            gui.fill(x1 - w, flatTop, x1, flatBottom, c);
        }
        for (int y = y0; y < Math.min(flatTop, y1); y++) {
            ringRow(x0, x1, y, y0, y1, w, rad, innerRad, c);
        }
        for (int y = Math.max(flatBottom, y0); y < y1; y++) {
            ringRow(x0, x1, y, y0, y1, w, rad, innerRad, c);
        }
    }

    private void ringRow(int x0, int x1, int y, int y0, int y1, int w,
                         float rad, float innerRad, int argb) {
        float outerInset = inset(y + 0.5f, y0, y1, rad);
        int left = Math.round(x0 + outerInset);
        int right = Math.round(x1 - outerInset);
        boolean innerRow = innerRad > 0.5f && y + 0.5f > y0 + w && y + 0.5f < y1 - w;
        if (innerRow) {
            float innerInset = inset(y + 0.5f, y0 + w, y1 - w, innerRad);
            int innerLeft = Math.round(x0 + w + innerInset);
            int innerRight = Math.round(x1 - w - innerInset);
            if (innerLeft > left) {
                gui.fill(left, y, innerLeft, y + 1, argb);
            }
            if (innerRight < right) {
                gui.fill(innerRight, y, right, y + 1, argb);
            }
        } else if (right > left) {
            gui.fill(left, y, right, y + 1, argb);
        }
    }

    private void ovalRows(float cx, float cy, float rx, float ry, int argb, int step) {
        if (rx <= 0f || ry <= 0f) {
            return;
        }
        int y0 = Math.round(cy - ry);
        int y1 = Math.round(cy + ry);
        int stride = Math.max(1, step);
        for (int y = y0; y < y1; y += stride) {
            float dy = (y + stride * 0.5f - cy) / ry;
            if (dy <= -1f || dy >= 1f) {
                continue;
            }
            float k = (float) Math.sqrt(Math.max(0f, 1f - dy * dy));
            int half = Math.round(rx * k);
            if (half <= 0) {
                continue;
            }
            gui.fill(Math.round(cx - half), y, Math.round(cx + half), Math.min(y + stride, y1), argb);
        }
    }

    private void ovalRing(float cx, float cy, float rx, float ry, float thickness, int argb) {
        if (rx <= 0f || ry <= 0f) {
            return;
        }
        int y0 = Math.round(cy - ry);
        int y1 = Math.round(cy + ry);
        float irx = Math.max(0f, rx - thickness);
        float iry = Math.max(0f, ry - thickness);
        for (int y = y0; y < y1; y++) {
            float dy = (y + 0.5f - cy) / ry;
            if (dy <= -1f || dy >= 1f) {
                continue;
            }
            int half = Math.round(rx * (float) Math.sqrt(Math.max(0f, 1f - dy * dy)));
            if (half <= 0) {
                continue;
            }
            float idy = (y + 0.5f - cy) / Math.max(0.001f, iry);
            if (irx > 0.5f && idy > -1f && idy < 1f) {
                int inner = Math.round(irx * (float) Math.sqrt(Math.max(0f, 1f - idy * idy)));
                int left = Math.round(cx - half);
                int innerLeft = Math.round(cx - inner);
                int innerRight = Math.round(cx + inner);
                int right = Math.round(cx + half);
                if (innerLeft > left) {
                    gui.fill(left, y, innerLeft, y + 1, argb);
                }
                if (innerRight < right) {
                    gui.fill(innerRight, y, right, y + 1, argb);
                }
            } else {
                gui.fill(Math.round(cx - half), y, Math.round(cx + half), y + 1, argb);
            }
        }
    }

    @Override
    public void circle(float cx, float cy, float radius, int argb) {
        float rx = radius * sx;
        float ry = radius * sy;
        ovalRows(fx(cx), fy(cy), rx, ry, color(argb), Math.max(1, Math.round(Math.max(rx, ry) / 48f)));
    }

    @Override
    public void circleStroke(float cx, float cy, float radius, float lineWidth, int argb) {
        ovalRing(fx(cx), fy(cy), radius * sx, radius * sy,
                Math.max(1f, lineWidth * Math.min(sx, sy)), color(argb));
    }

    @Override
    public void oval(float l, float t, float r, float b, int argb) {
        float cx = fx((l + r) * 0.5f);
        float cy = fy((t + b) * 0.5f);
        float rx = Math.max(0f, (r - l) * 0.5f * sx);
        float ry = Math.max(0f, (b - t) * 0.5f * sy);
        ovalRows(cx, cy, rx, ry, color(argb), Math.max(1, Math.round(Math.max(rx, ry) / 48f)));
    }

    @Override
    public void ovalStroke(float l, float t, float r, float b, float lineWidth, int argb) {
        ovalRing(fx((l + r) * 0.5f), fy((t + b) * 0.5f),
                Math.max(0f, (r - l) * 0.5f * sx), Math.max(0f, (b - t) * 0.5f * sy),
                Math.max(1f, lineWidth * Math.min(sx, sy)), color(argb));
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        float ax = fx(x1);
        float ay = fy(y1);
        float bx = fx(x2);
        float by = fy(y2);
        float dx = bx - ax;
        float dy = by - ay;
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.05f) {
            return;
        }
        int thickness = Math.max(1, Math.round(lineWidth * Math.min(sx, sy)));
        gui.pose().pushMatrix();
        gui.pose().translate(ax, ay);
        gui.pose().rotate((float) Math.atan2(dy, dx));
        gui.fill(0, -Math.round(thickness * 0.5f), Math.round(length), Math.round(thickness * 0.5f),
                color(argb));
        gui.pose().popMatrix();
    }

    @Override
    public void arc(float cx, float cy, float radius, float start, float sweep, boolean pie,
                    float lineWidth, int argb) {
        int segments = Math.max(8, Math.round(Math.abs(sweep) / 11f));
        float previousX = 0f;
        float previousY = 0f;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(start + sweep * (i / (float) segments));
            float x = cx + radius * (float) Math.cos(angle);
            float y = cy + radius * (float) Math.sin(angle);
            if (i > 0) {
                line(previousX, previousY, x, y, lineWidth, argb);
            }
            previousX = x;
            previousY = y;
        }
        if (pie) {
            line(cx, cy, previousX, previousY, lineWidth, argb);
        }
    }

    private static int lerp(int from, int to, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * f);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void linearGradient(float l, float t, float r, float b, float radius,
                               float x0, float y0, float x1, float y1, int from, int to) {
        int dx0 = ix(l);
        int dy0 = iy(t);
        int dx1 = ix(r);
        int dy1 = iy(b);
        if (dx1 <= dx0 || dy1 <= dy0) {
            return;
        }
        float rad = Math.min(radiusPx(radius), Math.min(dx1 - dx0, dy1 - dy0) * 0.5f);
        boolean vertical = Math.abs(y1 - y0) >= Math.abs(x1 - x0);
        int span = vertical ? dy1 - dy0 : dx1 - dx0;
        int steps = Math.max(2, Math.min(GRADIENT_STEPS, span));
        for (int i = 0; i < steps; i++) {
            int a = (vertical ? dy0 : dx0) + Math.round(span * (float) i / steps);
            int bb = (vertical ? dy0 : dx0) + Math.round(span * (float) (i + 1) / steps);
            if (bb <= a) {
                continue;
            }
            int c = color(lerp(from, to, (i + 0.5f) / steps));
            if (vertical) {
                float ins = inset(a + 0.5f, dy0, dy1, rad);
                int left = Math.round(dx0 + ins);
                int right = Math.round(dx1 - ins);
                if (right > left) {
                    gui.fill(left, a, right, bb, c);
                }
            } else {
                float ins = inset(a + 0.5f, dx0, dx1, rad);
                int top = Math.round(dy0 + ins);
                int bottom = Math.round(dy1 - ins);
                if (bottom > top) {
                    gui.fill(a, top, bb, bottom, c);
                }
            }
        }
    }

    @Override
    public void radialGradient(float cx, float cy, float radius, int center, int edge) {
        float rx = radius * sx;
        float ry = radius * sy;
        if (rx < 1f || ry < 1f) {
            return;
        }
        int layers = radius > 240f ? 3 : 4;
        int step = Math.max(1, Math.round(ry / 70f));
        for (int i = layers; i >= 1; i--) {
            float f = i / (float) layers;
            int c = color(lerp(center, edge, 1f - f));
            ovalRows(fx(cx), fy(cy), rx * f, ry * f, c, step);
        }
    }

    @Override
    public void shadow(float l, float t, float r, float b, float radius, float sigma, int argb) {
        if (((argb >>> 24) & 0xFF) == 0) {
            return;
        }
        // No blur filter here: a few expanding translucent rounded rects read as
        // the same soft drop shadow at interface sizes.
        int layers = 4;
        for (int i = layers; i >= 1; i--) {
            float grow = sigma * 0.45f * (i / (float) layers);
            int faint = (((int) (((argb >>> 24) & 0xFF) * 0.32f)) << 24) | (argb & 0xFFFFFF);
            roundRect(l - grow, t - grow + 1.5f, r + grow, b + grow + 1.5f, radius + grow, faint);
        }
    }

    // -------------------------------------------------------------- text

    @Override
    public void text(String value, float x, float baselineY, UiFont font, int argb) {
        if (value == null || value.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font mcFont = mc == null ? null : mc.font;
        if (mcFont == null) {
            return;
        }
        VanillaFont sized = font instanceof VanillaFont vanilla
                ? vanilla
                : new VanillaFont(Fonts.Weight.REGULAR, Math.max(4f, font.height() * 0.85f));
        float scale = sized.scale();
        if (scale <= 0.01f) {
            return;
        }
        int c = color(argb);
        gui.pose().pushMatrix();
        gui.pose().translate(fx(x), fy(baselineY));
        gui.pose().scale(sx * scale, sy * scale);
        gui.drawString(mcFont, value, 0, Math.round(-VANILLA_ASCENT), c, false);
        if (sized.emphasised()) {
            gui.drawString(mcFont, value, 1, Math.round(-VANILLA_ASCENT), c, false);
        }
        gui.pose().popMatrix();
    }

    @Override
    public void close() {
        // Everything cached here is owned by Minecraft's texture manager.
    }
}
