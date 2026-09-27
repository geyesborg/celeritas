package org.taumc.celeritas.mixin.core.crash;

import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Forge class: its names are not obfuscated, so nothing to remap
@Mixin(targets = {"net/minecraftforge/fml/client/SplashProgress$1"}, remap = false)
public class SplashProgressCallableMixin {
    @Inject(method = "call()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void checkContext(CallbackInfoReturnable<String> cir) {
        boolean isContextAvailable;
        try {
            isContextAvailable = Display.isCreated() && Display.getDrawable().isCurrent();
        } catch (Exception e) {
            isContextAvailable = false;
        }
        if (!isContextAvailable) {
            cir.setReturnValue("No context available");
        }
    }
}
