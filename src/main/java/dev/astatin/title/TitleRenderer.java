package dev.astatin.title;

import dev.astatin.animation.Anim;
import dev.astatin.menu.Frame;
import dev.astatin.render.Icons;
import dev.astatin.render.Render2D;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

import java.util.HashMap;
import java.util.Map;

/** The custom title screen: branded, quiet and fully drawn with Skija. */
public final class TitleRenderer {
    public static final String[] BUTTONS = {"Singleplayer", "Multiplayer", "Settings", "Quit"};
    private static final Icons.Glyph[] ICONS = {
            Icons.Glyph.HUD, Icons.Glyph.SPEED, Icons.Glyph.SLIDERS, Icons.Glyph.CLOSE
    };

    private final Map<String, Anim> hovers = new HashMap<>();
    private final Anim open = new Anim(0f, 300f, 32f);

    private static boolean compact(float width, float height) {
        return width < 600f || height < 380f;
    }

    public Frame button(int index, float width, float height) {
        if (compact(width, height)) {
            float side = Math.min(12f, width * 0.05f);
            float bottom = Math.min(12f, height * 0.05f);
            float header = Math.min(48f, height * 0.2f);
            float gap = Math.min(8f, height * 0.025f);
            float buttonH = Math.max(0f, Math.min(42f, (height - header - bottom - 3f * gap) / 4f));
            float buttonW = Math.max(0f, Math.min(300f, width - 2f * side));
            float y = header + (height - header - bottom - 4f * buttonH - 3f * gap) * 0.5f;
            return new Frame((width - buttonW) * 0.5f, y + index * (buttonH + gap), buttonW, buttonH);
        }
        float buttonW = Math.min(300f, width * 0.32f);
        float buttonH = 46f;
        float x = width - 64f - buttonW;
        float y = height * 0.30f + index * (buttonH + 8f);
        return new Frame(x, y, buttonW, buttonH);
    }

    public void update(TitleState state, float width, float height) {
        open.target(1f);
        for (int i = 0; i < BUTTONS.length; i++) {
            boolean hover = button(i, width, height).contains(state.mouseX, state.mouseY);
            anim("btn." + i).target(hover ? 1f : 0f);
        }
        float dt = Math.max(0f, Math.min(0.05f, state.dt));
        open.update(dt);
        for (Anim anim : hovers.values()) {
            anim.update(dt);
        }
    }

    public void snap(TitleState state, float width, float height) {
        open.snap(1f);
        for (int i = 0; i < BUTTONS.length; i++) {
            anim("btn." + i).snap(button(i, width, height).contains(state.mouseX, state.mouseY) ? 1f : 0f);
        }
    }

    private Anim anim(String key) {
        return hovers.computeIfAbsent(key, ignored -> new Anim(0f, 320f, 30f));
    }

    public void render(Render2D r, TitleState state) {
        float w = r.width();
        float h = r.height();
        float progress = open.get();
        if (progress <= 0.002f) {
            return;
        }

        // background with a very slow drift
        float zoom = 1.035f + 0.012f * (float) Math.sin(state.time * 0.08f);
        float panX = (float) Math.sin(state.time * 0.05f) * 6f;
        float panY = (float) Math.cos(state.time * 0.04f) * 4f;
        r.save();
        r.translate(w * 0.5f, h * 0.5f);
        r.scale(zoom, zoom);
        r.translate(-w * 0.5f + panX, -h * 0.5f + panY);
        r.imageCover("/assets/astatin/textures/gui/background.jpg", -10f, -10f, w + 10f, h + 10f);
        r.restore();

        // legibility gradients
        r.gradientRectHorizontal(0f, 0f, w, h, 0f, 0xE607080A, 0x3307080A);
        r.gradientRect(0f, h * 0.55f, w, h, 0f, 0x0007080A, 0xCC07080A);
        r.radialGlow(w * 0.78f, h * 0.18f, h * 0.85f, Theme.accentSoft(0.10f), Theme.accentSoft(0f));
        r.radialGlow(w * 0.5f, h * 0.55f, Math.max(w, h) * 0.7f, 0x00000000, 0x8C000000);

        for (int i = 0; i < 14; i++) {
            float y = h - ((state.time * (6f + i % 4 * 2f) + i * 73f) % (h + 60f)) + 30f;
            float x = (float) ((Math.sin(i * 41.7f) * 0.5f + 0.5f) * w)
                    + (float) Math.sin(state.time * 0.4f + i) * 8f;
            r.circle(x, y, i % 4 == 0 ? 1.5f : 0.9f,
                    Theme.accentSoft(0.10f + 0.15f * (float) Math.abs(Math.sin(state.time + i))));
        }

        if (compact(w, h)) {
            Frame first = button(0, w, h);
            if (first.y() >= 28f && w >= 100f) {
                r.textCenteredTop("ASTATIN", w * 0.5f, first.y() - 28f,
                        Fonts.bold(16f), Theme.TEXT);
            }
            for (int i = 0; i < BUTTONS.length; i++) {
                drawCompactButton(r, i);
            }
            return;
        }

        // brand block
        r.imageFit("/assets/astatin/textures/gui/logo.png", 60f, h * 0.22f, 210f, h * 0.22f + 148f);
        r.textTracked("ASTATIN", 62f, h * 0.22f + 182f, Fonts.bold(38f), Theme.TEXT, 2.2f);
        r.textTracked("A CLEANER WAY TO PLAY", 64f, h * 0.22f + 205f, Fonts.medium(8.2f),
                Theme.TEXT_DIM, 1.6f);
        r.gradientRectHorizontal(64f, h * 0.22f + 220f, 122f, h * 0.22f + 222f, 1f,
                Theme.accentLight(0.15f), 0x00FFA23A);

        // buttons
        for (int i = 0; i < BUTTONS.length; i++) {
            drawButton(r, state, i);
        }

        // footer
        r.line(64f, h - 36f, w - 64f, h - 36f, 1f, 0x14FFFFFF);
        r.textTracked("ASTATIN CLIENT", 64f, h - 22f, Fonts.medium(6.4f), Theme.TEXT_MUTED, 1.1f);
        r.textTracked("v1.0.0", 64f + r.trackedWidth("ASTATIN CLIENT", Fonts.medium(6.4f), 1.1f) + 10f,
                h - 22f, Fonts.medium(6.4f), Theme.AMBER, 1.1f);
        String right = "MINECRAFT 1.21.11  ·  FABRIC";
        r.textTracked(right, w - 64f - r.trackedWidth(right, Fonts.medium(6.4f), 1.1f),
                h - 22f, Fonts.medium(6.4f), Theme.TEXT_MUTED, 1.1f);
    }

    private void drawCompactButton(Render2D r, int index) {
        Frame frame = button(index, r.width(), r.height());
        float hover = anim("btn." + index).get();
        float radius = Math.min(10f, Math.min(frame.w(), frame.h()) * 0.25f);
        r.roundRect(frame.x(), frame.y(), frame.right(), frame.bottom(), radius,
                Theme.mix(0x18FFFFFF, Theme.accentSoft(0.16f), hover));
        r.roundRectStroke(frame.x() + 0.5f, frame.y() + 0.5f, frame.right() - 0.5f,
                frame.bottom() - 0.5f, radius, 1f,
                Theme.mix(0x20FFFFFF, Theme.accentSoft(0.45f), hover));
        float fontSize = Math.min(11f, Math.max(3f, frame.h() * 0.4f));
        var font = Fonts.medium(fontSize);
        float labelWidth = r.width(BUTTONS[index], font);
        if (labelWidth <= frame.w() - 8f) {
            r.textCenterY(BUTTONS[index], frame.centerX() - labelWidth * 0.5f,
                    frame.centerY(), font, Theme.TEXT);
        }
    }

    private void drawButton(Render2D r, TitleState state, int index) {
        Frame frame = button(index, r.width(), r.height());
        float hover = anim("btn." + index).get();
        float x = frame.x() + 2f * hover;
        float y = frame.y();
        float right = frame.right() + 2f * hover;
        r.shadow(x + 1f, y + 3f, right - 1f, frame.bottom() + 4f, 10f, 14f, 0x66000000);
        r.roundRect(x, y, right, frame.bottom(), 10f, 0x18FFFFFF);
        r.roundRectStroke(x + 0.5f, y + 0.5f, right - 0.5f, frame.bottom() - 0.5f, 10f, 1f,
                Theme.mix(0x20FFFFFF, Theme.accentSoft(0.45f), hover));
        if (hover > 0.01f) {
            r.roundRect(x, y, right, frame.bottom(), 10f, Theme.accentSoft(0.08f * hover));
            r.roundRect(x, y + 13f, x + 2f, frame.bottom() - 13f, 1f, Theme.accentSoft(0.85f * hover));
        }
        Icons.draw(r, ICONS[index], x + 22f, frame.centerY(), 14f,
                Theme.mix(Theme.TEXT_SOFT, Theme.AMBER, hover));
        r.textCenterY(BUTTONS[index], x + 40f, frame.centerY() - 1f, Fonts.medium(10.5f),
                Theme.mix(Theme.TEXT, 0xFFFFFFFF, hover));
        Icons.draw(r, Icons.Glyph.CHEVRON_RIGHT, right - 20f, frame.centerY(), 9f,
                Theme.mix(Theme.TEXT_MUTED, Theme.AMBER, hover));
    }
}
