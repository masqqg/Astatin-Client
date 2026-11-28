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
import dev.astatin.render.Render2D;
import dev.astatin.render.SkijaRenderer;
import dev.astatin.render.UiSurface;
import dev.astatin.render.VanillaBackend;
import dev.astatin.theme.Fonts;
import dev.astatin.title.AstatinTitleScreen;
import dev.astatin.theme.Theme;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    /** Created lazily: referencing Skija types at class-init would fail on Android. */
    private static UiSurface surface;
    private static boolean surfaceTried;

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
    private static boolean hudSized;

    @Override
    public void onInitializeClient() {
        Minecraft mc = Minecraft.getInstance();
        try {
            Fonts.load();
            fontsLoaded = true;
            LOGGER.info("Interface typography: {}", Fonts.source());
        } catch (Throwable t) {
            LOGGER.error("Could not load any interface font", t);
        }

        config = ConfigManager.load();
        hud = new HudManager();
        // Minecraft builds its window *after* client entrypoints run (1.21.11),
        // so getWindow() is still null here. Lay the HUD out on the first tick
        // instead of dereferencing it during startup.
        ensureHudLayout(mc);
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
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            persist();
            UiSurface renderer = renderer();
            if (renderer != null) {
                renderer.close();
            }
        });
        reloadHudDefaults();
        ready = true;
        LOGGER.info("Astatin Client initialized");
    }

    private static void reloadHudDefaults() {
        Minecraft mc = Minecraft.getInstance();
        ensureHudLayout(mc);
        if (hudSized && modules != null) {
            modules.applyConfig(config);
        }
    }

    /**
     * Seeds the default HUD layout once a real window exists. Safe to call any
     * number of times before that; it simply does nothing.
     */
    private static void ensureHudLayout(Minecraft mc) {
        if (hudSized || hud == null || mc == null) {
            return;
        }
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        if (window == null) {
            return;
        }
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        hudSized = true;
        hud.ensureDefaults(width, height, config);
    }

    private static void tick(Minecraft mc) {
        if (!ready) {
            return;
        }
        if (!hudSized) {
            ensureHudLayout(mc);
        }
        while (menuKey != null && menuKey.consumeClick()) {
            if (mc.screen == null) {
                if (uiAvailable()) {
                    mc.setScreen(new AstatinClickGuiScreen());
                }
            } else if (mc.screen instanceof AstatinClickGuiScreen || mc.screen instanceof HudEditorScreen) {
                mc.setScreen(null);
            }
        }
        while (editorKey != null && editorKey.consumeClick()) {
            if (mc.screen == null && uiAvailable()) {
                mc.setScreen(new HudEditorScreen());
            }
        }

        // If Skija or its natives failed, our screens would be invisible while
        // still eating input. Give the player back the vanilla interface.
        if (!uiAvailable() && (mc.screen instanceof AstatinClickGuiScreen
                || mc.screen instanceof HudEditorScreen || mc.screen instanceof AstatinTitleScreen)) {
            mc.setScreen(null);
            return;
        }

        // Covers a very early title screen created before the client initializer ran.
        if (uiAvailable() && mc.screen instanceof TitleScreen && !(mc.screen instanceof AstatinTitleScreen)) {
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

    /**
     * True when the custom interface can be drawn. Both renderers qualify: the
     * Skija one when its native library loads and Minecraft's own renderer
     * otherwise, which is what keeps the HUD and menu working on Android.
     */
    public static boolean uiAvailable() {
        return ready();
    }

    /** True only while the Skija surface is the one doing the drawing. */
    public static boolean skijaActive() {
        UiSurface renderer = renderer();
        return renderer != null && !renderer.isDisabled();
    }

    /** Starts a frame that draws through Minecraft's GuiGraphics. */
    public static Render2D beginVanilla(GuiGraphics guiGraphics) {
        Render2D ui = new Render2D(new VanillaBackend(guiGraphics));
        ui.beginFrame(guiGraphics.guiWidth(), guiGraphics.guiHeight());
        return ui;
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

    /**
     * The Skija surface, or null when it cannot exist on this platform. The
     * construction is wrapped because a missing native shows up as an Error.
     */
    public static UiSurface renderer() {
        if (!surfaceTried) {
            surfaceTried = true;
            // Lets the Minecraft fallback be exercised on a desktop JVM, e.g.
            // -Dastatin.vanilla=true, without pulling the native library out.
            if (Boolean.getBoolean("astatin.vanilla")) {
                LOGGER.info("Astatin renderer disabled on request; using Minecraft's own renderer");
                return null;
            }
            try {
                surface = new SkijaRenderer();
            } catch (Throwable t) {
                surface = null;
                LOGGER.warn("Skija is unavailable ({} / {}); using Minecraft's renderer instead",
                        System.getProperty("os.name"), System.getProperty("os.arch"));
            }
        }
        return surface;
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
