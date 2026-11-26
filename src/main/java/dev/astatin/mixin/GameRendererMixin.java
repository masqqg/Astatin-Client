package dev.astatin.mixin;

import dev.astatin.AstatinClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void astatin$zoom(Camera camera, float partialTick, boolean changingFov,
                              CallbackInfoReturnable<Float> cir) {
        float multiplier = AstatinClient.zoom().fovMultiplier();
        if (multiplier < 0.9995f) {
            cir.setReturnValue(cir.getReturnValue() * multiplier);
        }
    }
}
