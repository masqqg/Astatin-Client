package dev.astatin.menu;

import dev.astatin.AstatinClient;
import dev.astatin.modules.Category;
import dev.astatin.modules.Module;
import dev.astatin.modules.Modules;
import dev.astatin.modules.Setting;
import dev.astatin.render.Render2D;
import dev.astatin.theme.ColorUtil;
import dev.astatin.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class AstatinClickGuiScreen extends Screen {
    private final MenuRenderer renderer;
    private final MenuState state = new MenuState();
    private Setting draggingSlider;
    private Setting draggingColor;
    private boolean draggingHue;
    private boolean draggingSquare;

    public AstatinClickGuiScreen() {
        super(Component.literal("Astatin"));
        Modules modules = AstatinClient.modules();
        this.renderer = new MenuRenderer(modules);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Skija paints the whole screen after Minecraft has finished its frame.
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: the client draws its own backdrop.
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
        if (mc.getUser() != null && mc.getUser().getName() != null) {
            state.playerName = mc.getUser().getName();
        } else if (mc.player != null) {
            state.playerName = mc.player.getGameProfile().name();
        }
        MenuLayout layout = new MenuLayout(width, height);
        List<Module> visible = renderer.visible(state);
        state.scroll = Math.max(0f, Math.min(state.scroll, layout.maxScroll(visible.size())));
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
        state.mouseX = (int) event.x();
        state.mouseY = (int) event.y();
        MenuLayout layout = new MenuLayout(width, height);
        Modules modules = AstatinClient.modules();

        if (event.button() == 0) {
            if (layout.close.contains(event.x(), event.y())) {
                onClose();
                return true;
            }
            if (layout.search.contains(event.x(), event.y())) {
                state.searchFocused = true;
                state.openPicker = null;
                return true;
            }
            if (state.category == Category.HUD && !layout.compact
                    && layout.edit.contains(event.x(), event.y())) {
                AstatinClient.persist();
                Minecraft.getInstance().setScreen(new HudEditorScreen());
                return true;
            }
            for (Category category : Category.values()) {
                Frame row = layout.category(category.ordinal());
                if (row.contains(event.x(), event.y())) {
                    state.category = category;
                    state.selected = null;
                    state.openPicker = null;
                    state.search = "";
                    state.searchFocused = false;
                    state.scroll = 0f;
                    return true;
                }
            }

            List<Module> visible = renderer.visible(state);
            for (int i = 0; i < visible.size(); i++) {
                Module module = visible.get(i);
                Frame card = layout.card(i, visible.size(), state.scroll);
                if (card.bottom() > layout.grid.bottom() + 1f || card.y() < layout.grid.y() - 1f) {
                    continue;
                }
                if (layout.cardToggle(card).contains(event.x(), event.y())) {
                    modules.toggle(module);
                    AstatinClient.markDirty();
                    return true;
                }
                if (card.contains(event.x(), event.y())) {
                    if (module.action) {
                        AstatinClient.persist();
                        Minecraft.getInstance().setScreen(new HudEditorScreen());
                    } else {
                        state.selected = module;
                        state.openPicker = null;
                    }
                    return true;
                }
            }

            if (state.selected != null) {
                Module module = state.selected;
                if (layout.inspectorClose().contains(event.x(), event.y())) {
                    state.selected = null;
                    state.openPicker = null;
                    return true;
                }
                if (module.action && layout.inspectorAction().contains(event.x(), event.y())) {
                    AstatinClient.persist();
                    Minecraft.getInstance().setScreen(new HudEditorScreen());
                    return true;
                }
                if (!module.action && layout.inspectorToggle().contains(event.x(), event.y())) {
                    modules.toggle(module);
                    AstatinClient.markDirty();
                    return true;
                }
                for (int i = 0; i < module.settings.size(); i++) {
                    Setting setting = module.settings.get(i);
                    Frame row = layout.settingRow(module, i, state.openPicker);
                    if (!row.contains(event.x(), event.y())) {
                        continue;
                    }
                    if (setting.kind == Setting.Kind.BOOL) {
                        setting.boolValue = !setting.boolValue;
                        AstatinClient.markDirty();
                        return true;
                    }
                    if (setting.kind == Setting.Kind.SLIDER) {
                        draggingSlider = setting;
                        applySlider(module, setting, event.x(), layout);
                        return true;
                    }
                    if (setting.kind == Setting.Kind.COLOR) {
                        if (state.openPicker == setting) {
                            Frame picker = layout.colorPicker(row);
                            Frame square = layout.pickerSquare(picker);
                            Frame hue = layout.pickerHue(picker);
                            if (square.contains(event.x(), event.y())) {
                                draggingColor = setting;
                                draggingSquare = true;
                                applyPicker(setting, event.x(), event.y(), square, hue);
                                return true;
                            }
                            if (hue.contains(event.x(), event.y())) {
                                draggingColor = setting;
                                draggingHue = true;
                                applyPicker(setting, event.x(), event.y(), square, hue);
                                return true;
                            }
                            state.openPicker = null;
                        } else {
                            state.openPicker(setting);
                        }
                        return true;
                    }
                }
                if (!layout.inspector.contains(event.x(), event.y())) {
                    state.selected = null;
                    state.openPicker = null;
                    return true;
                }
            }

            if (state.searchFocused && !layout.search.contains(event.x(), event.y())) {
                state.searchFocused = false;
            }
            if (state.openPicker != null) {
                state.openPicker = null;
            }
            return false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSlider != null || draggingColor != null) {
            draggingSlider = null;
            draggingColor = null;
            draggingHue = false;
            draggingSquare = false;
            AstatinClient.markDirty();
            AstatinClient.persist();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MenuLayout layout = new MenuLayout(width, height);
        if (draggingSlider != null && state.selected != null) {
            applySlider(state.selected, draggingSlider, event.x(), layout);
            return true;
        }
        if (draggingColor != null && state.selected != null) {
            int index = state.selected.settings.indexOf(draggingColor);
            if (index >= 0) {
                Frame row = layout.settingRow(state.selected, index, state.openPicker);
                Frame picker = layout.colorPicker(row);
                applyPicker(draggingColor, event.x(), event.y(), layout.pickerSquare(picker), layout.pickerHue(picker));
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private void applySlider(Module module, Setting setting, double mouseX, MenuLayout layout) {
        int index = module.settings.indexOf(setting);
        if (index < 0) {
            return;
        }
        Frame track = layout.sliderTrack(layout.settingRow(module, index, state.openPicker));
        float t = (float) ((mouseX - track.x()) / Math.max(1f, track.w()));
        setting.setNormalized(t);
        if (setting.key().equals("clickgui.accent")) {
            Theme.setAccent(setting.color);
        }
        AstatinClient.markDirty();
    }

    private void applyPicker(Setting setting, double mouseX, double mouseY, Frame square, Frame hue) {
        if (draggingHue || (mouseX >= hue.x() - 4 && mouseX <= hue.right() + 4
                && mouseY >= hue.y() && mouseY <= hue.bottom())) {
            state.pickerHue = (float) ((mouseY - hue.y()) / Math.max(1f, hue.h())) * 360f;
        }
        if (draggingSquare || (mouseX >= square.x() && mouseX <= square.right()
                && mouseY >= square.y() && mouseY <= square.bottom())) {
            state.pickerSat = (float) ((mouseX - square.x()) / Math.max(1f, square.w()));
            state.pickerVal = 1f - (float) ((mouseY - square.y()) / Math.max(1f, square.h()));
            state.pickerSat = Math.max(0f, Math.min(1f, state.pickerSat));
            state.pickerVal = Math.max(0f, Math.min(1f, state.pickerVal));
        }
        setting.color = ColorUtil.hsv(state.pickerHue, state.pickerSat, state.pickerVal);
        if (setting.key().equals("clickgui.accent")) {
            Theme.setAccent(setting.color);
        }
        AstatinClient.markDirty();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        MenuLayout layout = new MenuLayout(width, height);
        if (layout.grid.contains(mouseX, mouseY)) {
            List<Module> visible = renderer.visible(state);
            state.scroll = Math.max(0f, Math.min(state.scroll - (float) scrollY * 28f,
                    layout.maxScroll(visible.size())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (state.openPicker != null) {
                state.openPicker = null;
                return true;
            }
            if (state.searchFocused) {
                state.searchFocused = false;
                return true;
            }
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        if (state.searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!state.search.isEmpty()) {
                    state.search = state.search.substring(0, state.search.length() - 1);
                    state.selected = null;
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                state.searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (state.searchFocused) {
            int codepoint = event.codepoint();
            if (codepoint >= 32 && codepoint < 127 && state.search.length() < 24) {
                state.search += (char) codepoint;
                state.selected = null;
            }
            return true;
        }
        return super.charTyped(event);
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
