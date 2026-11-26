package dev.astatin.modules;

import java.util.ArrayList;
import java.util.List;

public final class Module {
    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    public boolean enabled;
    /** An action module opens a screen instead of toggling. */
    public final boolean action;
    /** When true the module is backed by a HUD element with the same id. */
    public final boolean hudElement;

    public Module(String id, String name, String description, Category category, boolean enabled, boolean hudElement) {
        this(id, name, description, category, enabled, hudElement, false);
    }

    public Module(String id, String name, String description, Category category, boolean enabled,
                  boolean hudElement, boolean action) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabled;
        this.hudElement = hudElement;
        this.action = action;
    }

    public Module add(Setting setting) {
        setting.attach(id);
        settings.add(setting);
        return this;
    }

    public Setting setting(String id) {
        for (Setting setting : settings) {
            if (setting.id.equals(id)) {
                return setting;
            }
        }
        return null;
    }

    public boolean bool(String id, boolean fallback) {
        Setting setting = setting(id);
        return setting == null ? fallback : setting.boolValue;
    }

    public float number(String id, float fallback) {
        Setting setting = setting(id);
        return setting == null ? fallback : setting.value;
    }

    public int color(String id, int fallback) {
        Setting setting = setting(id);
        return setting == null ? fallback : setting.color;
    }
}
