package dev.astatin.hud;

import dev.astatin.render.Render2D;

/**
 * A single HUD widget. Positions are stored normalized (0..1) so the layout
 * survives resolution and GUI-scale changes. The widget itself draws at its
 * own local origin; the manager applies translation and scale.
 */
public abstract class HudElement {
    public final String id;
    public final String name;
    public boolean enabled = true;
    public float normX;
    public float normY;
    public float scale = 1f;

    protected HudElement(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract float baseWidth(HudData data);

    public abstract float baseHeight(HudData data);

    public abstract void draw(Render2D r, HudData data);

    public final float width(HudData data) {
        return baseWidth(data) * scale;
    }

    public final float height(HudData data) {
        return baseHeight(data) * scale;
    }

    public final void drawAt(Render2D r, HudData data, float x, float y) {
        r.save();
        r.translate(x, y);
        r.scale(scale, scale);
        r.save();
        draw(r, data);
        r.restore();
        r.restore();
    }

    public final float xOnScreen(float screenWidth, HudData data) {
        return normX * Math.max(0f, screenWidth - width(data));
    }

    public final float yOnScreen(float screenHeight, HudData data) {
        return normY * Math.max(0f, screenHeight - height(data));
    }
}
