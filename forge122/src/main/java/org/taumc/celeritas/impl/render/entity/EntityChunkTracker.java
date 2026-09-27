package org.taumc.celeritas.impl.render.entity;

/**
 * Change counter for the client world's entity-to-chunk assignment: bumped when
 * an entity is added to or removed from a client chunk's entity lists (which
 * includes moving between chunks) and when a client chunk loads or unloads.
 * {@link EntityGatherer} rebuilds its list of entity-bearing chunks only when
 * it changes, instead of scanning every loaded chunk each frame. Client thread.
 */
public final class EntityChunkTracker {
    private static int generation;

    private EntityChunkTracker() {}

    public static void markChanged() {
        generation++;
    }

    public static int generation() {
        return generation;
    }
}