package dev.astatin.modules;

import dev.astatin.theme.Theme;

public final class Setting {
    public enum Kind {
        BOOL, SLIDER, COLOR
    }

    public final String id;
    public final String name;
    public final Kind kind;
    public final float min;
    public final float max;
    public final float step;
    public final String unit;

    public boolean boolValue;
    public float value;
    public int color;

    private String key = "";

    private Setting(String id, String name, Kind kind, float min, float max, float step, String unit) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    public static Setting bool(String id, String name, boolean initial) {
        Setting setting = new Setting(id, name, Kind.BOOL, 0f, 1f, 1f, "");
        setting.boolValue = initial;
        return setting;
    }

    public static Setting slider(String id, String name, float initial, float min, float max, float step, String unit) {
        Setting setting = new Setting(id, name, Kind.SLIDER, min, max, step, unit);
        setting.value = initial;
        return setting;
    }

    public static Setting color(String id, String name, int initial) {
        Setting setting = new Setting(id, name, Kind.COLOR, 0f, 1f, 1f, "");
        setting.color = initial;
        return setting;
    }

    public String key() {
        return key;
    }

    void attach(String moduleId) {
        this.key = moduleId + "." + id;
    }

    public float normalized() {
        return max <= min ? 0f : (value - min) / (max - min);
    }

    public void setNormalized(float t) {
        float raw = min + (max - min) * Theme.clamp(t, 0f, 1f);
        float snapped = Math.round(raw / step) * step;
        value = Theme.clamp(snapped, min, max);
    }

    public String displayValue() {
        if (kind == Kind.SLIDER) {
            if (step >= 1f) {
                return String.valueOf(Math.round(value));
            }
            return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0$", "");
        }
        if (kind == Kind.COLOR) {
            return Theme.hex(color);
        }
        return boolValue ? "On" : "Off";
    }
}
