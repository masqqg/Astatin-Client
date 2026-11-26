package dev.astatin.mixin;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.Window;
import dev.astatin.AstatinOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "updateDisplay", at = @At("HEAD"))
    private void astatin$draw(TracyFrameCapture capture, CallbackInfo ci) {
        AstatinOverlay.render();
    }
}
