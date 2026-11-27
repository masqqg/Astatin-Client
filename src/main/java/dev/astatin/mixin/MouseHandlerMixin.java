package dev.astatin.mixin;

import dev.astatin.util.CpsTracker;
import dev.astatin.AstatinClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void astatin$turnCamera(LocalPlayer player, double dx, double dy) {
        if (!AstatinClient.camera().turn(Minecraft.getInstance(), dx, dy)) {
            player.turn(dx, dy);
        }
    }
    @Inject(method = "onButton", at = @At("HEAD"))
    private void astatin$trackCps(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) {
            CpsTracker.press(buttonInfo.button());
        }
    }
}
