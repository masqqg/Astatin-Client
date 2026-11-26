package dev.astatin.mixin;

import dev.astatin.util.CpsTracker;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void astatin$trackCps(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) {
            CpsTracker.press(buttonInfo.button());
        }
    }
}
