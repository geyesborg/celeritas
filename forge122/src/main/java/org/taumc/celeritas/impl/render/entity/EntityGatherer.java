package org.taumc.celeritas.impl.render.entity;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import org.taumc.celeritas.mixin.core.terrain.ChunkAccessor;
import org.taumc.celeritas.mixin.core.terrain.ChunkProviderClientAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EntityGatherer {
    public static final int NUM_PASSES = 2;

    private final List<Entity>[] entityLists;
    private final Consumer<Entity> addEntity;

    // Loaded chunks with entities, in loaded-chunk map order; rebuilt when the map or EntityChunkTracker changes
    private final List<Chunk> entityChunks = new ArrayList<>();
    private Object cachedChunkMap;
    private int cachedGeneration;
    // -Dceleritas.verifyEntityChunks=true: compare the cached list with a full scan every frame
    private static final boolean VERIFY = Boolean.getBoolean("celeritas.verifyEntityChunks");
    private int verifyFailures;

    private static void collectEntityChunks(Iterable<Chunk> loadedChunks, List<Chunk> out) {
        out.clear();
        for (Chunk chunk : loadedChunks) {
            if (((ChunkAccessor)chunk).celeritas$getHasEntities()) {
                out.add(chunk);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public EntityGatherer() {
        this.entityLists = new List[NUM_PASSES];
        for (int i = 0; i < NUM_PASSES; i++) {
            this.entityLists[i] = new ArrayList<>();
        }
        var entityLists = this.entityLists;
        this.addEntity = entity -> {
            for (int i = 0; i < NUM_PASSES; i++) {
                if (entity.shouldRenderInPass(i)) {
                    entityLists[i].add(entity);
                }
            }
        };
    }

    public void clear() {
        for (int i = 0; i < NUM_PASSES; i++) {
            entityLists[i].clear();
        }
    }

    public List<Entity>[] getLoadedEntityList(WorldClient world) {
        Consumer<Entity> addEntity = this.addEntity;
        // Iterate directly over chunk entity lists where possible - mods may create multipart entities that are not
        // added to the main loadedEntityList.
        if (world.getChunkProvider() instanceof ChunkProviderClientAccessor provider) {
            var loadedChunks = provider.celeritas$getLoadedChunks();
            // Scanning every loaded chunk each frame is costly at high render distances;
            // the entity-bearing chunks only change with entity/chunk events.
            if (loadedChunks != this.cachedChunkMap || EntityChunkTracker.generation() != this.cachedGeneration) {
                this.cachedChunkMap = loadedChunks;
                this.cachedGeneration = EntityChunkTracker.generation();
                collectEntityChunks(loadedChunks.values(), this.entityChunks);
            } else if (VERIFY) {
                List<Chunk> fresh = new ArrayList<>();
                collectEntityChunks(loadedChunks.values(), fresh);
                if (!fresh.equals(this.entityChunks) && this.verifyFailures++ < 10) {
                    org.taumc.celeritas.CeleritasVintage.logger().error("[EntityGatherer] cached entity chunk list is stale: {} cached, {} actual",
                            this.entityChunks.size(), fresh.size());
                }
            }
            for (Chunk chunk : this.entityChunks) {
                if (!((ChunkAccessor)chunk).celeritas$getHasEntities()) {
                    continue;
                }
                ClassInheritanceMultiMap<Entity>[] entityMaps = chunk.getEntityLists();
                for (ClassInheritanceMultiMap<Entity> map : entityMaps) {
                    map.forEach(addEntity);
                }
            }
        } else {
            // Best we can do is the loaded entity list - this will miss some multipart entities
            world.loadedEntityList.forEach(addEntity);
        }
        return this.entityLists;
    }
}
