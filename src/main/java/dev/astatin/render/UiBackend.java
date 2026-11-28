package dev.astatin.render;

/**
 * The primitive drawing operations the client needs. {@link Render2D} builds the
 * higher level language (glass panels, bars, tracked labels) on top of this, so
 * the exact same layout code runs on Skija and on Minecraft's own renderer.
 *
 * Coordinates are in GUI units: the caller begins a frame with the scaled GUI
 * size and every backend maps that to its own surface.
 */
public interface UiBackend {
    // ------------------------------------------------------------ images

    float imageWidth(String path);

    float imageHeight(String path);

    /** Draws the normalised source region (u0,v0)-(u1,v1) into the given rect. */
    void image(String path, float l, float t, float r, float b,
               float u0, float v0, float u1, float v1);

    // ------------------------------------------------------------ state

    void save();

    void restore();

    void translate(float x, float y);

    void scale(float sx, float sy);

    void clipRect(float l, float t, float r, float b);

    void clipRoundRect(float l, float t, float r, float b, float radius);

    // ------------------------------------------------------- primitives

    void rect(float l, float t, float r, float b, int color);

    void roundRect(float l, float t, float r, float b, float radius, int color);

    void roundRectStroke(float l, float t, float r, float b, float radius, float lineWidth, int color);

    void line(float x1, float y1, float x2, float y2, float lineWidth, int color);

    void circle(float cx, float cy, float radius, int color);

    void circleStroke(float cx, float cy, float radius, float lineWidth, int color);

    void oval(float l, float t, float r, float b, int color);

    void ovalStroke(float l, float t, float r, float b, float lineWidth, int color);

    void arc(float cx, float cy, float radius, float start, float sweep, boolean pie,
             float lineWidth, int color);

    /**
     * Fills a rounded rect with a linear gradient that runs from (x0,y0) to
     * (x1,y1). The two axes the client uses are vertical and horizontal.
     */
    void linearGradient(float l, float t, float r, float b, float radius,
                        float x0, float y0, float x1, float y1, int from, int to);

    void radialGradient(float cx, float cy, float radius, int center, int edge);

    void shadow(float l, float t, float r, float b, float radius, float sigma, int color);

    // ------------------------------------------------------------ text

    void text(String value, float x, float baselineY, UiFont font, int color);

    // --------------------------------------------------------- grouping

    /**
     * Multiplies the alpha of everything drawn until the matching pop. The rect
     * bounds the group on backends that need one for their offscreen layer.
     */
    void pushAlpha(float alpha, float l, float t, float r, float b);

    void popAlpha();

    void close();
}
