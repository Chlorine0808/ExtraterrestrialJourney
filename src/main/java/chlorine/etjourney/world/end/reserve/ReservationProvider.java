package chlorine.etjourney.world.end.reserve;

import java.util.List;

/**
 * Supplies the areas another mod will build on. generator and world are the End's chunk provider and world, typed
 * as Object so this package stays free of Minecraft; providers in the compat layer cast them.
 */
public interface ReservationProvider {

    List<Area> near(Object generator, Object world, int chunkX, int chunkZ, int chunkRadius);
}
