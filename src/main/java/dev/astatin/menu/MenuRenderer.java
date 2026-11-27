package dev.astatin.menu;

import dev.astatin.animation.Anim;
import dev.astatin.modules.Category;
import dev.astatin.modules.Module;
import dev.astatin.modules.Modules;
import dev.astatin.modules.Setting;
import dev.astatin.render.Icons;
import dev.astatin.render.Render2D;
import dev.astatin.theme.ColorUtil;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The client menu. Layout is computed by {@link MenuLayout}; this class owns the
 * animation state and the actual drawing. It is completely independent of
 * Minecraft so the offline preview renders the exact same pixels.
 */
public final class MenuRenderer {
    private final Modules modules;
    private final Map<String, Anim> anims = new HashMap<>();
    private final Anim open = new Anim(0f, 320f, 34f);
    private final Anim inspector = new Anim(0f, 320f, 34f);
    private final Anim searchFocus = new Anim(0f, 320f, 30f);
    private final Anim indicator = new Anim(0f, 300f, 34f);
    private final Paint layerPaint = new Paint();
    private final Paint panelPaint = new Paint();

    public MenuRenderer(Modules modules) {
        this.modules = modules;
    }

    // ------------------------------------------------------------- animation

    private Anim anim(String key) {
        return anims.computeIfAbsent(key, ignored -> new Anim(0f, 300f, 28f));
    }

    private boolean animations() {
        return modules.animations();
    }

    public void update(MenuState state, float width, float height) {
        MenuLayout layout = new MenuLayout(width, height);
        List<Module> visible = visible(state);
        setTargets(state, layout, visible);
        float dt = Math.max(0f, Math.min(0.05f, state.dt));
        boolean animate = animations();
        open.target(1f);
        inspector.target(state.selected != null ? 1f : 0f);
        searchFocus.target(state.searchFocused ? 1f : 0f);
        indicator.target(state.category.ordinal());
        for (Anim anim : anims.values()) {
            if (animate) {
                anim.update(dt);
            } else {
                anim.snap(anim.target());
            }
        }
        if (animate) {
            open.update(dt);
            inspector.update(dt);
            searchFocus.update(dt);
            indicator.update(dt);
        } else {
            open.snap(1f);
            inspector.snap(state.selected != null ? 1f : 0f);
            searchFocus.snap(state.searchFocused ? 1f : 0f);
            indicator.snap(state.category.ordinal());
        }
    }

    /** Jumps every animation to its target. Used by the preview captures. */
    public void snap(MenuState state, float width, float height) {
        MenuLayout layout = new MenuLayout(width, height);
        setTargets(state, layout, visible(state));
        open.snap(1f);
        inspector.snap(state.selected != null ? 1f : 0f);
        searchFocus.snap(state.searchFocused ? 1f : 0f);
        indicator.snap(state.category.ordinal());
        for (Anim anim : anims.values()) {
            anim.snap(anim.target());
        }
    }

    private void setTargets(MenuState state, MenuLayout layout, List<Module> visible) {
        for (Category category : Category.values()) {
            Frame frame = layout.category(category.ordinal());
            anim("cat." + category.ordinal()).target(frame.contains(state.mouseX, state.mouseY) ? 1f : 0f);
            anim("cat.active." + category.ordinal()).target(
                    state.category == category && state.search.isBlank() ? 1f : 0f);
        }
        for (int i = 0; i < visible.size(); i++) {
            Module module = visible.get(i);
            Frame card = layout.card(i, visible.size(), state.scroll);
            boolean hover = card.contains(state.mouseX, state.mouseY) && card.bottom() <= layout.grid.bottom() + 1f;
            anim("card." + module.id).target(hover ? 1f : 0f);
            anim("toggle." + module.id).target(module.enabled ? 1f : 0f);
            for (Setting setting : module.settings) {
                anim("set." + setting.key()).target(setting.boolValue ? 1f : 0f);
            }
        }
        anim("close").target(layout.close.contains(state.mouseX, state.mouseY) ? 1f : 0f);
        anim("search").target(layout.search.contains(state.mouseX, state.mouseY) ? 1f : 0f);
        anim("edit").target(layout.edit.contains(state.mouseX, state.mouseY) ? 1f : 0f);
        if (state.selected != null) {
            anim("ins.close").target(layout.inspectorClose().contains(state.mouseX, state.mouseY) ? 1f : 0f);
        }
    }

    public List<Module> visible(MenuState state) {
        return state.search.isBlank()
                ? modules.byCategory(state.category)
                : modules.search(state.search);
    }

    // ---------------------------------------------------------------- render

    public void render(Render2D r, MenuState state) {
        float w = r.width();
        float h = r.height();
        MenuLayout layout = new MenuLayout(w, h);
        List<Module> visible = visible(state);

        drawBackdrop(r, state, w, h);

        float progress = open.get();
        if (progress <= 0.002f) {
            return;
        }
        if (layout.mobile) {
            if (progress < .999f) {
                layerPaint.setAlphaf(Math.max(0f, Math.min(1f, progress)));
                r.canvas().saveLayer(Rect.makeLTRB(0f, 0f, w, h), layerPaint);
            }
            drawMobile(r, state, layout, visible);
            if (progress < .999f) r.canvas().restore();
            return;
        }
        layerPaint.setAlphaf(Math.min(1f, progress));
        float scale = 0.975f + 0.025f * progress;
        r.canvas().saveLayer(Rect.makeLTRB(0f, 0f, w, h), layerPaint);
        r.save();
        r.translate(w * 0.5f, h * 0.5f);
        r.scale(scale, scale);
        r.translate(-w * 0.5f, -h * 0.5f);

        drawSidebar(r, state, layout);
        drawHeader(r, state, layout);
        drawGrid(r, state, layout, visible);
        if (inspector.get() > 0.01f && state.selected != null) {
            drawInspector(r, state, layout, state.selected);
        }

        r.restore();
        r.canvas().restore();
    }

    private void drawMobile(Render2D r, MenuState state, MenuLayout layout, List<Module> visible) {
        Frame main = layout.main;
        r.roundRect(main.x(), main.y(), main.right(), main.bottom(), 10f, 0xF20B0C0F);
        r.textCenterY("ASTATIN", main.x() + 10f, main.y() + 18f, Fonts.bold(11f), Theme.TEXT);
        if (state.selected == null) {
            Frame search = layout.search;
            r.roundRect(search.x(), search.y(), search.right(), search.bottom(), 7f, 0x20FFFFFF);
            String query = state.search.isBlank() ? "Search" : state.search;
            r.textCenterY(trim(r, query, Fonts.regular(9f), search.w() - 12f), search.x() + 6f,
                    search.centerY(), Fonts.regular(9f), state.search.isBlank() ? Theme.TEXT_MUTED : Theme.TEXT);
        }
        Icons.draw(r, Icons.Glyph.CLOSE, layout.close.centerX(), layout.close.centerY(), 12f, Theme.TEXT);

        if (state.selected == null) {
            for (Category category : Category.values()) {
                Frame frame = layout.category(category.ordinal());
                float active = anim("cat.active." + category.ordinal()).get();
                r.roundRect(frame.x(), frame.y(), frame.right(), frame.bottom(), 7f,
                        Theme.mix(0x10FFFFFF, Theme.accentSoft(0.22f), active));
                var font = Fonts.medium(9f);
                String label = trim(r, category.label, font, frame.w() - 8f);
                r.textCenterY(label, frame.centerX() - r.width(label, font) * 0.5f, frame.centerY(), font,
                        Theme.mix(Theme.TEXT_DIM, Theme.TEXT, active));
            }
            r.save();
            r.clipRect(layout.grid.x(), layout.grid.y(), layout.grid.right(), layout.grid.bottom());
            for (int i = 0; i < visible.size(); i++) {
                Frame card = layout.card(i, visible.size(), state.scroll);
                if (card.bottom() >= layout.grid.y() && card.y() <= layout.grid.bottom()) {
                    drawMobileCard(r, layout, visible.get(i), card);
                }
            }
            r.restore();
        } else {
            drawMobileInspector(r, state, layout, state.selected);
        }
        drawMobileScroll(r, layout.scrollUp, "↑");
        drawMobileScroll(r, layout.scrollDown, "↓");
    }

    private void drawMobileScroll(Render2D r, Frame frame, String label) {
        r.roundRect(frame.x(), frame.y(), frame.right(), frame.bottom(), 7f, 0x20FFFFFF);
        var font = Fonts.semibold(14f);
        r.textCenterY(label, frame.centerX() - r.width(label, font) * 0.5f,
                frame.centerY(), font, Theme.TEXT);
    }

    private void drawMobileCard(Render2D r, MenuLayout layout, Module module, Frame card) {
        float hover = anim("card." + module.id).get();
        r.roundRect(card.x(), card.y(), card.right(), card.bottom(), 8f,
                Theme.mix(0x18FFFFFF, Theme.accentSoft(.16f), hover));
        r.roundRectStroke(card.x() + .5f, card.y() + .5f, card.right() - .5f, card.bottom() - .5f,
                8f, 1f, Theme.mix(0x10FFFFFF, Theme.accentSoft(.4f), hover));
        Icons.draw(r, iconFor(module), card.x() + 21f, card.centerY(), 14f,
                module.enabled ? Theme.AMBER : Theme.TEXT_DIM);
        var font = Fonts.semibold(10f);
        String name = trim(r, module.name, font, card.w() - 100f);
        r.textCenterY(name, card.x() + 38f, card.centerY(), font, Theme.TEXT);
        if (module.action) {
            Icons.draw(r, Icons.Glyph.CHEVRON_RIGHT, card.right() - 24f, card.centerY(), 10f, Theme.AMBER);
        } else {
            drawToggle(r, layout.cardToggle(card), module, module.enabled);
        }
    }

    private void drawMobileInspector(Render2D r, MenuState state, MenuLayout layout, Module module) {
        Frame panel = layout.inspector;
        r.roundRect(panel.x(), panel.y(), panel.right(), panel.bottom(), 8f, 0xFF17181B);
        r.textCenterY(trim(r, module.name, Fonts.semibold(11f), panel.w() - 52f),
                panel.x() + 10f, panel.y() + 18f, Fonts.semibold(11f), Theme.TEXT);
        Icons.draw(r, Icons.Glyph.CLOSE, layout.inspectorClose().centerX(), layout.inspectorClose().centerY(),
                11f, Theme.TEXT);
        r.textCenterY("Enabled", panel.x() + 10f, panel.y() + 41f, Fonts.medium(9f), Theme.TEXT_SOFT);
        drawToggle(r, layout.inspectorToggle(), module, module.enabled);
        Frame viewport = layout.settingsViewport();
        r.save();
        r.clipRect(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        if (module.settings.isEmpty()) {
            r.textTop("No settings for this module.", panel.x() + 16f, viewport.y(),
                    Fonts.regular(8.5f), Theme.TEXT_MUTED);
        } else {
            for (int i = 0; i < module.settings.size(); i++) {
                Setting setting = module.settings.get(i);
                Frame row = layout.settingRow(module, i, state.openPicker, state.settingsScroll);
                if (row.bottom() > viewport.y() && row.y() < viewport.bottom()) {
                    drawSetting(r, state, layout, setting, row);
                }
            }
        }
        r.restore();
    }

    private void drawBackdrop(Render2D r, MenuState state, float w, float h) {
        r.rect(0f, 0f, w, h, 0xB407080A);
        r.radialGlow(w * 0.16f, -h * 0.1f, h * 0.9f, Theme.accentSoft(0.075f), Theme.accentSoft(0f));
        r.radialGlow(w * 0.9f, h * 0.95f, h * 0.85f, 0x0F6E8BFF, 0x006E8BFF);
        r.radialGlow(w * 0.5f, h * 0.5f, Math.max(w, h) * 0.72f, 0x00000000, 0x6B000000);

        // A handful of slow amber sparks. Deterministic, so the scene is stable.
        float time = animations() ? state.time : 0f;
        for (int i = 0; i < 16; i++) {
            float seed = i * 78.233f;
            float baseX = (float) ((Math.sin(seed) * 0.5f + 0.5f) * w);
            float speed = 6f + (i % 5) * 3f;
            float y = h - ((time * speed + i * 61f) % (h + 80f)) + 40f;
            float x = baseX + (float) Math.sin(time * 0.35f + i) * 10f;
            float alpha = 0.1f + 0.18f * (float) Math.abs(Math.sin(time * 0.9f + i));
            float size = i % 4 == 0 ? 1.6f : 1f;
            r.circle(x, y, size, Theme.accentSoft(alpha));
        }
    }

    private void drawSidebar(Render2D r, MenuState state, MenuLayout layout) {
        Frame side = layout.sidebar;
        r.glass(side.x(), side.y(), side.right(), side.bottom(), Theme.RADIUS_PANEL, 0x70000000, 14f);
        r.accentEdge(side.x(), side.y(), side.right(), side.bottom(), Theme.RADIUS_PANEL);

        r.textTop("Ast", side.x() + 15f, side.y() + 14f, Fonts.semibold(18f), Theme.TEXT);
        Icons.draw(r, Icons.Glyph.STAR, side.x() + 45f, side.y() + 19f, 5.5f, Theme.accentSoft(0.9f));
        r.textTracked("ASTATIN", side.x() + 52f, side.y() + 30f, Fonts.bold(11.5f), Theme.TEXT, 0.8f);
        r.textTracked("CLIENT", side.x() + 52.5f, side.y() + 42f, Fonts.medium(6.2f), Theme.TEXT_MUTED, 1.2f);

        r.line(side.x() + 14f, side.y() + 55f, side.right() - 14f, side.y() + 55f, 1f, 0x12FFFFFF);
        r.textTracked("CATEGORIES", side.x() + 16f, side.y() + 70f, Fonts.medium(6f), Theme.TEXT_MUTED, 1.2f);

        // sliding active marker
        float markerIndex = indicator.get();
        int markerRow = Math.round(markerIndex);
        if (!layout.compact) {
            Frame marker = layout.category(markerRow);
            float ease = Anim.outCubic(Math.abs(markerIndex - markerRow) < 0.02f ? 1f : 0.65f);
            float y = marker.y() + 11f;
            r.roundRect(side.x() + 9f, y, side.x() + 12f, y + 15f, 1.5f,
                    Theme.accentSoft(0.9f * ease));
        }

        Category[] categories = Category.values();
        for (Category category : categories) {
            Frame row = layout.category(category.ordinal());
            float hover = anim("cat." + category.ordinal()).get();
            boolean active = state.category == category && state.search.isBlank();
            if (active) {
                r.gradientRectHorizontal(row.x(), row.y(), row.right(), row.bottom(), 9f,
                        Theme.accentSoft(0.13f), Theme.accentSoft(0.015f));
                r.roundRectStroke(row.x() + 0.5f, row.y() + 0.5f, row.right() - 0.5f, row.bottom() - 0.5f,
                        9f, 1f, Theme.accentSoft(0.18f));
            } else if (hover > 0.01f) {
                r.roundRect(row.x(), row.y(), row.right(), row.bottom(), 9f, Theme.withAlpha(0xFFFFFFFF, 0.035f * hover));
            }
            int iconColor = active ? Theme.AMBER : Theme.mix(Theme.TEXT_MUTED, Theme.TEXT_SOFT, hover);
            Icons.draw(r, category.icon, row.x() + 19f, row.centerY(), 14f, iconColor);
            int textColor = active ? Theme.TEXT : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, hover);
            r.textCenterY(category.label, row.x() + 36f, row.centerY() - 1f, Fonts.medium(11f), textColor);

            List<Module> categoryModules = modules.byCategory(category);
            int enabled = 0;
            for (Module module : categoryModules) {
                if (module.enabled) {
                    enabled++;
                }
            }
            String count = enabled + "/" + categoryModules.size();
            r.textCenterYRight(count, row.right() - 12f, row.centerY() - 1f, Fonts.medium(8f),
                    active ? Theme.accentSoft(0.85f) : Theme.TEXT_MUTED);
        }

        Frame profile = layout.profile;
        r.roundRect(profile.x(), profile.y(), profile.right(), profile.bottom(), 10f, 0x0AFFFFFF);
        r.roundRectStroke(profile.x() + 0.5f, profile.y() + 0.5f, profile.right() - 0.5f, profile.bottom() - 0.5f,
                10f, 1f, 0x10FFFFFF);
        float avatarX = profile.x() + 9f;
        float avatarY = profile.y() + 9f;
        r.gradientRect(avatarX, avatarY, avatarX + 28f, avatarY + 28f, 8f,
                Theme.accentLight(0.12f), Theme.accentDark(0.05f));
        r.textCenteredTop("A", avatarX + 14f, avatarY + 7.5f, Fonts.bold(12f), Theme.TEXT_ON_ACCENT);
        r.textCenterY("Astatin", profile.x() + 45f, profile.y() + 17f, Fonts.semibold(10f), Theme.TEXT);
        r.textCenterY("v1.0.0 · Fabric", profile.x() + 45f, profile.y() + 30f, Fonts.regular(7.5f), Theme.TEXT_MUTED);
        Icons.draw(r, Icons.Glyph.SLIDERS, profile.right() - 18f, profile.centerY(), 12f, Theme.TEXT_MUTED);

        String hint = "RIGHT SHIFT TO CLOSE";
        r.textTracked(hint, side.x() + (side.w() - r.trackedWidth(hint, Fonts.medium(6f), 1.1f)) * 0.5f,
                side.bottom() - 9f, Fonts.medium(6f), Theme.TEXT_MUTED, 1.1f);
    }

    private void drawHeader(Render2D r, MenuState state, MenuLayout layout) {
        Frame main = layout.main;
        Category category = state.category;
        r.roundRect(main.x() + 18f, main.y() + 15f, main.x() + 48f, main.y() + 45f, 9f, Theme.accentSoft(0.10f));
        r.roundRectStroke(main.x() + 18.5f, main.y() + 15.5f, main.x() + 47.5f, main.y() + 44.5f, 9f, 1f,
                Theme.accentSoft(0.22f));
        Icons.draw(r, category.icon, main.x() + 33f, main.y() + 30f, 16f, Theme.AMBER);

        String title;
        String subtitle;
        if (!state.search.isBlank()) {
            List<Module> results = modules.search(state.search);
            title = "Search";
            subtitle = results.size() + (results.size() == 1 ? " result" : " results") + " for \"" + state.search + "\"";
        } else {
            List<Module> list = modules.byCategory(category);
            title = category.label;
            subtitle = category.description + "  ·  " + list.size() + " modules";
        }
        r.textTop(title, main.x() + 58f, main.y() + 17f, Fonts.bold(16f), Theme.TEXT);
        r.textTop(subtitle, main.x() + 58f, main.y() + 38f, Fonts.regular(8.4f), Theme.TEXT_MUTED);
        r.line(main.x() + 18f, main.y() + HEADER_LINE, main.right() - 18f, main.y() + HEADER_LINE, 1f, 0x0EFFFFFF);

        // search
        Frame search = layout.search;
        float focus = searchFocus.get();
        r.roundRect(search.x(), search.y(), search.right(), search.bottom(), 9f, 0x3D000000);
        r.roundRectStroke(search.x() + 0.5f, search.y() + 0.5f, search.right() - 0.5f, search.bottom() - 0.5f,
                9f, 1f, Theme.mix(0x18FFFFFF, Theme.accentSoft(0.45f), Math.max(focus, anim("search").get() * 0.5f)));
        Icons.draw(r, Icons.Glyph.SEARCH, search.x() + 13f, search.centerY(), 11f,
                Theme.mix(Theme.TEXT_MUTED, Theme.AMBER, focus));
        String shown = state.search.isEmpty() ? "Search modules…" : state.search;
        int textColor = state.search.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT_SOFT;
        r.textCenterY(shown, search.x() + 25f, search.centerY() + 0.5f, Fonts.regular(9.5f), textColor);
        if (state.searchFocused) {
            float caretX = search.x() + 25f + r.width(state.search, Fonts.regular(9.5f)) + 1f;
            if (state.time % 1f < 0.55f) {
                r.rect(caretX, search.centerY() - 6f, caretX + 1f, search.centerY() + 6f, Theme.AMBER);
            }
        }
        if (!state.search.isEmpty()) {
            Icons.draw(r, Icons.Glyph.CLOSE, search.right() - 13f, search.centerY(), 8f, Theme.TEXT_MUTED);
        }

        // HUD editor shortcut
        if (state.category == Category.HUD && !layout.compact) {
            Frame edit = layout.edit;
            float hover = anim("edit").get();
            r.roundRect(edit.x(), edit.y(), edit.right(), edit.bottom(), 9f,
                    Theme.accentSoft(0.08f + 0.08f * hover));
            r.roundRectStroke(edit.x() + 0.5f, edit.y() + 0.5f, edit.right() - 0.5f, edit.bottom() - 0.5f,
                    9f, 1f, Theme.accentSoft(0.24f + 0.18f * hover));
            Icons.draw(r, Icons.Glyph.EDIT, edit.x() + 14f, edit.centerY(), 10f, Theme.AMBER);
            String label = "EDIT HUD";
            r.textTracked(label, edit.x() + 24f, edit.y() + 18.5f, Fonts.medium(6.3f),
                    Theme.accentSoft(0.95f), 0.9f);
        }

        // close
        Frame close = layout.close;
        float closeHover = anim("close").get();
        if (closeHover > 0.01f) {
            r.roundRect(close.x(), close.y(), close.right(), close.bottom(), 8f,
                    Theme.withAlpha(0xFFFFFFFF, 0.07f * closeHover));
        }
        Icons.draw(r, Icons.Glyph.CLOSE, close.centerX(), close.centerY(), 11f,
                Theme.mix(Theme.TEXT_MUTED, Theme.TEXT, closeHover));
    }

    private static final float HEADER_LINE = 59f;

    private void drawGrid(Render2D r, MenuState state, MenuLayout layout, List<Module> visible) {
        Frame grid = layout.grid;
        if (visible.isEmpty()) {
            Icons.draw(r, Icons.Glyph.SEARCH, grid.centerX(), grid.y() + grid.h() * 0.34f, 28f, Theme.TEXT_MUTED);
            r.textCenteredTop("Nothing here yet", grid.centerX(), grid.y() + grid.h() * 0.34f + 24f,
                    Fonts.semibold(11f), Theme.TEXT_SOFT);
            r.textCenteredTop(state.search.isBlank() ? "This category has no modules"
                            : "No module matches \"" + state.search + "\"",
                    grid.centerX(), grid.y() + grid.h() * 0.34f + 40f, Fonts.regular(8.5f), Theme.TEXT_MUTED);
            return;
        }

        r.save();
        r.clipRect(grid.x() - 2f, grid.y() - 6f, grid.right() + 2f, grid.bottom() + 2f);
        for (int i = 0; i < visible.size(); i++) {
            Frame card = layout.card(i, visible.size(), state.scroll);
            if (card.bottom() < grid.y() - 8f || card.y() > grid.bottom() + 8f) {
                continue;
            }
            drawCard(r, state, layout, visible.get(i), card, i);
        }
        r.restore();

        float maxScroll = layout.maxScroll(visible.size());
        if (maxScroll > 0.5f) {
            float trackX = layout.main.right() - 7f;
            float trackTop = grid.y();
            float trackH = grid.h();
            r.roundRect(trackX, trackTop, trackX + 3f, trackTop + trackH, 1.5f, 0x12FFFFFF);
            float thumbH = Math.max(26f, trackH * (grid.h() / layout.contentHeight(visible.size())));
            float thumbY = trackTop + (trackH - thumbH) * (state.scroll / maxScroll);
            r.roundRect(trackX, thumbY, trackX + 3f, thumbY + thumbH, 1.5f, Theme.accentSoft(0.55f));
        }
    }

    private void drawCard(Render2D r, MenuState state, MenuLayout layout, Module module, Frame card, int index) {
        float hover = anim("card." + module.id).get();
        float lift = hover * 1.5f;
        float x = card.x();
        float y = card.y() - lift;
        boolean selected = state.selected == module;
        boolean enabled = module.enabled;

        if (hover > 0.02f || selected) {
            r.shadow(x + 2f, y + 5f, card.right() - 2f, card.bottom() + 6f, Theme.RADIUS_CARD + 1f,
                    10f, selected ? Theme.accentSoft(0.10f) : 0x33000000);
        }
        int fill = selected ? Theme.accentSoft(0.055f)
                : Theme.withAlpha(0xFFFFFFFF, 0.030f + 0.028f * hover);
        r.roundRect(x, y, card.right(), card.bottom(), Theme.RADIUS_CARD, fill);
        int border = selected ? Theme.accentSoft(0.48f)
                : Theme.mix(0x0EFFFFFF, Theme.accentSoft(0.26f), hover);
        r.roundRectStroke(x + 0.5f, y + 0.5f, card.right() - 0.5f, card.bottom() - 0.5f,
                Theme.RADIUS_CARD, 1f, border);

        if (enabled) {
            float barY = y + 16f;
            r.roundRect(x + 1f, barY, x + 2.6f, barY + 22f, 0.8f, Theme.accentSoft(0.8f));
        }

        // icon tile
        float tile = 30f;
        r.roundRect(x + 15f, y + 16f, x + 15f + tile, y + 16f + tile, 9f,
                enabled ? Theme.accentSoft(0.12f) : 0x0CFFFFFF);
        if (enabled) {
            r.roundRectStroke(x + 15.5f, y + 16.5f, x + 14.5f + tile, y + 15.5f + tile, 9f, 1f,
                    Theme.accentSoft(0.20f));
        }
        Icons.draw(r, iconFor(module), x + 15f + tile * 0.5f, y + 16f + tile * 0.5f, 15f,
                enabled ? Theme.AMBER : Theme.TEXT_DIM);

        r.textTop(module.name, x + 54f, y + 15f, Fonts.semibold(12.5f), Theme.TEXT);
        String desc = trim(r, module.description, Fonts.regular(8.7f), card.w() - 70f);
        r.textTop(desc, x + 54f, y + 33f, Fonts.regular(8.7f), Theme.TEXT_MUTED);

        // footer
        float footerY = card.bottom() - 19f;
        r.circle(x + 18f, footerY + 4f, 2.2f, enabled ? Theme.AMBER : Theme.TEXT_MUTED);
        r.textTracked(enabled ? "ACTIVE" : "OFF", x + 24f, footerY + 7f, Fonts.medium(6.2f),
                enabled ? Theme.AMBER : Theme.TEXT_MUTED, 1f);

        List<Setting> settings = module.settings;
        if (module.action) {
            r.textTracked("OPEN", card.right() - 16f - r.trackedWidth("OPEN", Fonts.medium(6.2f), 1f),
                    footerY + 7f, Fonts.medium(6.2f), Theme.AMBER, 1f);
        } else if (!settings.isEmpty() && !state.search.isBlank()) {
            r.textTracked(module.category.label.toUpperCase(), card.right() - 16f
                            - r.trackedWidth(module.category.label.toUpperCase(), Fonts.medium(6.2f), 1f),
                    footerY + 7f, Fonts.medium(6.2f), Theme.TEXT_MUTED, 1f);
        } else if (!settings.isEmpty()) {
            String count = settings.size() + (settings.size() == 1 ? " OPTION" : " OPTIONS");
            r.textTracked(count, card.right() - 16f - r.trackedWidth(count, Fonts.medium(6.2f), 1f),
                    footerY + 7f, Fonts.medium(6.2f), Theme.TEXT_MUTED, 1f);
        }

        if (!module.action) {
            drawToggle(r, layout.cardToggle(new Frame(x, y, card.w(), card.h())), module, enabled);
        } else {
            Icons.draw(r, Icons.Glyph.CHEVRON_RIGHT, card.right() - 24f, y + 25f, 10f,
                    Theme.AMBER);
        }
    }

    private Icons.Glyph iconFor(Module module) {
        return switch (module.id) {
            case "watermark" -> Icons.Glyph.STAR;
            case "keystrokes" -> Icons.Glyph.KEY;
            case "cps" -> Icons.Glyph.GAUGE;
            case "coords" -> Icons.Glyph.CROSSHAIR;
            case "armor" -> Icons.Glyph.SHIELD;
            case "potions" -> Icons.Glyph.EYE;
            case "zoom" -> Icons.Glyph.ZOOM;
            case "fullbright" -> Icons.Glyph.SUN;
            case "vignette" -> Icons.Glyph.CONTRAST;
            case "clickgui" -> Icons.Glyph.INTERFACE;
            case "hudeditor" -> Icons.Glyph.EDIT;
            case "sprint" -> Icons.Glyph.SPEED;
            default -> module.category == Category.HUD ? Icons.Glyph.HUD : Icons.Glyph.SLIDERS;
        };
    }

    private void drawInspector(Render2D r, MenuState state, MenuLayout layout, Module module) {
        float p = inspector.get();
        Frame panel = layout.inspector;
        float offset = (1f - p) * 26f;

        // dim the grid behind the inspector and keep the panel itself readable
        r.rect(layout.main.x() + 1f, layout.grid.y() - 6f, layout.main.right() - 1f, layout.grid.bottom() + 6f,
                Theme.withAlpha(0x000000, 0.44f * p));

        panelPaint.setAlphaf(Math.min(1f, p));
        r.canvas().saveLayer(Rect.makeLTRB(panel.x() - 4f, panel.y() - 4f, panel.right() + 4f, panel.bottom() + 4f),
                panelPaint);
        r.save();
        r.translate(offset, 0f);

        r.roundRect(panel.x(), panel.y(), panel.right(), panel.bottom(), 14f, 0xF20B0C0F);
        r.glass(panel.x(), panel.y(), panel.right(), panel.bottom(), 14f, 0x88000000, 16f);
        r.accentEdge(panel.x(), panel.y(), panel.right(), panel.bottom(), 14f);

        Frame close = layout.inspectorClose();
        float closeHover = anim("ins.close").get();
        if (closeHover > 0.01f) {
            r.roundRect(close.x(), close.y(), close.right(), close.bottom(), 7f,
                    Theme.withAlpha(0xFFFFFFFF, 0.07f * closeHover));
        }
        Icons.draw(r, Icons.Glyph.CLOSE, close.centerX(), close.centerY(), 9f,
                Theme.mix(Theme.TEXT_MUTED, Theme.TEXT, closeHover));

        r.textTop(module.name, panel.x() + 16f, panel.y() + 15f, Fonts.semibold(13.5f), Theme.TEXT);
        r.textTracked(module.category.label.toUpperCase(), panel.x() + 16f, panel.y() + 34f,
                Fonts.medium(6.2f), Theme.AMBER, 1f);

        r.line(panel.x() + 16f, panel.y() + 50f, panel.right() - 16f, panel.y() + 50f, 1f, 0x12FFFFFF);

        if (module.action) {
            Frame action = layout.inspectorAction();
            float hover = anim("ins.action").get();
            r.gradientRect(action.x(), action.y(), action.right(), action.bottom(), 8f,
                    Theme.accentLight(0.10f + 0.06f * hover), Theme.accentDark(0.02f + 0.03f * hover));
            r.roundRectStroke(action.x() + 0.5f, action.y() + 0.5f, action.right() - 0.5f, action.bottom() - 0.5f,
                    8f, 1f, Theme.accentSoft(0.40f + 0.2f * hover));
            Icons.draw(r, Icons.Glyph.EDIT, action.x() + 18f, action.centerY(), 11f, Theme.TEXT_ON_ACCENT);
            r.textCenterY("OPEN HUD EDITOR", action.x() + 30f, action.centerY(), Fonts.semibold(8.5f),
                    Theme.TEXT_ON_ACCENT);
        } else {
            r.textCenterY("Enabled", panel.x() + 16f, panel.y() + 66f, Fonts.medium(9.5f), Theme.TEXT_SOFT);
            drawToggle(r, layout.inspectorToggle(), module, module.enabled);
        }

        // description
        drawWrapped(r, module.description, panel.x() + 16f, panel.y() + (module.action ? 96f : 84f),
                panel.w() - 32f, Fonts.regular(8.2f), Theme.TEXT_MUTED, 2, 10.5f);

        r.line(panel.x() + 16f, layout.settingsTop() - 12f, panel.right() - 16f, layout.settingsTop() - 12f,
                1f, 0x0EFFFFFF);

        if (module.settings.isEmpty()) {
            r.textTop("No settings for this module.", panel.x() + 16f, layout.settingsTop(),
                    Fonts.regular(8.5f), Theme.TEXT_MUTED);
        } else {
            for (int i = 0; i < module.settings.size(); i++) {
                Setting setting = module.settings.get(i);
                Frame row = layout.settingRow(module, i, state.openPicker);
                if (row.y() > panel.bottom() - 20f) {
                    break;
                }
                drawSetting(r, state, layout, setting, row);
            }
        }

        r.circle(panel.x() + 20f, panel.bottom() - 14f, 2f, Theme.GREEN);
        r.textCenterY("Changes save automatically", panel.x() + 26f, panel.bottom() - 14f,
                Fonts.regular(7.4f), Theme.TEXT_MUTED);

        r.restore();
        r.canvas().restore();
    }

    private void drawSetting(Render2D r, MenuState state, MenuLayout layout, Setting setting, Frame row) {
        if (setting.kind == Setting.Kind.BOOL) {
            r.textCenterY(setting.name, row.x(), row.y() + 13f, Fonts.medium(9.2f), Theme.TEXT_SOFT);
            drawToggle(r, new Frame(row.right() - 34f, row.y() + 4f, 34f, 18f),
                    setting.boolValue, "set." + setting.key());
        } else if (setting.kind == Setting.Kind.SLIDER) {
            r.textCenterY(setting.name, row.x(), row.y() + 9f, Fonts.medium(9.2f), Theme.TEXT_SOFT);
            String value = setting.displayValue() + (setting.unit.isEmpty() ? "" : " " + setting.unit);
            r.textCenterYRight(value, row.right(), row.y() + 9f, Fonts.semibold(8.5f), Theme.AMBER);
            Frame track = layout.sliderTrack(row);
            r.roundRect(track.x(), track.y(), track.right(), track.bottom(), 2f, 0x1CFFFFFF);
            float t = setting.normalized();
            if (t > 0.001f) {
                r.gradientRectHorizontal(track.x(), track.y(), track.x() + track.w() * t, track.bottom(), 2f,
                        Theme.accentLight(0.15f), Theme.accentDark(0.05f));
            }
            float knobX = track.x() + track.w() * t;
            r.circle(knobX, track.centerY(), 7f, Theme.accentSoft(0.16f));
            r.circle(knobX, track.centerY(), 3.6f, 0xFFFFFFFF);
            r.circleStroke(knobX, track.centerY(), 3.6f, 1.2f, Theme.accentSoft(0.9f));
        } else {
            r.textCenterY(setting.name, row.x(), row.y() + 14f, Fonts.medium(9.2f), Theme.TEXT_SOFT);
            r.textCenterYRight(Theme.hex(setting.color), row.right() - 28f, row.y() + 14f,
                    Fonts.medium(8f), Theme.TEXT_MUTED);
            Frame swatch = layout.colorSwatch(row);
            r.roundRect(swatch.x(), swatch.y(), swatch.right(), swatch.bottom(), 6f, setting.color);
            r.roundRectStroke(swatch.x() + 0.5f, swatch.y() + 0.5f, swatch.right() - 0.5f, swatch.bottom() - 0.5f,
                    6f, 1f, 0x33FFFFFF);
            if (state.openPicker == setting) {
                drawPicker(r, state, layout.colorPicker(row));
            }
        }
    }

    private void drawPicker(Render2D r, MenuState state, Frame picker) {
        Frame square = layoutSquare(picker);
        int hueColor = ColorUtil.hsv(state.pickerHue, 1f, 1f);
        r.roundRect(square.x(), square.y(), square.right(), square.bottom(), 7f, hueColor);
        r.save();
        r.clipRoundRect(square.x(), square.y(), square.right(), square.bottom(), 7f);
        r.gradientRectHorizontal(square.x(), square.y(), square.right(), square.bottom(), 0f,
                0xFFFFFFFF, ColorUtil.hsv(state.pickerHue, 1f, 1f, 1f));
        r.gradientRect(square.x(), square.y(), square.right(), square.bottom(), 0f,
                0x00000000, 0xFF000000);
        r.restore();
        r.roundRectStroke(square.x() + 0.5f, square.y() + 0.5f, square.right() - 0.5f, square.bottom() - 0.5f,
                7f, 1f, 0x26FFFFFF);

        float cursorX = square.x() + state.pickerSat * square.w();
        float cursorY = square.y() + (1f - state.pickerVal) * square.h();
        r.circle(cursorX, cursorY, 5f, 0xCC000000);
        r.circleStroke(cursorX, cursorY, 4.2f, 1.6f, 0xFFFFFFFF);

        Frame hue = layoutHue(picker);
        r.save();
        r.clipRoundRect(hue.x(), hue.y(), hue.right(), hue.bottom(), 5f);
        int[] colors = {0xFFFF3B30, 0xFFFFCC00, 0xFF34C759, 0xFF32ADE6, 0xFF5856D6, 0xFFFF2D55, 0xFFFF3B30};
        float segment = hue.h() / (colors.length - 1f);
        for (int i = 0; i < colors.length - 1; i++) {
            r.gradientRect(hue.x(), hue.y() + segment * i, hue.right(), hue.y() + segment * (i + 1) + 0.5f, 0f,
                    colors[i], colors[i + 1]);
        }
        r.restore();
        float hueY = hue.y() + (state.pickerHue / 360f) * hue.h();
        r.roundRect(hue.x() - 2f, hueY - 2.5f, hue.right() + 2f, hueY + 2.5f, 2.5f, 0xFFFFFFFF);
        r.roundRectStroke(hue.x() - 1f, hueY - 1.5f, hue.right() + 1f, hueY + 1.5f, 1.5f, 1f, 0x66000000);

        Frame hex = layoutHex(picker);
        int color = state.openPicker != null ? state.openPicker.color : ColorUtil.hsv(state.pickerHue, state.pickerSat, state.pickerVal);
        r.roundRect(hex.x(), hex.y() - 1f, hex.x() + 12f, hex.y() + 11f, 3.5f, color);
        r.roundRectStroke(hex.x() + 0.5f, hex.y() - 0.5f, hex.x() + 11.5f, hex.y() + 10.5f, 3.5f, 1f, 0x33FFFFFF);
        r.textCenterY(Theme.hex(color), hex.x() + 19f, hex.y() + 5f, Fonts.medium(8f), Theme.TEXT_SOFT);
    }

    private Frame layoutSquare(Frame picker) {
        return new Frame(picker.x(), picker.y(), picker.w() - 22f, 74f);
    }

    private Frame layoutHue(Frame picker) {
        return new Frame(picker.right() - 14f, picker.y(), 8f, 74f);
    }

    private Frame layoutHex(Frame picker) {
        return new Frame(picker.x(), picker.y() + 82f, picker.w(), 14f);
    }

    // --------------------------------------------------------------- widgets

    private void drawToggle(Render2D r, Frame frame, Module module, boolean enabled) {
        drawToggle(r, frame, enabled, "toggle." + module.id);
    }

    private void drawToggle(Render2D r, Frame frame, boolean enabled, String animKey) {
        float t = anim(animKey).get();
        int track = Theme.mix(0x1FFFFFFF, Theme.accentSoft(0.95f), t);
        r.roundRect(frame.x(), frame.y(), frame.right(), frame.bottom(), frame.h() * 0.5f, track);
        if (t > 0.02f) {
            r.shadow(frame.x() + 1f, frame.y() + 2f, frame.right() - 1f, frame.bottom() + 2f,
                    frame.h() * 0.5f, 6f, Theme.accentSoft(0.28f * t));
            r.roundRect(frame.x(), frame.y(), frame.right(), frame.bottom(), frame.h() * 0.5f,
                    Theme.accentSoft(0.96f * t));
        }
        r.roundRectStroke(frame.x() + 0.5f, frame.y() + 0.5f, frame.right() - 0.5f, frame.bottom() - 0.5f,
                frame.h() * 0.5f, 1f, t > 0.02f ? Theme.accentLight(0.35f) : 0x24FFFFFF);
        float knobSize = frame.h() - 5f;
        float left = frame.x() + 2.5f;
        float right = frame.right() - 2.5f - knobSize;
        float x = left + (right - left) * t;
        r.shadow(x, frame.y() + 2.5f, x + knobSize, frame.y() + 2.5f + knobSize, knobSize * 0.5f, 3f, 0x55000000);
        r.circle(x + knobSize * 0.5f, frame.centerY(), knobSize * 0.5f, 0xFFFFFFFF);
    }

    private String trim(Render2D r, String value, io.github.humbleui.skija.Font font, float maxWidth) {
        if (r.width(value, font) <= maxWidth) {
            return value;
        }
        String result = value;
        while (result.length() > 1 && r.width(result + "…", font) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private void drawWrapped(Render2D r, String text, float x, float y, float maxWidth,
                             io.github.humbleui.skija.Font font, int color, int maxLines, float lineHeight) {
        StringBuilder line = new StringBuilder();
        float cursorY = y;
        int drawn = 0;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (r.width(candidate, font) > maxWidth && !line.isEmpty()) {
                r.textTop(line.toString(), x, cursorY, font, color);
                drawn++;
                cursorY += lineHeight;
                line = new StringBuilder(word);
                if (drawn >= maxLines) {
                    return;
                }
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty() && drawn < maxLines) {
            r.textTop(line.toString(), x, cursorY, font, color);
        }
    }
}
