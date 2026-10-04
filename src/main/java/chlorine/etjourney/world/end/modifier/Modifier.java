package chlorine.etjourney.world.end.modifier;

import java.util.List;

/**
 * One part of the terrain. It writes to any of three outputs: the column state, 3D shapes, and block-level work.
 * weight (0 to 1) is how strongly its style applies at the place; borders blend by lowering it.
 */
public interface Modifier {

    /** Application order: smaller runs first. */
    int order();

    default void column(ChunkArea area, ColumnState state, double weight) {}

    default void shapes(ChunkArea area, List<Shape> out, double weight) {}

    default void blocks(ChunkArea area, BlockSink sink, double weight) {}
}
