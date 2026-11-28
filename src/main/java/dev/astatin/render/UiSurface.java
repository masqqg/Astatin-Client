package dev.astatin.render;

/**
 * A frame-scoped drawing surface. The only implementation is the Skija one; the
 * interface exists so {@code AstatinOverlay} can hold it without linking against
 * Skija classes on platforms where the natives are missing.
 */
public interface UiSurface {
    boolean begin(int framebufferWidth, int framebufferHeight, float guiScale,
                  int guiWidth, int guiHeight);

    Render2D ui();

    void end();

    void close();

    boolean isDisabled();
}
