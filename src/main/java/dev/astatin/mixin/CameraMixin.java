package dev.astatin.mixin;

import dev.astatin.AstatinClient;
import dev.astatin.modules.CameraController;
import dev.astatin.modules.CameraState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Redirect(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float astatin$freelookYaw(Entity entity, float partialTick) {
        CameraController camera = AstatinClient.camera();
        camera.update(Minecraft.getInstance());
        return camera.freelook() ? camera.state().yaw() : entity.getViewYRot(partialTick);
    }

    @Redirect(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float astatin$freelookPitch(Entity entity, float partialTick) {
        CameraController camera = AstatinClient.camera();
        camera.update(Minecraft.getInstance());
        return camera.freelook() ? camera.state().pitch() : entity.getViewXRot(partialTick);
    }

    @Inject(method = "setup", at = @At("RETURN"))
    private void astatin$freecam(Level level, Entity entity, boolean detached, boolean mirrored,
                                 float partialTick, CallbackInfo ci) {
        CameraController camera = AstatinClient.camera();
        if (camera.freecam(Minecraft.getInstance())) {
            CameraState state = camera.state();
            // Hide the player while the free camera intersects their body, then render them at a distance.
            var playerBounds = entity.getBoundingBox().move(entity.getPosition(partialTick).subtract(entity.position()));
            this.detached = state.outsidePlayer(partialTick, playerBounds);
            setRotation(state.yaw(), state.pitch());
            setPosition(state.x(partialTick), state.y(partialTick), state.z(partialTick));
        }
    }
}
