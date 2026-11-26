package dev.astatin.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain Gson model. Keeping it free of Minecraft types means it can be parsed
 * and written without a running client, and makes migrations simple.
 */
public final class ConfigData {
    public int version = 1;
    public String accent = "#FFA23A";
    public boolean glass = true;
    public boolean animations = true;
    public float menuScale = 1.0f;
    public List<HudState> hud = new ArrayList<>();
    public Map<String, Boolean> enabled = new LinkedHashMap<>();
    public Map<String, Float> numbers = new LinkedHashMap<>();
    public Map<String, Integer> colors = new LinkedHashMap<>();

    public static final class HudState {
        public String id = "";
        public boolean enabled = true;
        public float x = 0f;
        public float y = 0f;
        public float scale = 1f;

        public HudState() {
        }

        public HudState(String id, boolean enabled, float x, float y, float scale) {
            this.id = id;
            this.enabled = enabled;
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }

    public HudState hud(String id) {
        for (HudState state : hud) {
            if (state.id.equals(id)) {
                return state;
            }
        }
        return null;
    }
}
