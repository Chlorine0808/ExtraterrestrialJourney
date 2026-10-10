package chlorine.etjourney.world.end;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Islets;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Land;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.feature.Shoals;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.ChunkArea;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.modifier.ModifierChain;
import chlorine.etjourney.world.end.modifier.TerrainView;
import chlorine.etjourney.world.end.modifier.builtin.CoreModifiers;
import chlorine.etjourney.world.end.region.RegionMap;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Style;
import chlorine.etjourney.world.end.region.StyleWeights;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Area;

/**
 * Answers terrain questions at any point, for feature placement and tools: style weights, land height, and a bare
 * column (continents, mountains, styles; no lakes or features). Placement probes use it, so a feature decides from
 * the same numbers whichever chunk asks.
 */
public final class TerrainSampler {

    private final long seed;
    private final RegionPicker picker;
    private final ModifierChain chain;
    private final Map<Point, StyleWeights> weightCache = new ConcurrentHashMap<>();
    private final ArcPaths.SegmentWeight arcGround = (x, y, z) -> nearZoneIsland(x, y, z) ? 0 : arcProbe().weight(x, z);
    private final Map<Modifier, Style> owners = new ConcurrentHashMap<>();

    public TerrainSampler(long seed, RegionPicker picker) {
        this.seed = seed;
        this.picker = picker;
        this.chain = new ModifierChain(columnModifiers(picker));
    }

    public long seed() {
        return seed;
    }

    public RegionPicker picker() {
        return picker;
    }

    /** Core column modifiers plus every style's modifiers; each style's apply at that style's weight. */
    static List<Modifier> columnModifiers(RegionPicker picker) {
        List<Modifier> all = new ArrayList<>(CoreModifiers.all());
        for (Style style : picker.styles()) all.addAll(style.modifiers);
        return all;
    }

    public StyleWeights weights(double x, double z) {
        // Exact coordinates: rounding to a block made answers depend on which point in the block asked first.
        Point key = new Point(x, z);
        StyleWeights w = weightCache.get(key);
        if (w == null) {
            w = StyleWeights.at(picker, seed, x, z);
            if (weightCache.size() > 65536) weightCache.clear();
            weightCache.put(key, w);
        }
        return w;
    }

    /** Land height without reservations. */
    public double land(double x, double z) {
        return Land.height(seed, x, z, 0, weights(x, z).voidShare());
    }

    /** A column with continents, mountains (unless left out) and styles, but no lakes or features. */
    public ColumnState bareColumn(double x, double z, boolean mountains) {
        ColumnState state = new ColumnState(x, z);
        TerrainView view = new BareView(x, z, mountains);
        chain.column(
            new ChunkArea(seed, (int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4, Collections.emptyList(), view),
            state,
            m -> weightOf(m, x, z));
        return state;
    }

    /** Weight of a modifier at a point: 1 for core modifiers, its style's weight otherwise. */
    double weightOf(Modifier modifier, double x, double z) {
        Style owner = ownerOf(modifier);
        return owner == null ? 1 : weights(x, z).of(owner);
    }

    Style ownerOf(Modifier modifier) {
        if (owners.isEmpty()) {
            for (Style style : picker.styles()) for (Modifier m : style.modifiers) owners.put(m, style);
        }
        return owners.get(modifier);
    }

    public ZoneIslands.LandProbe landProbe() {
        return this::land;
    }

    public Lakes.Probe lakeProbe() {
        return new Lakes.Probe() {

            @Override
            public boolean allowed(double x, double z) {
                return weights(x, z).allowsLakes();
            }

            @Override
            public double land(double x, double z) {
                return TerrainSampler.this.land(x, z);
            }

            @Override
            public double ground(double x, double z, boolean mountains) {
                return bareColumn(x, z, mountains).top;
            }

            @Override
            public double bottom(double x, double z) {
                return bareColumn(x, z, true).bottom;
            }
        };
    }

    public Holes.Probe holeProbe() {
        Lakes.Probe lakes = lakeProbe();
        return new Holes.Probe() {

            @Override
            public boolean allowed(double x, double z) {
                return weights(x, z).allowsHoles();
            }

            @Override
            public double land(double x, double z) {
                return TerrainSampler.this.land(x, z);
            }

            @Override
            public double top(double x, double z) {
                return bareColumn(x, z, true).top;
            }

            @Override
            public List<Lakes.Lake> lakesNear(double x, double z, double range) {
                return Lakes.near(seed, x, z, range, lakes);
            }
        };
    }

    public Islets.Probe isletProbe() {
        return new Islets.Probe() {

            @Override
            public double weight(double x, double z) {
                return weights(x, z).baseOf(Styles.ISLETS);
            }

            @Override
            public List<ZoneIslands.Island> zoneIslandsNear(double x, double z) {
                return ZoneIslands.near(seed, x, z, ZoneIslands.CELL, landProbe());
            }
        };
    }

    public Shoals.Probe shoalProbe() {
        return new Shoals.Probe() {

            @Override
            public double weight(double x, double z) {
                return weights(x, z).baseOf(Styles.SHOALS);
            }

            @Override
            public List<ZoneIslands.Island> zoneIslandsNear(double x, double z) {
                return ZoneIslands.near(seed, x, z, ZoneIslands.CELL, landProbe());
            }
        };
    }

    public StructureProbe structureProbe() {
        return new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return styleWeight(style, x, z);
            }

            @Override
            public double land(double x, double z) {
                return TerrainSampler.this.land(x, z);
            }

            @Override
            public double ground(double x, double z) {
                return TerrainSampler.this.land(x, z) > 0 ? bareColumn(x, z, true).top : -1000;
            }

            @Override
            public double underside(double x, double z) {
                return TerrainSampler.this.land(x, z) > 0 ? bareColumn(x, z, true).bottom : -1000;
            }

            @Override
            public double[] regionCentre(int cx, int cz) {
                return RegionMap.cellCentre(seed, cx, cz);
            }
        };
    }

    public double styleWeight(String style, double x, double z) {
        Style s = picker.byName(style);
        return s == null ? 0 : weights(x, z).of(s);
    }

    /** ARCS weight for arc segments, 0 near zone islands; one instance, so paths can keep its answers. */
    public ArcPaths.SegmentWeight arcGround() {
        return arcGround;
    }

    public ArcPaths.Probe arcProbe() {
        return new ArcPaths.Probe() {

            @Override
            public double weight(double x, double z) {
                return weights(x, z).of(Styles.ARCS);
            }

            @Override
            public double baseWeight(double x, double z) {
                return weights(x, z).baseOf(Styles.ARCS);
            }
        };
    }

    /** True when (x, y, z) lies within or just around a zone island. */
    public boolean nearZoneIsland(double x, double y, double z) {
        for (ZoneIslands.Island island : ZoneIslands.near(seed, x, z, ZoneIslands.CELL, landProbe())) {
            if (Math.hypot(x - island.x, z - island.z) > island.radius * 1.3 + 12) continue;
            if (y > island.y - island.down - 12 && y < island.y + island.up + 12) return true;
        }
        return false;
    }

    private static final class Point {

        private final double x, z;

        Point(double x, double z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Point && ((Point) o).x == x && ((Point) o).z == z;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(x) * 31 + Double.hashCode(z);
        }
    }

    /** View for bare columns: seeds and (optionally) mountains near the point, no other features. */
    private final class BareView implements TerrainView {

        private final List<Continent.Seed> seeds;
        private final List<Mountains.Mountain> mountains;

        BareView(double x, double z, boolean withMountains) {
            seeds = Continent.seedsNear(seed, x, z, Continent.warpReach());
            mountains = withMountains ? Mountains.near(seed, x, z, 0) : Collections.emptyList();
        }

        @Override
        public List<Continent.Seed> seeds() {
            return seeds;
        }

        @Override
        public List<Mountains.Mountain> mountains() {
            return mountains;
        }

        @Override
        public List<Lakes.Lake> lakes() {
            return Collections.emptyList();
        }

        @Override
        public List<Holes.Hole> holes() {
            return Collections.emptyList();
        }

        @Override
        public List<ZoneIslands.Island> zoneIslands() {
            return Collections.emptyList();
        }

        @Override
        public List<Islets.Islet> islets() {
            return Collections.emptyList();
        }

        @Override
        public List<Shoals.School> schools() {
            return Collections.emptyList();
        }

        @Override
        public ArcPaths.Segments arcs() {
            return ArcPaths.segmentsNear(Collections.emptyList(), 0, 0, 0, 0, (a, b, c) -> true);
        }

        @Override
        public List<Area> reservedAt(int chunkX, int chunkZ) {
            return Collections.emptyList();
        }

        @Override
        public double voidShare(double x, double z) {
            return weights(x, z).voidShare();
        }

        @Override
        public double mountainScale(double x, double z) {
            return weights(x, z).mountainScale();
        }

        @Override
        public double valleyScale(double x, double z) {
            return weights(x, z).valleyScale();
        }

        @Override
        public double weight(String style, double x, double z) {
            return styleWeight(style, x, z);
        }

        @Override
        public StructureProbe structures() {
            return structureProbe();
        }

        @Override
        public ColumnState column(double x, double z) {
            return bareColumn(x, z, true);
        }

        @Override
        public double[] densityField(boolean upper, boolean withShapes) {
            throw new UnsupportedOperationException("bare columns have no density grid");
        }

        @Override
        public double densityAt(int x, int y, int z) {
            throw new UnsupportedOperationException("bare columns have no density grid");
        }
    }
}
