package dev.astatin.hud;

import dev.astatin.config.ConfigData;
import dev.astatin.menu.Frame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    @Test
    void compactDefaultsFitWithoutOverlapEvenWithFourPotions() {
        for (float[] size : new float[][]{{320, 240}, {480, 270}, {360, 640}, {854, 240}}) {
            HudManager hud = new HudManager();
            hud.ensureDefaults(size[0], size[1], new ConfigData());
            HudData data = new HudData();
            for (int i = 0; i < 4; i++) data.potions.add(new HudData.PotionInfo());
            for (int i = 0; i < hud.elements().size(); i++) {
                HudElement a = hud.elements().get(i);
                float ax = a.xOnScreen(size[0], data), ay = a.yOnScreen(size[1], data);
                assertTrue(ax >= 0f && ay >= 0f && ax + a.width(data) <= size[0] + .01f
                        && ay + a.height(data) <= size[1] + .01f, a.id);
                assertTrue(ay + a.height(data) <= size[1] - 42f + .01f,
                        a.id + " overlaps the reserved health and hotbar area");
                for (int j = i + 1; j < hud.elements().size(); j++) {
                    HudElement b = hud.elements().get(j);
                    float bx = b.xOnScreen(size[0], data), by = b.yOnScreen(size[1], data);
                    assertTrue(ax + a.width(data) <= bx + .01f || bx + b.width(data) <= ax + .01f
                            || ay + a.height(data) <= by + .01f || by + b.height(data) <= ay + .01f,
                            a.id + " overlaps " + b.id + " at " + size[0] + "x" + size[1]);
                }
            }
        }
    }

    @Test
    void savedPositionsAndScaleArePreserved() {
        ConfigData config = new ConfigData();
        ConfigData.HudState saved = new ConfigData.HudState("watermark", false, .83f, .27f, 1.25f);
        config.hud.add(saved);
        HudManager hud = new HudManager();
        hud.ensureDefaults(320, 240, config);
        HudElement element = hud.get("watermark");
        assertSame(saved, config.hud("watermark"));
        assertFalse(element.enabled);
        assertEquals(.83f, element.normX);
        assertEquals(.27f, element.normY);
        assertEquals(1.25f, element.scale);
        assertEquals(.83f, saved.x);
        assertEquals(.27f, saved.y);
        assertEquals(1.25f, saved.scale);
    }

    @Test
    void desktopDefaultsAndEditorCloseGeometry() {
        HudManager hud = new HudManager();
        hud.ensureDefaults(854, 480, new ConfigData());
        HudElement watermark = hud.get("watermark");
        assertEquals(1f, watermark.scale);
        assertEquals(14f, watermark.xOnScreen(854, new HudData()), .001f);
        assertEquals(14f, watermark.yOnScreen(480, new HudData()), .001f);
        Frame close = HudManager.editorClose(320);
        assertTrue(close.x() >= 0 && close.right() <= 320 && close.y() >= 0 && close.bottom() <= 240);
        assertTrue(close.contains(close.centerX(), close.centerY()));
    }

    @Test
    void resetUsesCompactDefaultOnlyOnSmallScreens() {
        HudManager defaults = new HudManager();
        defaults.ensureDefaults(320, 240, new ConfigData());
        HudManager hud = new HudManager();
        ConfigData config = new ConfigData();
        config.hud.add(new ConfigData.HudState("watermark", true, .8f, .9f, 1.4f));
        hud.ensureDefaults(320, 240, config);
        HudElement element = hud.get("watermark");
        hud.reset(element, 320, 240, config);
        HudElement expected = defaults.get("watermark");
        assertEquals(expected.normX, element.normX);
        assertEquals(expected.normY, element.normY);
        assertEquals(expected.scale, element.scale);
        assertEquals(.8f, config.hud("watermark").x);
        hud.reset(element, 854, 480, config);
        assertEquals(1f, element.scale);
        assertEquals(14f, element.xOnScreen(854, new HudData()), .001f);
        assertEquals(14f, element.yOnScreen(480, new HudData()), .001f);
    }

    @Test
    void resizeKeepsSmallDefaultsWithoutChangingDesktopMinimum() {
        HudManager hud = new HudManager();
        hud.ensureDefaults(320, 240, new ConfigData());
        HudElement element = hud.get("watermark");
        float compactScale = element.scale;
        assertTrue(compactScale < .65f);
        hud.resize(element, -.1f, 320, 240);
        assertEquals(compactScale, element.scale, .001f);
        hud.resize(element, .03f, 320, 240);
        assertEquals(compactScale + .03f, element.scale, .001f);
        hud.resize(element, -.5f, 854, 480);
        assertEquals(.65f, element.scale);
    }
}
