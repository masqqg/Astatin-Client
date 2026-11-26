package dev.astatin.mixin;

import dev.astatin.AstatinClient;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true)
    private static void astatin$fullbright(float ambientLight, int light, CallbackInfoReturnable<Float> cir) {
        if (AstatinClient.fullbright()) {
            cir.setReturnValue(1.0f);
        }
    }
}
