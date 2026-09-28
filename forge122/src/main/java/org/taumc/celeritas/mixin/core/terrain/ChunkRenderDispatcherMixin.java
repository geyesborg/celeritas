package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Celeritas builds all terrain, but RenderGlobal.loadRenderers still constructs
 * vanilla's dispatcher, which sizes itself for building chunks: one worker thread
 * per core and one RegionRenderCacheBuilder (10.5 MB of direct buffers) per 10 MB
 * of 30% of the max heap, e.g. 81 builders (~850 MB off-heap) and 8 idle threads
 * with -Xmx4G. It stays functional for any caller, in vanilla's single-threaded
 * mode (no worker threads, updates run inline) with a single builder. Cleanroom's
 * patch moves the constructor body to {@code <init>(I)V} (the no-arg one delegates
 * with -1 = size from the heap); an explicit count passed by a caller is kept.
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
