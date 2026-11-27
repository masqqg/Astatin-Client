package dev.astatin.modules;

import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.AABB;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraStateTest {
    @Test
    void hidesPlayerUntilCameraLeavesExpandedBody() {
        CameraState camera = new CameraState();
        AABB player = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
        camera.reset(0, 1.62, 0, 0, 0);
        assertFalse(camera.outsidePlayer(1, player));
        camera.reset(0, 0.1, 0, 0, 0);
        assertFalse(camera.outsidePlayer(1, player));
        camera.reset(0, 1.62, 0.79, 0, 0);
        assertFalse(camera.outsidePlayer(1, player));
        camera.reset(0, 1.62, 0.81, 0, 0);
        assertTrue(camera.outsidePlayer(1, player));
        camera.reset(0, 1.62, 0, 0, 0);
        camera.move(1, 0, 0, 1);
        assertFalse(camera.outsidePlayer(0.5f, player));
        assertTrue(camera.outsidePlayer(1, player));
    }

    @Test
    void movesRelativeToViewAndInterpolates() {
        CameraState camera = new CameraState();
        camera.reset(10, 20, 30, 0, 0);
        camera.move(1, 0, 0, 0.5);
        assertEquals(10, camera.x(1), 1e-6);
        assertEquals(20, camera.y(1), 1e-6);
        assertEquals(30.25, camera.z(0.5f), 1e-6);
        assertEquals(30.5, camera.z(1), 1e-6);
        camera.reset(0, 0, 0, 90, 0);
        camera.move(1, 0, 0, 1);
        assertEquals(-1, camera.x(1), 1e-6);
        assertEquals(0, camera.z(1), 1e-6);
    }

    @Test
    void diagonalMovementIsNormalizedAndVerticalControlsWork() {
        CameraState camera = new CameraState();
        camera.reset(0, 0, 0, 0, 0);
        camera.move(1, 1, 1, 1);
        assertEquals(1, Math.sqrt(camera.x(1) * camera.x(1) + camera.y(1) * camera.y(1)
                + camera.z(1) * camera.z(1)), 1e-6);
        camera.reset(0, 0, 0, 0, 90);
        camera.move(1, 0, 0, 0.5);
        assertEquals(-0.5, camera.y(1), 1e-6);
        camera.reset(0, 0, 0, 0, 90);
        camera.move(1, 0, 1, 0.5);
        assertEquals(0, camera.x(1), 1e-6);
        assertEquals(0, camera.y(1), 1e-6);
        assertEquals(0, camera.z(1), 1e-6);
    }

    @Test
    void mouseTurnMatchesVanillaScaleAndPitchLimits() {
        CameraState camera = new CameraState();
        camera.reset(0, 0, 0, 30, 0);
        camera.turn(100, 1000);
        assertEquals(45, camera.yaw(), 1e-6);
        assertEquals(90, camera.pitch(), 1e-6);
        camera.turn(0, -2000);
        assertEquals(-90, camera.pitch(), 1e-6);
    }
}
