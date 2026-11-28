package dev.astatin.render;

/**
 * A sized typeface as the UI code sees it. Two implementations exist: one that
 * wraps a Skia font and one that wraps Minecraft's own font, so the menu and the
 * HUD can be drawn even when the Skija native library cannot be loaded.
 *
 * Ascent is negative and measured upwards from the baseline, matching Skia.
 */
public interface UiFont {
    float measureWidth(String text);

    float ascent();

    float descent();

    default float height() {
        return descent() - ascent();
    }
}
