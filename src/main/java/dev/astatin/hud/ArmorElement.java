package dev.astatin.hud;

import dev.astatin.render.Render2D;
import dev.astatin.render.UiFont;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

/** Armour pieces with name, durability bar and percentage. */
public final class ArmorElement extends HudElement {
    private static final String[] SLOT_NAMES = {"Helmet", "Chestplate", "Leggings", "Boots"};
    private static final String[] SLOT_LETTERS = {"H", "C", "L", "B"};

    public ArmorElement() {
        super("armor", "Armor");
    }

    @Override
    public float baseWidth(HudData data) {
        return 188f;
    }

    @Override
    public float baseHeight(HudData data) {
        return 30f + 4 * 21f + 6f;
    }

    @Override
    public void draw(Render2D r, HudData data) {
        float w = baseWidth(data);
        float h = baseHeight(data);
        r.glass(0f, 0f, w, h, 12f, 0x5C000000, 10f);

        r.circle(11.5f, 14.5f, 2f, Theme.AMBER);
        r.textTracked("ARMOR", 17f, 17f, Fonts.medium(6.5f), Theme.TEXT_MUTED, 0.9f);
        r.textRight(String.valueOf(data.armorPoints), w - 11f, 18.5f, Fonts.semibold(10f), Theme.AMBER);

        float y = 27f;
        for (int i = 0; i < 4; i++) {
            HudData.ArmorPiece piece = i < data.armor.size() ? data.armor.get(i) : null;
            row(r, w, y, i, piece, data.showDurability);
            y += 21f;
        }
    }

    private void row(Render2D r, float w, float y, int slot, HudData.ArmorPiece piece, boolean durability) {
        boolean present = piece != null && piece.present;
        int iconColor = present ? Theme.TEXT_SOFT : Theme.TEXT_MUTED;
        r.roundRect(9f, y, 25f, y + 17f, 5f, present ? 0x12FFFFFF : 0x08FFFFFF);
        r.textCenteredTop(SLOT_LETTERS[slot], 17f, y + 4f, Fonts.semibold(7.5f), iconColor);

        String name = present ? piece.name : "Empty";
        int nameColor = present ? Theme.TEXT_SOFT : Theme.TEXT_MUTED;
        r.textTop(trim(r, name, Fonts.regular(9f), w - 100f), 30f, y + 1.5f, Fonts.regular(9f), nameColor);

        if (present && durability) {
            float fraction = Math.max(0f, Math.min(1f, piece.durability));
            r.bar(30f, y + 12.5f, w - 44f, y + 15f, 1.3f, fraction,
                    0x1AFFFFFF, fraction > 0.5f ? Theme.GREEN : (fraction > 0.2f ? Theme.AMBER : Theme.RED));
            r.textTop(Math.round(fraction * 100f) + "%", w - 11f - r.width(Math.round(fraction * 100f) + "%", Fonts.medium(7.5f)),
                    y + 8f, Fonts.medium(7.5f), Theme.TEXT_DIM);
        } else if (present) {
            r.textTop(SLOT_NAMES[slot], 30f, y + 10f, Fonts.regular(7f), Theme.TEXT_MUTED);
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
