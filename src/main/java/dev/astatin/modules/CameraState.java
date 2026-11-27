package dev.astatin.modules;

import net.minecraft.world.phys.AABB;

/** Mutable view position and direction, independent of the player's state. */
public final class CameraState {
    private double x, y, z, previousX, previousY, previousZ;
    private float yaw, pitch;

    public void reset(double x, double y, double z, float yaw, float pitch) {
        this.x = previousX = x;
        this.y = previousY = y;
        this.z = previousZ = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void turn(double deltaX, double deltaY) {
        yaw += (float) deltaX * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) deltaY * 0.15f));
    }

    public void move(int forward, int sideways, int vertical, double speed) {
        previousX = x;
        previousY = y;
        previousZ = z;
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double dx = -Math.sin(yawRadians) * Math.cos(pitchRadians) * forward + Math.cos(yawRadians) * sideways;
        double dy = -Math.sin(pitchRadians) * forward + vertical;
        double dz = Math.cos(yawRadians) * Math.cos(pitchRadians) * forward + Math.sin(yawRadians) * sideways;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 1e-10) {
            double scale = speed / Math.max(1, length);
            x += dx * scale;
            y += dy * scale;
            z += dz * scale;
        }
    }

    public double x(float partialTick) { return previousX + (x - previousX) * partialTick; }
    public double y(float partialTick) { return previousY + (y - previousY) * partialTick; }
    public double z(float partialTick) { return previousZ + (z - previousZ) * partialTick; }
    public boolean outsidePlayer(float partialTick, AABB playerBounds) {
        return !playerBounds.inflate(0.5).contains(x(partialTick), y(partialTick), z(partialTick));
    }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
}
