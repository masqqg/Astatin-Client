package dev.astatin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.astatin.config.ConfigData;
import dev.astatin.config.ConfigManager;
import dev.astatin.hud.HudCollector;
import dev.astatin.hud.HudData;
import dev.astatin.hud.HudManager;
import dev.astatin.menu.AstatinClickGuiScreen;
import dev.astatin.menu.HudEditorScreen;
import dev.astatin.modules.Module;
import dev.astatin.modules.Modules;
import dev.astatin.modules.ZoomState;
import dev.astatin.render.SkijaRenderer;
import dev.astatin.theme.Fonts;
import dev.astatin.title.AstatinTitleScreen;
import dev.astatin.theme.Theme;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AstatinClient implements ClientModInitializer {
    public static final String MOD_ID = "astatin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final HudData HUD_DATA = new HudData();
    private static final ZoomState ZOOM = new ZoomState();
    private static final SkijaRenderer RENDERER = new SkijaRenderer();

    private static ConfigData config = new ConfigData();
    private static HudManager hud;
    private static Modules modules;
    private static KeyMapping menuKey;
    private static KeyMapping zoomKey;
    private static KeyMapping editorKey;
    private static boolean ready;
    private static boolean fullbright;
    private static boolean dirty;
    private static int dirtyTicks;
    private static float hudMicros;
    private static boolean fontsLoaded;

    @Override
    public void onInitializeClient() {
        Minecraft mc = Minecraft.getInstance();
        try {
            Fonts.load();
            fontsLoaded = true;
        } catch (Throwable t) {
            LOGGER.error("Could not load the bundled interface font", t);
        }

        config = ConfigManager.load();
        hud = new HudManager();
        hud.ensureDefaults(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), config);
        modules = new Modules(hud);
        modules.applyConfig(config);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.astatin.menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.astatin.zoom",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
        editorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.astatin.hudeditor",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> persist());
        reloadHudDefaults();
        ready = true;
        LOGGER.info("Astatin Client initialized");
    }

    private static void reloadHudDefaults() {
        Minecraft mc = Minecraft.getInstance();
        if (hud != null) {
            hud.ensureDefaults(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), config);
        }
        if (modules != null) {
            modules.applyConfig(config);
        }
    }

    private static void tick(Minecraft mc) {
        if (!ready) {
            return;
        }
        while (menuKey != null && menuKey.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new AstatinClickGuiScreen());
            } else if (mc.screen instanceof AstatinClickGuiScreen || mc.screen instanceof HudEditorScreen) {
                mc.setScreen(null);
            }
        }
        while (editorKey != null && editorKey.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new HudEditorScreen());
            }
        }

        // Covers a very early title screen created before the client initializer ran.
        if (mc.screen instanceof TitleScreen && !(mc.screen instanceof AstatinTitleScreen)) {
            mc.setScreen(new AstatinTitleScreen());
        }

        Module zoom = module("zoom");
        if (zoom != null) {
            boolean held = zoom.enabled && zoomKey != null && zoomKey.isDown()
                    && mc.screen == null && mc.level != null && !mc.options.hideGui;
            ZOOM.hold(held, zoom.number("amount", 4f), zoom.number("smoothness", 0.55f));
        }

        Module fullbrightModule = module("fullbright");
        fullbright = fullbrightModule != null && fullbrightModule.enabled;

        Module sprint = module("sprint");
        if (sprint != null && sprint.enabled && mc.player != null && mc.screen == null
                && mc.player.input.hasForwardImpulse() && !mc.player.isShiftKeyDown()) {
            mc.player.setSprinting(true);
        }

        if (dirty) {
            dirtyTicks++;
            if (dirtyTicks >= 40) {
                persist();
            }
        }
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void persist() {
        if (modules == null || hud == null || config == null) {
            return;
        }
        modules.store(config);
        config.accent = Theme.hex(Theme.accent());
        ConfigManager.save(config);
        dirty = false;
        dirtyTicks = 0;
    }

    public static void collectHud(Minecraft mc) {
        if (modules != null) {
            HudCollector.collect(mc, HUD_DATA, modules);
        }
    }

    private static Module module(String id) {
        return modules == null ? null : modules.get(id);
    }

    public static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    public static boolean ready() {
        return ready && fontsLoaded;
    }

    public static ConfigData config() {
        return config;
    }

    public static HudManager hud() {
        return hud;
    }

    public static Modules modules() {
        return modules;
    }

    public static HudData hudData() {
        return HUD_DATA;
    }

    public static ZoomState zoom() {
        return ZOOM;
    }

    public static SkijaRenderer renderer() {
        return RENDERER;
    }

    public static boolean fullbright() {
        return fullbright;
    }

    public static float hudMicros() {
        return hudMicros;
    }

    public static void setHudMicros(float micros) {
        hudMicros = micros;
    }
}
