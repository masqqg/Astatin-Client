package dev.astatin.hud;

import dev.astatin.config.ConfigData;
import dev.astatin.render.Render2D;
import dev.astatin.theme.Fonts;
import dev.astatin.theme.Theme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the HUD widgets, their persisted positions and the edit overlay. Screen
 * coordinates are derived from normalized anchors so a resize never throws the
 * layout away.
 */
public final class HudManager {
    private final List<HudElement> elements = new ArrayList<>();
    private final Map<String, HudElement> byId = new LinkedHashMap<>();

    public HudManager() {
        add(new WatermarkElement());
        add(new PotionsElement());
        add(new KeystrokesElement());
        add(new CpsElement());
        add(new ArmorElement());
        add(new CoordsElement());
    }

    private void add(HudElement element) {
        elements.add(element);
        byId.put(element.id, element);
    }

    public List<HudElement> elements() {
        return elements;
    }

    public HudElement get(String id) {
        return byId.get(id);
    }

    public void ensureDefaults(float screenWidth, float screenHeight, ConfigData config) {
        for (HudElement element : elements) {
            ConfigData.HudState state = config.hud(element.id);
            if (state == null) {
                state = defaultState(element, screenWidth, screenHeight);
                config.hud.add(state);
            }
            element.enabled = state.enabled;
            element.normX = state.x;
            element.normY = state.y;
            element.scale = state.scale <= 0f ? 1f : state.scale;
        }
    }

    private ConfigData.HudState defaultState(HudElement element, float w, float h) {
        HudData data = new HudData();
        float elW = element.baseWidth(data);
        float elH = element.baseHeight(data);
        float x;
        float y;
        switch (element.id) {
            case "watermark" -> {
                x = 14f;
                y = 14f;
            }
            case "potions" -> {
                x = w - 14f - elW;
                y = 14f;
            }
            case "keystrokes" -> {
                x = 14f;
                y = h - 14f - elH;
            }
            case "cps" -> {
                x = 14f + 104f + 8f;
                y = h - 14f - elH;
            }
            case "coords" -> {
                x = w - 14f - elW;
                y = h - 14f - elH;
            }
            case "armor" -> {
                x = w - 14f - elW;
                y = h - 14f - 69f - 8f - elH;
            }
            default -> {
                x = 20f;
                y = 20f;
            }
        }
        return new ConfigData.HudState(element.id, element.enabled,
                clamp01(x / Math.max(1f, w - elW)),
                clamp01(y / Math.max(1f, h - elH)), element.scale);
    }

    private static float clamp01(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }

    public void store(ConfigData config) {
        for (HudElement element : elements) {
            ConfigData.HudState state = config.hud(element.id);
            if (state == null) {
                state = new ConfigData.HudState(element.id, element.enabled, element.normX, element.normY, element.scale);
                config.hud.add(state);
            }
            state.enabled = element.enabled;
            state.x = element.normX;
            state.y = element.normY;
            state.scale = element.scale;
        }
    }

    public void render(Render2D r, HudData data, float screenWidth, float screenHeight) {
        for (HudElement element : elements) {
            if (!element.enabled) {
                continue;
            }
            float x = element.xOnScreen(screenWidth, data);
            float y = element.yOnScreen(screenHeight, data);
            element.drawAt(r, data, x, y);
        }
    }

    public void renderEditOverlay(Render2D r, HudData data, float screenWidth, float screenHeight) {
        for (HudElement element : elements) {
            if (!element.enabled) {
                continue;
            }
            float x = element.xOnScreen(screenWidth, data);
            float y = element.yOnScreen(screenHeight, data);
            float w = element.width(data);
            float h = element.height(data);
            float pad = 4f;

            r.roundRect(x - pad, y - pad, x + w + pad, y + h + pad, 8f, Theme.accentSoft(0.045f));
            r.roundRectStroke(x - pad, y - pad, x + w + pad, y + h + pad, 8f, 1f, Theme.accentSoft(0.35f));

            float c = 9f;
            int accent = Theme.accentSoft(0.9f);
            // corner brackets
            r.line(x - pad, y - pad, x - pad + c, y - pad, 1.6f, accent);
            r.line(x - pad, y - pad, x - pad, y - pad + c, 1.6f, accent);
            r.line(x + w + pad - c, y - pad, x + w + pad, y - pad, 1.6f, accent);
            r.line(x + w + pad, y - pad, x + w + pad, y - pad + c, 1.6f, accent);
            r.line(x - pad, y + h + pad - c, x - pad, y + h + pad, 1.6f, accent);
            r.line(x - pad, y + h + pad, x - pad + c, y + h + pad, 1.6f, accent);
            r.line(x + w + pad - c, y + h + pad, x + w + pad, y + h + pad, 1.6f, accent);
            r.line(x + w + pad, y + h + pad - c, x + w + pad, y + h + pad, 1.6f, accent);

            String label = element.name.toUpperCase();
            float labelW = r.trackedWidth(label, Fonts.medium(6.5f), 0.9f) + 12f;
            r.roundRect(x - pad, y - pad - 15f, x - pad + labelW, y - pad - 4f, 3.5f, Theme.accentSoft(0.92f));
            r.textTracked(label, x - pad + 6f, y - pad - 7.5f, Fonts.medium(6.5f), Theme.TEXT_ON_ACCENT, 0.9f);

            // resize handle
            r.circle(x + w + pad, y + h + pad, 5.5f, Theme.accentSoft(0.95f));
            r.circle(x + w + pad, y + h + pad, 2.2f, 0x401A1206);
        }
    }

    public HudElement at(float mouseX, float mouseY, HudData data, float screenWidth, float screenHeight, boolean handleOnly) {
        // Reverse iteration means the visually topmost element wins.
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            if (!element.enabled) {
                continue;
            }
            float x = element.xOnScreen(screenWidth, data);
            float y = element.yOnScreen(screenHeight, data);
            float w = element.width(data);
            float h = element.height(data);
            if (handleOnly) {
                float dx = mouseX - (x + w);
                float dy = mouseY - (y + h);
                if (dx * dx + dy * dy <= 12f * 12f) {
                    return element;
                }
            } else if (mouseX >= x - 4f && mouseX <= x + w + 4f && mouseY >= y - 4f && mouseY <= y + h + 4f) {
                return element;
            }
        }
        return null;
    }

    public void drag(HudElement element, float mouseX, float mouseY, float grabX, float grabY,
                     HudData data, float screenWidth, float screenHeight) {
        float elW = element.width(data);
        float elH = element.height(data);
        float usableW = Math.max(1f, screenWidth - elW);
        float usableH = Math.max(1f, screenHeight - elH);
        element.normX = clamp01((mouseX - grabX) / usableW);
        element.normY = clamp01((mouseY - grabY) / usableH);
    }

    public void resize(HudElement element, float delta) {
        element.scale = Math.max(0.65f, Math.min(1.75f, element.scale + delta));
    }

    public void reset(HudElement element, float screenWidth, float screenHeight, ConfigData config) {
        ConfigData.HudState state = defaultState(element, screenWidth, screenHeight);
        element.normX = state.x;
        element.normY = state.y;
        element.scale = 1f;
    }

    public void drawEditorChrome(Render2D r, float screenWidth, float screenHeight, float micros, int fps) {
        r.rect(0f, 0f, screenWidth, screenHeight, 0x6607080A);
        // dot grid
        int dot = 0x14FFFFFF;
        for (float x = 20f; x < screenWidth; x += 32f) {
            for (float y = 20f; y < screenHeight; y += 32f) {
                r.circle(x, y, 0.8f, dot);
            }
        }

        r.radialGlow(screenWidth * 0.5f, 0f, screenHeight * 0.7f, Theme.accentSoft(0.05f), Theme.accentSoft(0f));

        float perfW = 214f;
        float perfX = (screenWidth - perfW) * 0.5f;
        r.glass(perfX, 12f, perfX + perfW, 44f, 10f, 0x66000000, 10f);
        r.circle(perfX + 14f, 28f, 3f, Theme.AMBER);
        r.textTracked("HUD RENDER", perfX + 23f, 31f, Fonts.medium(6.2f), Theme.TEXT_MUTED, 1f);
        r.textCenterY(String.format(java.util.Locale.ROOT, "%.2f ms", micros), perfX + 88f, 28f,
                Fonts.semibold(10f), micros < 1f ? Theme.GREEN : Theme.AMBER);
        r.line(perfX + 142f, 20f, perfX + 142f, 36f, 1f, 0x18FFFFFF);
        r.textCenterYRight(fps + " FPS", perfX + perfW - 12f, 28f, Fonts.medium(8f), Theme.TEXT_SOFT);

        float panelW = 430f;
        float panelH = 34f;
        float x = (screenWidth - panelW) * 0.5f;
        float y = screenHeight - panelH - 16f;
        r.glass(x, y, x + panelW, y + panelH, 11f, 0x66000000, 10f);
        r.accentEdge(x, y, x + panelW, y + panelH, 11f);
        r.circle(x + 16f, y + panelH * 0.5f, 3f, Theme.AMBER);
        r.textCenterY("Drag to move", x + 26f, y + panelH * 0.5f, Fonts.medium(8.5f), Theme.TEXT_SOFT);
        r.textCenterY("·", x + 94f, y + panelH * 0.5f, Fonts.regular(9f), Theme.TEXT_MUTED);
        r.textCenterY("Scroll to resize", x + 104f, y + panelH * 0.5f, Fonts.medium(8.5f), Theme.TEXT_SOFT);
        r.textCenterY("·", x + 194f, y + panelH * 0.5f, Fonts.regular(9f), Theme.TEXT_MUTED);
        r.textCenterY("Right click to reset", x + 204f, y + panelH * 0.5f, Fonts.medium(8.5f), Theme.TEXT_SOFT);
        r.textCenterY("H to close", x + panelW - 16f - r.width("H to close", Fonts.medium(8.5f)),
                y + panelH * 0.5f, Fonts.medium(8.5f), Theme.AMBER);
    }
}
