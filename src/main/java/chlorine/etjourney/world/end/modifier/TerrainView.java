package chlorine.etjourney.world.end.modifier;

import java.util.List;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.feature.Shoals;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.reserve.Area;

/** Everything near one chunk that the built-in modifiers read, gathered once by the chunk's plan. */
public interface TerrainView {

    List<Continent.Seed> seeds();

    List<Mountains.Mountain> mountains();

    /** Lakes near the chunk, without those touching reserved areas. */
    List<Lakes.Lake> lakes();

    List<Holes.Hole> holes();

    /** Zone islands near the chunk, without those touching reserved areas. */
    List<ZoneIslands.Island> zoneIslands();

    List<Islets.Islet> islets();

    List<Shoals.School> schools();

    /** Arc tube segments crossing the chunk, already filtered to ARCS ground clear of islands and reservations. */
    ArcPaths.Segments arcs();

    /** Reserved areas as seen from another chunk (a feature decides by its own centre chunk). */
    List<Area> reservedAt(int chunkX, int chunkZ);

    /** Base weight of styles without a continent at (x, z). */
    double voidShare(double x, double z);

    double mountainScale(double x, double z);

    double valleyScale(double x, double z);
}
