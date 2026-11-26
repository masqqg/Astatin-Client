package dev.astatin.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the HUD needs, with no Minecraft types. The game fills this in
 * once per frame; the preview harness fills it with sample values. Both end up
 * drawing through exactly the same code.
 */
public final class HudData {
    public int fps = 144;
    public int ping = 12;
    public int cpsLeft = 7;
    public int cpsRight = 4;

    public String posX = "1248";
    public String posY = "72";
    public String posZ = "-3096";
    public String dimension = "Overworld";
    public String facing = "SE";

    public int armorPoints = 17;
    public final List<ArmorPiece> armor = new ArrayList<>();
    public final List<PotionInfo> potions = new ArrayList<>();

    /** W, A, S, D, LMB, RMB. */
    public final boolean[] keys = new boolean[6];
    public final float[] cpsHistory = new float[14];

    // display flags driven by module settings
    public boolean showFps = true;
    public boolean showPing = true;
    public boolean showMouse = true;
    public boolean showDimension = true;
    public boolean showDurability = true;
    public boolean showDuration = true;
    public boolean splitCps = true;

    public HudData sample() {
        armor.clear();
        armor.add(new ArmorPiece("Netherite", 0.92f, true));
        armor.add(new ArmorPiece("Netherite", 0.74f, true));
        armor.add(new ArmorPiece("Diamond", 0.58f, true));
        armor.add(new ArmorPiece("Netherite", 0.86f, true));

        potions.clear();
        potions.add(new PotionInfo("Speed", "II", 83, 0x7DE7FF, 0.78f));
        potions.add(new PotionInfo("Fire Resistance", "I", 152, 0xFFB347, 0.62f));
        potions.add(new PotionInfo("Night Vision", "I", 214, 0x8A7DFF, 0.44f));

        keys[0] = true;
        keys[1] = false;
        keys[2] = true;
        keys[3] = false;
        keys[4] = false;
        keys[5] = false;

        for (int i = 0; i < cpsHistory.length; i++) {
            cpsHistory[i] = 0.3f + 0.7f * Math.abs((float) Math.sin(i * 0.9f));
        }
        return this;
    }

    public static final class ArmorPiece {
        public String name;
        public float durability;
        public boolean present;

        public ArmorPiece() {
        }

        public ArmorPiece(String name, float durability, boolean present) {
            this.name = name;
            this.durability = durability;
            this.present = present;
        }
    }

    public static final class PotionInfo {
        public String name;
        public String level;
        public int seconds;
        public int color;
        public float fraction;

        public PotionInfo() {
        }

        public PotionInfo(String name, String level, int seconds, int color, float fraction) {
            this.name = name;
            this.level = level;
            this.seconds = seconds;
            this.color = color;
            this.fraction = fraction;
        }

        public String duration() {
            int minutes = seconds / 60;
            int rest = seconds % 60;
            return String.format("%d:%02d", minutes, rest);
        }
    }
}
