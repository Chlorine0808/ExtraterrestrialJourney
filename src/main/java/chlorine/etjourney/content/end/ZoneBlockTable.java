package chlorine.etjourney.content.end;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Registry names of the End zone and biome blocks and the EndBlock kind each one stands for. */
final class ZoneBlockTable {

    /** One block; a surface has the rock whose texture its bottom face shows, a rock has none. */
    static final class Entry {

        final String name;
        final EndBlock kind;
        final String bottom;

        Entry(String name, EndBlock kind, String bottom) {
            this.name = name;
            this.kind = kind;
            this.bottom = bottom;
        }
    }

    static final List<Entry> ENTRIES = Collections.unmodifiableList(
        Arrays.asList(
            new Entry("solar_rock", EndBlock.SOLAR_FILL, null),
            new Entry("solar_crust", EndBlock.SOLAR_TOP, "solar_rock"),
            new Entry("vortex_rock", EndBlock.VORTEX_FILL, null),
            new Entry("vortex_turf", EndBlock.VORTEX_TOP, "vortex_rock"),
            new Entry("nebula_rock", EndBlock.NEBULA_FILL, null),
            new Entry("nebula_moss", EndBlock.NEBULA_TOP, "nebula_rock"),
            new Entry("stardust_rock", EndBlock.STARDUST_FILL, null),
            new Entry("stardust_crust", EndBlock.STARDUST_TOP, "stardust_rock"),
            new Entry("grove_rock", EndBlock.GROVE_FILL, null),
            new Entry("grove_moss", EndBlock.GROVE_TOP, "grove_rock"),
            new Entry("highland_rock", EndBlock.HIGHLAND_FILL, null),
            new Entry("highland_crust", EndBlock.HIGHLAND_TOP, "highland_rock"),
            new Entry("mycelia_rock", EndBlock.MYCELIA_FILL, null),
            new Entry("mycelia_turf", EndBlock.MYCELIA_TOP, "mycelia_rock"),
            new Entry("amber_rock", EndBlock.AMBER_FILL, null),
            new Entry("amber_crust", EndBlock.AMBER_TOP, "amber_rock"),
            new Entry("lucent_rock", EndBlock.LUCENTWOODS_FILL, null),
            new Entry("lucent_moss", EndBlock.LUCENTWOODS_TOP, "lucent_rock")));

    private ZoneBlockTable() {}
}
