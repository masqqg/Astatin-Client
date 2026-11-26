package dev.astatin.render;

/**
 * Tiny geometric icon set drawn with strokes so the interface never depends on
 * a font glyph being present. All icons are drawn inside a square of the given
 * size and are meant to be read at 12-16 px.
 */
public final class Icons {
    public enum Glyph {
        HUD, VISUALS, INTERFACE, MISC, SEARCH, CLOSE, CHEVRON_DOWN, CHEVRON_RIGHT,
        CHECK, SLIDERS, EDIT, ARROW_LEFT, PLUS, MINUS, EYE, ZOOM, SPEED, SUN, GAUGE, KEY, SHIELD,
        STAR, CROSSHAIR, CONTRAST
    }

    private Icons() {
    }

    public static void draw(Render2D r, Glyph glyph, float cx, float cy, float size, int color) {
        float h = size * 0.5f;
        float lw = Math.max(1.1f, size * 0.09f);
        switch (glyph) {
            case HUD -> {
                r.roundRectStroke(cx - h, cy - h * 0.8f, cx + h, cy + h * 0.55f, size * 0.18f, lw, color);
                r.line(cx - h * 0.45f, cy + h * 0.8f, cx + h * 0.45f, cy + h * 0.8f, lw, color);
                r.line(cx + h * 0.55f, cy - h * 0.35f, cx + h * 0.55f, cy + h * 0.12f, lw, color);
            }
            case VISUALS -> {
                r.ovalStroke(cx - h, cy - h * 0.55f, cx + h, cy + h * 0.55f, lw, color);
                r.circle(cx, cy, size * 0.14f, color);
            }
            case INTERFACE -> {
                r.roundRectStroke(cx - h, cy - h, cx + h, cy + h, size * 0.16f, lw, color);
                r.line(cx - h, cy - h * 0.25f, cx + h, cy - h * 0.25f, lw, color);
                r.line(cx - h * 0.35f, cy - h, cx - h * 0.35f, cy + h, lw, color);
            }
            case MISC -> {
                for (int i = 0; i < 3; i++) {
                    float y = cy - h * 0.6f + i * h * 0.6f;
                    r.line(cx - h, y, cx + h, y, lw, color);
                    float knob = cx + (i == 1 ? h * 0.35f : (i == 0 ? -h * 0.3f : h * 0.1f));
                    r.circle(knob, y, size * 0.11f, color);
                }
            }
            case SEARCH -> {
                r.circleStroke(cx - size * 0.08f, cy - size * 0.08f, size * 0.3f, lw, color);
                r.line(cx + size * 0.14f, cy + size * 0.14f, cx + h, cy + h, lw, color);
            }
            case CLOSE -> {
                r.line(cx - h * 0.55f, cy - h * 0.55f, cx + h * 0.55f, cy + h * 0.55f, lw, color);
                r.line(cx + h * 0.55f, cy - h * 0.55f, cx - h * 0.55f, cy + h * 0.55f, lw, color);
            }
            case CHEVRON_DOWN -> {
                r.line(cx - h * 0.5f, cy - h * 0.22f, cx, cy + h * 0.3f, lw, color);
                r.line(cx, cy + h * 0.3f, cx + h * 0.5f, cy - h * 0.22f, lw, color);
            }
            case CHEVRON_RIGHT -> {
                r.line(cx - h * 0.22f, cy - h * 0.5f, cx + h * 0.3f, cy, lw, color);
                r.line(cx + h * 0.3f, cy, cx - h * 0.22f, cy + h * 0.5f, lw, color);
            }
            case CHECK -> {
                r.line(cx - h * 0.55f, cy + h * 0.02f, cx - h * 0.12f, cy + h * 0.42f, lw, color);
                r.line(cx - h * 0.12f, cy + h * 0.42f, cx + h * 0.58f, cy - h * 0.42f, lw, color);
            }
            case SLIDERS -> {
                for (int i = 0; i < 3; i++) {
                    float y = cy + (i - 1) * h * 0.62f;
                    r.line(cx - h, y, cx + h, y, lw, color);
                    r.circle(cx - h * 0.25f + i * h * 0.5f, y, size * 0.09f, color);
                }
            }
            case EDIT -> {
                r.line(cx - h * 0.55f, cy + h * 0.55f, cx + h * 0.45f, cy - h * 0.45f, lw, color);
                r.line(cx + h * 0.15f, cy - h * 0.7f, cx + h * 0.65f, cy - h * 0.2f, lw, color);
                r.line(cx - h * 0.65f, cy + h * 0.65f, cx - h * 0.2f, cy + h * 0.55f, lw, color);
            }
            case ARROW_LEFT -> {
                r.line(cx + h * 0.55f, cy, cx - h * 0.5f, cy, lw, color);
                r.line(cx - h * 0.5f, cy, cx - h * 0.02f, cy - h * 0.48f, lw, color);
                r.line(cx - h * 0.5f, cy, cx - h * 0.02f, cy + h * 0.48f, lw, color);
            }
            case PLUS -> {
                r.line(cx - h * 0.5f, cy, cx + h * 0.5f, cy, lw, color);
                r.line(cx, cy - h * 0.5f, cx, cy + h * 0.5f, lw, color);
            }
            case MINUS -> r.line(cx - h * 0.5f, cy, cx + h * 0.5f, cy, lw, color);
            case EYE -> {
                r.ovalStroke(cx - h, cy - h * 0.52f, cx + h, cy + h * 0.52f, lw, color);
                r.circle(cx, cy, size * 0.13f, color);
            }
            case ZOOM -> {
                r.circleStroke(cx - size * 0.06f, cy - size * 0.06f, size * 0.28f, lw, color);
                r.line(cx + size * 0.14f, cy + size * 0.14f, cx + h, cy + h, lw, color);
                r.line(cx - size * 0.2f, cy - size * 0.06f, cx + size * 0.08f, cy - size * 0.06f, lw, color);
                r.line(cx - size * 0.06f, cy - size * 0.2f, cx - size * 0.06f, cy + size * 0.08f, lw, color);
            }
            case SPEED -> {
                r.line(cx - h * 0.65f, cy - h * 0.55f, cx - h * 0.05f, cy, lw, color);
                r.line(cx - h * 0.05f, cy, cx - h * 0.65f, cy + h * 0.55f, lw, color);
                r.line(cx + h * 0.05f, cy - h * 0.55f, cx + h * 0.65f, cy, lw, color);
                r.line(cx + h * 0.65f, cy, cx + h * 0.05f, cy + h * 0.55f, lw, color);
            }
            case SUN -> {
                r.circleStroke(cx, cy, size * 0.2f, lw, color);
                for (int i = 0; i < 4; i++) {
                    double angle = Math.PI * i / 4.0;
                    float dx = (float) Math.cos(angle) * h;
                    float dy = (float) Math.sin(angle) * h;
                    r.line(cx + dx * 0.62f, cy + dy * 0.62f, cx + dx, cy + dy, lw, color);
                    r.line(cx - dx * 0.62f, cy - dy * 0.62f, cx - dx, cy - dy, lw, color);
                }
            }
            case GAUGE -> {
                r.arc(cx, cy + h * 0.18f, size * 0.42f, 180f, 180f, false, lw, color);
                r.line(cx, cy + h * 0.18f, cx + size * 0.22f, cy - size * 0.13f, lw, color);
            }
            case KEY -> {
                r.circleStroke(cx - h * 0.35f, cy - h * 0.3f, size * 0.2f, lw, color);
                r.line(cx - h * 0.15f, cy - h * 0.1f, cx + h * 0.55f, cy + h * 0.6f, lw, color);
                r.line(cx + h * 0.25f, cy + h * 0.3f, cx + h * 0.45f, cy + h * 0.1f, lw, color);
            }
            case SHIELD -> {
                r.roundRectStroke(cx - h * 0.62f, cy - h * 0.75f, cx + h * 0.62f, cy + h * 0.15f,
                        size * 0.3f, lw, color);
                r.line(cx - h * 0.62f, cy + h * 0.1f, cx, cy + h * 0.75f, lw, color);
                r.line(cx + h * 0.62f, cy + h * 0.1f, cx, cy + h * 0.75f, lw, color);
            }
            case STAR -> {
                r.line(cx, cy - h * 0.85f, cx, cy + h * 0.85f, lw, color);
                r.line(cx - h * 0.85f, cy, cx + h * 0.85f, cy, lw, color);
                r.line(cx - h * 0.38f, cy - h * 0.38f, cx + h * 0.38f, cy + h * 0.38f, lw * 0.7f, color);
                r.line(cx + h * 0.38f, cy - h * 0.38f, cx - h * 0.38f, cy + h * 0.38f, lw * 0.7f, color);
            }
            case CROSSHAIR -> {
                r.circleStroke(cx, cy, size * 0.27f, lw, color);
                r.line(cx, cy - h, cx, cy - h * 0.5f, lw, color);
                r.line(cx, cy + h * 0.5f, cx, cy + h, lw, color);
                r.line(cx - h, cy, cx - h * 0.5f, cy, lw, color);
                r.line(cx + h * 0.5f, cy, cx + h, cy, lw, color);
            }
            case CONTRAST -> {
                r.save();
                r.clipRect(cx, cy - h, cx + h, cy + h);
                r.circle(cx, cy, size * 0.36f, color);
                r.restore();
                r.circleStroke(cx, cy, size * 0.36f, lw, color);
            }
        }
    }
}
