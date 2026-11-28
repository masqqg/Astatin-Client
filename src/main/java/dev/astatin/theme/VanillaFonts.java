package dev.astatin.theme;

import dev.astatin.render.UiFont;

/** Provides Minecraft-font backed type, always available as a last resort. */
final class VanillaFonts implements Fonts.Provider {
    private VanillaFonts() {
    }

    static Fonts.Provider provider() {
        return new VanillaFonts();
    }

    @Override
    public UiFont font(Fonts.Weight weight, float size) {
        return new VanillaFont(weight, size);
    }
}
