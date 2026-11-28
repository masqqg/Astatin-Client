package dev.astatin.title;

import dev.astatin.AstatinOverlay;
import dev.astatin.render.Render2D;
import dev.astatin.util.FrameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class AstatinTitleScreen extends Screen {
    private final TitleRenderer renderer = new TitleRenderer();
    private final TitleState state = new TitleState();

    public AstatinTitleScreen() {
        super(Component.literal("Astatin"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        AstatinOverlay.renderFallbackScreen(this, guiGraphics, FrameClock.delta());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void renderSkija(Render2D r, float dt) {
        Minecraft mc = Minecraft.getInstance();
        state.mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        state.mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        state.dt = dt;
        state.time += dt;
        renderer.update(state, width, height);
        renderer.render(r, state);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        state.mouseX = (int) mouseX;
        state.mouseY = (int) mouseY;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < TitleRenderer.BUTTONS.length; i++) {
            if (!renderer.button(i, width, height).contains(event.x(), event.y())) {
                continue;
            }
            switch (i) {
                case 0 -> mc.setScreen(new SelectWorldScreen(this));
                case 1 -> mc.setScreen(new JoinMultiplayerScreen(this));
                case 2 -> mc.setScreen(new OptionsScreen(this, mc.options));
                case 3 -> mc.stop();
                default -> {
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
