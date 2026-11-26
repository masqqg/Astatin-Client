package dev.astatin.hud;

import dev.astatin.render.Render2D;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

/** Position readout with axis labels and a dimension chip. */
public final class CoordsElement extends HudElement {
    public CoordsElement() {
        super("coords", "Coordinates");
    }

    @Override
    public float baseWidth(HudData data) {
        return 182f;
    }

    @Override
    public float baseHeight(HudData data) {
        return 69f;
    }

    @Override
    public void draw(Render2D r, HudData data) {
        float w = baseWidth(data);
        float h = baseHeight(data);
        r.glass(0f, 0f, w, h, 12f, 0x5C000000, 10f);

        r.circle(11.5f, 14.5f, 2f, Theme.AMBER);
        r.textTracked("POSITION", 17f, 17f, Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.9f);
        r.textTracked(data.facing, w - 12f - r.trackedWidth(data.facing, Fonts.medium(6.5f), 0.9f), 17f,
                Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.9f);

        float columns = (w - 20f) / 3f;
        axis(r, "X", data.posX, 11f, 30f, columns);
        axis(r, "Y", data.posY, 11f + columns, 30f, columns);
        axis(r, "Z", data.posZ, 11f + columns * 2f, 30f, columns);

        if (data.showDimension) {
            String label = data.dimension.toUpperCase();
            float labelW = r.trackedWidth(label, Fonts.medium(6f), 0.8f);
            float chipW = labelW + 14f;
            r.roundRect(10f, h - 19f, 10f + chipW, h - 7f, 4.5f, Theme.accentSoft(0.12f));
            r.roundRectStroke(10.5f, h - 18.5f, 9.5f + chipW, h - 7.5f, 4.5f, 1f, Theme.accentSoft(0.22f));
            r.textTracked(label, 16f, h - 10.5f, Fonts.medium(6f), Theme.AMBER, 0.8f);
        }
    }

    private void axis(Render2D r, String label, String value, float x, float valueTop, float columnWidth) {
        r.textTop(label, x, valueTop + 1f, Fonts.medium(7.5f), Theme.accentSoft(0.75f));
        float labelW = r.width(label, Fonts.medium(7.5f));
        r.textTop(value, x + labelW + 4f, valueTop, Fonts.semibold(11.5f), Theme.TEXT_SOFT);
    }
}
