package dev.astatin.hud;

import dev.astatin.modules.Modules;
import dev.astatin.util.CpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HudCollector {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static long lastSlow;
    private static int lastFps;
    private static int lastPing;

    private HudCollector() {
    }

    public static void collect(Minecraft mc, HudData data, Modules modules) {
        modules.applyTo(data);
        long now = System.currentTimeMillis();

        if (mc.player != null) {
            data.posX = String.valueOf(Mth.floor(mc.player.getX()));
            data.posY = String.valueOf(Mth.floor(mc.player.getY()));
            data.posZ = String.valueOf(Mth.floor(mc.player.getZ()));
            data.facing = facing(mc.player.getDirection());
            if (mc.level != null) {
                String path = mc.level.dimension().identifier().getPath();
                data.dimension = switch (path) {
                    case "the_nether" -> "Nether";
                    case "the_end" -> "End";
                    default -> "Overworld";
                };
            }
        }

        data.cpsLeft = CpsTracker.left();
        data.cpsRight = CpsTracker.right();
        data.keys[0] = mc.options.keyUp.isDown();
        data.keys[1] = mc.options.keyLeft.isDown();
        data.keys[2] = mc.options.keyDown.isDown();
        data.keys[3] = mc.options.keyRight.isDown();
        data.keys[4] = mc.mouseHandler.isLeftPressed();
        data.keys[5] = mc.mouseHandler.isRightPressed();

        if (now - lastSlow < 200L) {
            data.fps = lastFps;
            data.ping = lastPing;
            return;
        }
        lastSlow = now;
        data.fps = mc.getFps();
        lastFps = data.fps;

        if (mc.player != null && mc.getConnection() != null) {
            ClientPacketListener connection = mc.getConnection();
            PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
            data.ping = info == null ? 0 : Math.max(0, info.getLatency());
            lastPing = data.ping;
            data.armorPoints = mc.player.getArmorValue();
            collectArmor(mc, data);
            collectPotions(mc, data);
        } else {
            data.ping = 0;
            lastPing = 0;
            data.armor.clear();
            data.potions.clear();
        }
    }

    private static String facing(Direction direction) {
        return switch (direction) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            case WEST -> "W";
            default -> "?";
        };
    }

    private static void collectArmor(Minecraft mc, HudData data) {
        data.armor.clear();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                data.armor.add(new HudData.ArmorPiece("Empty", 0f, false));
            } else {
                int max = stack.getMaxDamage();
                float durability = max <= 0 ? 1f : 1f - stack.getDamageValue() / (float) max;
                data.armor.add(new HudData.ArmorPiece(stack.getHoverName().getString(), durability, true));
            }
        }
    }

    private static void collectPotions(Minecraft mc, HudData data) {
        data.potions.clear();
        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
        for (MobEffectInstance effect : effects) {
            if (data.potions.size() >= 4) {
                break;
            }
            int color = 0xFF000000 | (effect.getEffect().value().getColor() & 0xFFFFFF);
            int seconds = effect.getDuration() / 20;
            String name = effect.getEffect().value().getDisplayName().getString();
            data.potions.add(new HudData.PotionInfo(name, roman(effect.getAmplifier() + 1), seconds,
                    color, Math.min(1f, seconds / 180f)));
        }
    }

    private static String roman(int value) {
        return switch (value) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> value <= 1 ? "I" : String.valueOf(value);
        };
    }
}
