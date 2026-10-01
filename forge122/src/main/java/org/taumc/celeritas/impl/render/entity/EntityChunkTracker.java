package org.taumc.celeritas.impl.render.entity;

/**
 * Counts changes to which client chunks hold entities, so {@link EntityGatherer} rebuilds its
 * chunk list only when it changes instead of scanning every loaded chunk each frame.
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