package dev.astatin.hud;

import dev.astatin.render.Render2D;
import dev.astatin.render.UiFont;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

/** Active effects with colour, amplifier and remaining time. */
public final class PotionsElement extends HudElement {
    public PotionsElement() {
        super("potions", "Potions");
    }

    @Override
    public float baseWidth(HudData data) {
        return 188f;
    }

    @Override
    public float baseHeight(HudData data) {
        int rows = Math.min(4, Math.max(1, data.potions.size()));
        return 30f + rows * 24f + 10f;
    }

    @Override
    public void draw(Render2D r, HudData data) {
        float w = baseWidth(data);
        float h = baseHeight(data);
        r.glass(0f, 0f, w, h, 12f, 0x5C000000, 10f);

        r.circle(11.5f, 14.5f, 2f, Theme.AMBER);
        r.textTracked("EFFECTS", 17f, 17f, Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.9f);
        r.textRight(String.valueOf(data.potions.size()), w - 11f, 18.5f, Fonts.semibold(10f), Theme.TEXT_SOFT);

        if (data.potions.isEmpty()) {
            r.textCenterY("No active effects", 16f, 47f, Fonts.regular(9f), Theme.TEXT_MUTED);
            return;
        }

        float y = 27f;
        int limit = Math.min(4, data.potions.size());
        for (int i = 0; i < limit; i++) {
            HudData.PotionInfo effect = data.potions.get(i);
            float rowBottom = y + 21f;
            r.roundRect(10f, y + 1f, 12.5f, y + 19f, 1.2f, Theme.withAlpha(effect.color, 0.9f));
            r.textTop(trim(r, effect.name, Fonts.regular(9.5f), w - 92f), 18f, y + 0.5f, Fonts.regular(9.5f), Theme.TEXT);
            if (effect.level != null && !effect.level.isEmpty() && !effect.level.equals("I")) {
                float chipW = r.width(effect.level, Fonts.medium(6.5f)) + 7f;
                r.roundRect(18f + r.width(effect.name, Fonts.regular(9.5f)) + 5f, y + 2.5f,
                        18f + r.width(effect.name, Fonts.regular(9.5f)) + 5f + chipW, y + 12f, 3f,
                        Theme.withAlpha(effect.color, 0.18f));
                r.textTop(effect.level, 21.5f + r.width(effect.name, Fonts.regular(9.5f)) + 5f, y + 4f,
                        Fonts.medium(6.5f), Theme.withAlpha(effect.color, 0.95f));
            }
            if (data.showDuration) {
                r.textTop(effect.duration(), w - 11f - r.width(effect.duration(), Fonts.medium(8f)), y + 1f,
                        Fonts.medium(8f), Theme.TEXT_DIM);
            }
            r.bar(18f, rowBottom, w - 11f, rowBottom + 2.2f, 1.1f, effect.fraction,
                    0x20FFFFFF, Theme.withAlpha(effect.color, 0.8f));
            y += 24f;
        }
    }

    private String trim(Render2D r, String value, UiFont font, float maxWidth) {
        if (r.width(value, font) <= maxWidth) {
            return value;
        }
        String result = value;
        while (result.length() > 1 && r.width(result + "…", font) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }
}
