package dev.astatin.modules;

import dev.astatin.AstatinClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Owns temporary camera state without altering player position or rotation. */
public final class CameraController {
    private final CameraState state = new CameraState();
    private ClientLevel level;
    private LocalPlayer player;
    private boolean freecam;
    private boolean freelook;
    private KeyMapping freelookKey;

    public void setFreelookKey(KeyMapping key) { freelookKey = key; }

    public void update(Minecraft mc) {
        Module free = module("freecam");
        Module look = module("freelook");
        if (player != null && (mc.player != player || mc.level != level)) {
            reset();
            if (free != null && free.enabled) {
                free.enabled = false;
                AstatinClient.markDirty();
            }
        }
        boolean valid = mc.player != null && mc.level != null && mc.getCameraEntity() == mc.player;
        boolean wantsFreecam = valid && free != null && free.enabled;
        if (!valid && free != null && free.enabled) {
            free.enabled = false;
            AstatinClient.markDirty();
        }
        boolean wantsFreelook = valid && !wantsFreecam && mc.screen == null && look != null && look.enabled
                && freelookKey != null && freelookKey.isDown();
        if ((wantsFreecam && !freecam) || (wantsFreelook && !freelook)) {
            Vec3 eye = mc.player.getEyePosition();
            state.reset(eye.x, eye.y, eye.z, mc.player.getYRot(), mc.player.getXRot());
            player = mc.player;
            level = mc.level;
        }
        freecam = wantsFreecam;
        freelook = wantsFreelook;
        if (!freecam && !freelook) {
            player = null;
            level = null;
        }
    }

    public void tick(Minecraft mc) {
        update(mc);
        if (!freecam) return;
        int forward = 0, sideways = 0, vertical = 0;
        if (mc.screen == null) {
            forward = value(mc.options.keyUp) - value(mc.options.keyDown);
            sideways = value(mc.options.keyRight) - value(mc.options.keyLeft);
            vertical = value(mc.options.keyJump) - value(mc.options.keyShift);
        }
        Module free = module("freecam");
        state.move(forward, sideways, vertical, free == null ? 0.25 : free.number("speed", 0.25f));
    }

    private int value(KeyMapping key) { return key.isDown() ? 1 : 0; }

    public boolean turn(Minecraft mc, double deltaX, double deltaY) {
        update(mc);
        if ((freecam || freelook) && mc.screen == null) {
            state.turn(deltaX, deltaY);
            return true;
        }
        return false;
    }

    public void reset() { freecam = false; freelook = false; player = null; level = null; }
    public void disconnect() {
        reset();
        Module free = module("freecam");
        if (free != null && free.enabled) {
            free.enabled = false;
            AstatinClient.markDirty();
        }
    }
    public boolean freecam(Minecraft mc) { update(mc); return freecam; }
    public boolean freecam() { return freecam; }
    public boolean freelook() { return freelook; }
    public CameraState state() { return state; }

    private static Module module(String id) {
        Modules modules = AstatinClient.modules();
        return modules == null ? null : modules.get(id);
    }
}
