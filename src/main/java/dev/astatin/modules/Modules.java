package dev.astatin.modules;

import dev.astatin.config.ConfigData;
import dev.astatin.hud.HudData;
import dev.astatin.hud.HudElement;
import dev.astatin.hud.HudManager;
import dev.astatin.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single source of truth for every toggle in the client. HUD modules share
 * their enabled flag with the matching HUD element, so the menu, the HUD and
 * the config file can never disagree.
 */
public final class Modules {
    private final List<Module> modules = new ArrayList<>();
    private final HudManager hud;

    public Modules(HudManager hud) {
        this.hud = hud;

        add(new Module("watermark", "Watermark", "Brand block with live FPS and ping readouts.",
                Category.HUD, true, true))
                .add(Setting.bool("fps", "Show FPS", true))
                .add(Setting.bool("ping", "Show ping", true));
        add(new Module("keystrokes", "Keystrokes", "Keys and mouse buttons with pressed-state feedback.",
                Category.HUD, true, true))
                .add(Setting.bool("mouse", "Show mouse buttons", true));
        add(new Module("cps", "CPS", "Clicks per second with a quiet history graph.",
                Category.HUD, true, true))
                .add(Setting.bool("split", "Split left and right", true));
        add(new Module("coords", "Coordinates", "Position, facing and dimension at a glance.",
                Category.HUD, true, true))
                .add(Setting.bool("dimension", "Show dimension", true));
        add(new Module("armor", "Armor", "Equipped armour with live durability bars.",
                Category.HUD, true, true))
                .add(Setting.bool("durability", "Show durability", true));
        add(new Module("potions", "Potions", "Active effects, amplifiers and remaining time.",
                Category.HUD, true, true))
                .add(Setting.bool("duration", "Show duration", true));

        add(new Module("zoom", "Zoom", "Smooth hold-to-zoom with an adjustable factor.",
                Category.VISUALS, true, false))
                .add(Setting.slider("amount", "Zoom amount", 4f, 1.5f, 8f, 0.5f, "x"))
                .add(Setting.slider("smoothness", "Smoothing", 0.55f, 0f, 1f, 0.05f, ""));
        add(new Module("fullbright", "Fullbright", "Lifts the light map for visibility in dark areas.",
                Category.VISUALS, false, false));
        add(new Module("vignette", "Vignette", "A soft cinematic darkening around the edges of the screen.",
                Category.VISUALS, false, false))
                .add(Setting.slider("strength", "Strength", 0.22f, 0f, 0.5f, 0.02f, ""));

        add(new Module("clickgui", "ClickGUI", "Accent, glass and motion for the client menus.",
                Category.INTERFACE, true, false))
                .add(Setting.color("accent", "Accent colour", Theme.AMBER))
                .add(Setting.bool("glass", "Frosted glass", true))
                .add(Setting.bool("animations", "Animations", true));
        add(new Module("hudeditor", "HUD Editor", "Move and resize every HUD element.",
                Category.INTERFACE, false, false, true));

        add(new Module("sprint", "Toggle Sprint", "Keeps you sprinting while moving forward.",
                Category.MISC, false, false));
        syncHud();
    }

    private void syncHud() {
        for (Module module : modules) {
            if (module.hudElement) {
                HudElement element = hud.get(module.id);
                if (element != null) {
                    element.enabled = module.enabled;
                }
            }
        }
    }

    private Module add(Module module) {
        modules.add(module);
        return module;
    }

    public List<Module> all() {
        return modules;
    }

    public Module get(String id) {
        for (Module module : modules) {
            if (module.id.equals(id)) {
                return module;
            }
        }
        return null;
    }

    public List<Module> byCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.category == category) {
                result.add(module);
            }
        }
        return result;
    }

    public List<Module> search(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (needle.isEmpty()
                    || module.name.toLowerCase(Locale.ROOT).contains(needle)
                    || module.description.toLowerCase(Locale.ROOT).contains(needle)
                    || module.category.label.toLowerCase(Locale.ROOT).contains(needle)) {
                result.add(module);
            }
        }
        return result;
    }

    public void setEnabled(Module module, boolean enabled) {
        if (module.action) {
            return;
        }
        module.enabled = enabled;
        if (module.hudElement) {
            HudElement element = hud.get(module.id);
            if (element != null) {
                element.enabled = enabled;
            }
        }
    }

    public void toggle(Module module) {
        setEnabled(module, !module.enabled);
    }

    public void applyConfig(ConfigData config) {
        for (Module module : modules) {
            Boolean enabled = config.enabled.get(module.id);
            if (enabled != null && !module.action) {
                setEnabled(module, enabled);
            }
            for (Setting setting : module.settings) {
                if (setting.kind == Setting.Kind.BOOL) {
                    Boolean value = config.enabled.get(setting.key());
                    if (value != null) {
                        setting.boolValue = value;
                    }
                } else if (setting.kind == Setting.Kind.SLIDER) {
                    Float value = config.numbers.get(setting.key());
                    if (value != null) {
                        setting.value = Theme.clamp(value, setting.min, setting.max);
                    }
                } else {
                    Integer value = config.colors.get(setting.key());
                    if (value != null) {
                        setting.color = value;
                    }
                }
            }
        }
        Module gui = get("clickgui");
        if (gui != null) {
            Theme.setAccent(gui.color("accent", Theme.AMBER));
        }
        syncHud();
        applyTo(new HudData());
    }

    public void store(ConfigData config) {
        for (Module module : modules) {
            if (!module.action) {
                config.enabled.put(module.id, module.enabled);
            }
            for (Setting setting : module.settings) {
                if (setting.kind == Setting.Kind.BOOL) {
                    config.enabled.put(setting.key(), setting.boolValue);
                } else if (setting.kind == Setting.Kind.SLIDER) {
                    config.numbers.put(setting.key(), setting.value);
                } else {
                    config.colors.put(setting.key(), setting.color);
                }
            }
        }
        hud.store(config);
    }

    public void applyTo(HudData data) {
        Module watermark = get("watermark");
        Module keystrokes = get("keystrokes");
        Module coords = get("coords");
        Module armor = get("armor");
        Module potions = get("potions");
        Module cps = get("cps");
        data.showFps = watermark == null || watermark.bool("fps", true);
        data.showPing = watermark == null || watermark.bool("ping", true);
        data.showMouse = keystrokes == null || keystrokes.bool("mouse", true);
        data.showDimension = coords == null || coords.bool("dimension", true);
        data.showDurability = armor == null || armor.bool("durability", true);
        data.showDuration = potions == null || potions.bool("duration", true);
        data.splitCps = cps == null || cps.bool("split", true);
    }

    public boolean glass() {
        Module gui = get("clickgui");
        return gui == null || gui.bool("glass", true);
    }

    public boolean animations() {
        Module gui = get("clickgui");
        return gui == null || gui.bool("animations", true);
    }

    public HudElement element(String id) {
        return hud.get(id);
    }
}
