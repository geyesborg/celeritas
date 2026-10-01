package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * RenderGlobal still constructs vanilla's dispatcher, which sizes itself for building chunks
 * (at -Xmx4G: 81 builders, ~850 MB of direct buffers, 8 idle threads). Celeritas builds all
 * terrain, so it runs single-threaded with one builder; a count passed explicitly is kept.
 * Cleanroom moves the constructor body to {@code <init>(I)V}, where -1 means "size from the heap".
 */
@Mixin(ChunkRenderDispatcher.class)
public class ChunkRenderDispatcherMixin {
    @Redirect(method = "<init>(I)V", at = @At(value = "INVOKE", target = "Ljava/lang/Runtime;availableProcessors()I"))
    private int celeritas$noWorkerThreads(Runtime runtime) {
        return 1;
    }

    @Redirect(method = "<init>(I)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I", ordinal = 1))
    private int celeritas$singleRenderBuilder(int value, int min, int max) {
        return 1;
    }
}
