package org.taumc.celeritas.mixin.core.terrain;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.taumc.celeritas.impl.render.entity.EntityChunkTracker;

/** Client chunk entity-list changes invalidate the entity gatherer's chunk list. */
@Mixin(Chunk.class)
public abstract class ChunkEntityTrackingMixin {
    @Shadow
    @Final
    private World world;

    @Inject(method = "addEntity", at = @At("TAIL"))
    private void celeritas$afterAddEntity(Entity entity, CallbackInfo ci) {
        if (this.world.isRemote) {
            EntityChunkTracker.markChanged();
        }
    }

    @Inject(method = "removeEntityAtIndex", at = @At("TAIL"))
    private void celeritas$afterRemoveEntity(Entity entity, int index, CallbackInfo ci) {
        if (this.world.isRemote) {
            EntityChunkTracker.markChanged();
        }
    }
}