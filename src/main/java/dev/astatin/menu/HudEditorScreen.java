package dev.astatin.menu;

import dev.astatin.AstatinClient;
import dev.astatin.hud.HudData;
import dev.astatin.hud.HudElement;
import dev.astatin.hud.HudManager;
import dev.astatin.render.Render2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HudEditorScreen extends Screen {
    private HudElement dragging;
    private HudElement resizing;
    private float grabX;
    private float grabY;
    private double lastMouseY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void renderSkija(Render2D r, float dt) {
        // Only reached when there is no world; draw a simple editor background.
        r.rect(0f, 0f, r.width(), r.height(), 0xCC07080A);
        r.textCenteredTop("HUD Editor", r.width() * 0.5f, r.height() * 0.45f,
                dev.astatin.theme.Fonts.semibold(16f), dev.astatin.theme.Theme.TEXT);
        r.textCenteredTop("Join a world to edit your HUD", r.width() * 0.5f, r.height() * 0.45f + 26f,
                dev.astatin.theme.Fonts.regular(9f), dev.astatin.theme.Theme.TEXT_MUTED);
        HudManager.drawEditorClose(r, r.width());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Positions are read directly from the handler while dragging.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && HudManager.editorClose(width).contains(event.x(), event.y())) {
            onClose();
            return true;
        }
        HudManager hud = AstatinClient.hud();
        HudData data = AstatinClient.hudData();
        if (hud == null) {
            return false;
        }
        float mouseX = (float) event.x();
        float mouseY = (float) event.y();
        if (event.button() == 1) {
            HudElement element = hud.at(mouseX, mouseY, data, width, height, false);
            if (element != null) {
                hud.reset(element, width, height, AstatinClient.config());
                AstatinClient.markDirty();
                return true;
            }
            return false;
        }
        if (event.button() == 0) {
            HudElement handle = hud.at(mouseX, mouseY, data, width, height, true);
            if (handle != null) {
                resizing = handle;
                lastMouseY = event.y();
                return true;
            }
            HudElement element = hud.at(mouseX, mouseY, data, width, height, false);
            if (element != null) {
                dragging = element;
                grabX = mouseX - element.xOnScreen(width, data);
                grabY = mouseY - element.yOnScreen(height, data);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        HudManager hud = AstatinClient.hud();
        if (hud == null) {
            return false;
        }
        HudData data = AstatinClient.hudData();
        if (dragging != null) {
            hud.drag(dragging, (float) event.x(), (float) event.y(), grabX, grabY, data, width, height);
            return true;
        }
        if (resizing != null) {
            float delta = (float) ((event.y() - lastMouseY) * 0.006);
            hud.resize(resizing, delta, width, height);
            lastMouseY = event.y();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null || resizing != null) {
            dragging = null;
            resizing = null;
            AstatinClient.markDirty();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HudManager hud = AstatinClient.hud();
        if (hud != null) {
            HudElement element = hud.at((float) mouseX, (float) mouseY, AstatinClient.hudData(),
                    width, height, false);
            if (element != null) {
                hud.resize(element, (float) scrollY * 0.05f, width, height);
                AstatinClient.markDirty();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_H || key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        AstatinClient.persist();
        super.onClose();
    }

    @Override
    public void removed() {
        AstatinClient.persist();
        super.removed();
    }

}
