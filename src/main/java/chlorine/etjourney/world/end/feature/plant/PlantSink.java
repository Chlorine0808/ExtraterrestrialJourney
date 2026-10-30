package chlorine.etjourney.world.end.feature.plant;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Where a plant writes its blocks; the implementation writes only into air or replaceable flora. */
public interface PlantSink {

    void put(int x, int y, int z, EndBlock block);
}
