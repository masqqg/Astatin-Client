package dev.astatin.mixin;

import dev.astatin.AstatinClient;
import dev.astatin.title.AstatinTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void astatin$replaceTitle(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen && !(screen instanceof AstatinTitleScreen) && AstatinClient.ready()) {
            ci.cancel();
            Minecraft.getInstance().setScreen(new AstatinTitleScreen());
        }
    }
}
