package dev.astatin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.astatin.hud.HudData;
import dev.astatin.hud.HudManager;
import dev.astatin.menu.AstatinClickGuiScreen;
import dev.astatin.menu.HudEditorScreen;
import dev.astatin.modules.Module;
import dev.astatin.render.Render2D;
import dev.astatin.render.SkijaRenderer;
import dev.astatin.theme.Theme;
import dev.astatin.title.AstatinTitleScreen;
import dev.astatin.util.FrameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AstatinOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger("astatin/overlay");
    private static boolean errorLogged;

    private AstatinOverlay() {
    }

    public static void render() {
        if (!AstatinClient.ready() || !RenderSystem.isOnRenderThread()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.getOverlay() != null) {
            return;
        }
        Screen screen = mc.screen;
        if (screen != null && !(screen instanceof AstatinClickGuiScreen)
                && !(screen instanceof HudEditorScreen) && !(screen instanceof AstatinTitleScreen)) {
            return;
        }
        int framebufferWidth = mc.getWindow().getWidth();
        int framebufferHeight = mc.getWindow().getHeight();
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0 || guiWidth <= 0 || guiHeight <= 0) {
            return;
        }
        float guiScale = (float) framebufferWidth / (float) guiWidth;

        SkijaRenderer renderer = AstatinClient.renderer();
        if (!renderer.begin(framebufferWidth, framebufferHeight, guiScale, guiWidth, guiHeight)) {
            return;
        }
        try {
            Render2D r = renderer.ui();
            float dt = FrameClock.delta();
            AstatinClient.zoom().update(dt);

            boolean inGame = mc.level != null && mc.player != null;
            if (screen instanceof AstatinClickGuiScreen gui) {
                gui.renderSkija(r, dt);
            } else if (screen instanceof HudEditorScreen editor) {
                if (inGame) {
                    long start = System.nanoTime();
                    AstatinClient.collectHud(mc);
                    HudData data = AstatinClient.hudData();
                    HudManager hud = AstatinClient.hud();
                    hud.render(r, data, guiWidth, guiHeight);
                    hud.renderEditOverlay(r, data, guiWidth, guiHeight);
                    float micros = (System.nanoTime() - start) / 1000f;
                    AstatinClient.setHudMicros(micros);
                    hud.drawEditorChrome(r, guiWidth, guiHeight, micros, mc.getFps());
                } else {
                    editor.renderSkija(r, dt);
                }
            } else if (screen instanceof AstatinTitleScreen title) {
                title.renderSkija(r, dt);
            } else if (inGame && screen == null && !mc.options.hideGui) {
                drawGameHud(mc, r, guiWidth, guiHeight);
            }
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.error("Astatin overlay failed", t);
            }
        } finally {
            renderer.end();
        }
    }

    private static void drawGameHud(Minecraft mc, Render2D r, int guiWidth, int guiHeight) {
        Module vignette = AstatinClient.modules() == null ? null : AstatinClient.modules().get("vignette");
        if (vignette != null && vignette.enabled) {
            float strength = Math.min(0.5f, Math.max(0f, vignette.number("strength", 0.22f)));
            r.radialGlow(guiWidth * 0.5f, guiHeight * 0.5f,
                    Math.max(guiWidth, guiHeight) * 0.78f,
                    0x00000000, Theme.withAlpha(0x000000, strength));
        }

        long start = System.nanoTime();
        AstatinClient.collectHud(mc);
        AstatinClient.hud().render(r, AstatinClient.hudData(), guiWidth, guiHeight);
        AstatinClient.setHudMicros((System.nanoTime() - start) / 1000f);
    }
}
