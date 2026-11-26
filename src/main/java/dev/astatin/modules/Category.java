package dev.astatin.modules;

import dev.astatin.render.Icons;

public enum Category {
    HUD("HUD", "On-screen information", Icons.Glyph.HUD),
    VISUALS("Visuals", "Rendering and comfort", Icons.Glyph.VISUALS),
    INTERFACE("Interface", "Menus and layout", Icons.Glyph.INTERFACE),
    MISC("Misc", "Quality of life", Icons.Glyph.MISC);

    public final String label;
    public final String description;
    public final Icons.Glyph icon;

    Category(String label, String description, Icons.Glyph icon) {
        this.label = label;
        this.description = description;
        this.icon = icon;
    }
}
