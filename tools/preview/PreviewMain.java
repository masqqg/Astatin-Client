import dev.astatin.config.ConfigData;
import dev.astatin.hud.HudData;
import dev.astatin.hud.HudManager;
import dev.astatin.menu.Frame;
import dev.astatin.menu.MenuLayout;
import dev.astatin.menu.MenuRenderer;
import dev.astatin.menu.MenuState;
import dev.astatin.modules.Category;
import dev.astatin.modules.Module;
import dev.astatin.modules.Modules;
import dev.astatin.render.Render2D;
import dev.astatin.render.SkiaBackend;
import dev.astatin.theme.Theme;
import dev.astatin.title.TitleRenderer;
import dev.astatin.title.TitleState;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Offline capture harness. It runs the production UI code against a raster
 * Skija surface (no Minecraft, no GPU) so the visuals can be inspected on a
 * machine that cannot launch the game.
 */
public final class PreviewMain {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final float GUI_W = 960f;
    private static final float GUI_H = 540f;

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "preview");
        Files.createDirectories(out);

        hud(out.resolve("hud.png"));
        menu(out.resolve("menu.png"), false);
        menu(out.resolve("menu-settings.png"), true);
        title(out.resolve("title.png"));
        System.out.println("wrote " + out.toAbsolutePath());
    }

    private static Surface surface() {
        return Surface.makeRasterN32Premul(WIDTH, HEIGHT);
    }

    private static void save(Surface surface, Path path) throws Exception {
        Image image = surface.makeImageSnapshot();
        Data data = image.encodeToData(EncodedImageFormat.PNG, 100);
        Files.write(path, data.getBytes());
        data.close();
        image.close();
        surface.close();
    }

    private static void background(Render2D r, String path) {
        r.imageCover(path, 0f, 0f, GUI_W, GUI_H);
    }

    private static void hud(Path path) throws Exception {
        Surface surface = surface();
        Canvas canvas = surface.getCanvas();
        canvas.scale(2f, 2f);
        Render2D r = new Render2D(new SkiaBackend(canvas));
        r.beginFrame(GUI_W, GUI_H);

        background(r, "/assets/astatin/textures/gui/scene.jpg");
        r.rect(0f, 0f, GUI_W, GUI_H, 0x14000000);

        HudData data = new HudData();
        HudManager hud = new HudManager();
        ConfigData config = new ConfigData();
        hud.ensureDefaults(GUI_W, GUI_H, config);
        Modules modules = new Modules(hud);
        modules.applyConfig(config);
        modules.applyTo(data);
        data.sample();

        hud.render(r, data, GUI_W, GUI_H);
        r.endFrame();
        save(surface, path);
    }

    private static void menu(Path path, boolean settings) throws Exception {
        Surface surface = surface();
        Canvas canvas = surface.getCanvas();
        canvas.scale(2f, 2f);
        Render2D r = new Render2D(new SkiaBackend(canvas));
        r.beginFrame(GUI_W, GUI_H);

        background(r, "/assets/astatin/textures/gui/scene.jpg");
        r.rect(0f, 0f, GUI_W, GUI_H, 0x66000000);

        HudManager hud = new HudManager();
        ConfigData config = new ConfigData();
        hud.ensureDefaults(GUI_W, GUI_H, config);
        Modules modules = new Modules(hud);
        modules.applyConfig(config);

        MenuRenderer renderer = new MenuRenderer(modules);
        MenuState state = new MenuState();
        state.time = 3.2f;
        state.dt = 1f / 60f;
        state.category = settings ? Category.INTERFACE : Category.HUD;
        if (settings) {
            Module gui = modules.get("clickgui");
            state.selected = gui;
            state.openPicker(gui.setting("accent"));
            MenuLayout layout = new MenuLayout(GUI_W, GUI_H);
            Frame swatch = layout.colorSwatch(layout.settingRow(gui, 0, state.openPicker));
            state.mouseX = Math.round(swatch.x() + 4f);
            state.mouseY = Math.round(swatch.y() + 8f);
        } else {
            MenuLayout layout = new MenuLayout(GUI_W, GUI_H);
            java.util.List<Module> visible = modules.byCategory(state.category);
            int index = 3; // Coordinates
            Frame card = layout.card(index, visible.size(), 0f);
            state.mouseX = Math.round(card.x() + card.w() * 0.42f);
            state.mouseY = Math.round(card.y() + 56f);
        }
        renderer.snap(state, GUI_W, GUI_H);
        renderer.render(r, state);
        r.endFrame();
        save(surface, path);
    }

    private static void title(Path path) throws Exception {
        Surface surface = surface();
        Canvas canvas = surface.getCanvas();
        canvas.scale(2f, 2f);
        Render2D r = new Render2D(new SkiaBackend(canvas));
        r.beginFrame(GUI_W, GUI_H);

        TitleRenderer renderer = new TitleRenderer();
        TitleState state = new TitleState();
        state.time = 2.4f;
        Frame button = renderer.button(0, GUI_W, GUI_H);
        state.mouseX = Math.round(button.x() + button.w() * 0.45f);
        state.mouseY = Math.round(button.centerY());
        renderer.snap(state, GUI_W, GUI_H);
        renderer.render(r, state);
        r.endFrame();
        save(surface, path);
    }

    private PreviewMain() {
    }
}
