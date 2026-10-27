package chlorine.etjourney.content.end;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Registry names of the biome plants, whether a placed block may replace each, and the light each gives. */
final class PlantBlockTable {

    static final class Entry {

        final String name;
        final EndBlock kind;
        /** Grass-like: placing a block over it replaces it, as with vanilla tall grass. */
        final boolean replaceable;
        final int light;

        Entry(String name, EndBlock kind, boolean replaceable, int light) {
            this.name = name;
            this.kind = kind;
            this.replaceable = replaceable;
            this.light = light;
        }
    }

    static final List<Entry> ENTRIES = Collections.unmodifiableList(
        Arrays.asList(
            new Entry("grove_grass", EndBlock.GROVE_GRASS, true, 0),
            new Entry("grove_fern", EndBlock.GROVE_FERN, true, 0),
            new Entry("grove_bloom", EndBlock.GROVE_BLOOM, false, 0),
            new Entry("highland_tuft", EndBlock.HIGHLAND_TUFT, true, 0),
            new Entry("highland_thistle", EndBlock.HIGHLAND_THISTLE, false, 0),
            new Entry("mycelia_sprout", EndBlock.MYCELIA_SPROUT, true, 0),
            new Entry("glowcap", EndBlock.GLOWCAP, false, 7),
            new Entry("amber_reed", EndBlock.AMBER_REED, true, 0),
            new Entry("amber_bulb", EndBlock.AMBER_BULB, false, 4)));

    private PlantBlockTable() {}
}
