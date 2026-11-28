package dev.astatin.mixin;

import dev.astatin.AstatinOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the Astatin HUD with Minecraft's own renderer when the Skija overlay is
 * unavailable, so the HUD still appears on Android launchers and other systems
 * the Skija native library does not support.
 */
@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void astatin$fallbackHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        AstatinOverlay.renderFallbackHud(guiGraphics);
    }
}
