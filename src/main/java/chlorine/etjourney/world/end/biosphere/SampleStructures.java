package chlorine.etjourney.world.end.biosphere;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Keeps villages, mineshafts, strongholds, temples, fortresses and dungeons out of the sample dimensions. */
public final class SampleStructures {

    private static final Set<Class<?>> VANILLA = new HashSet<>(
        Arrays.asList(
            MapGenVillage.class,
            MapGenMineshaft.class,
            MapGenStronghold.class,
            MapGenScatteredFeature.class,
            MapGenNetherBridge.class));

    /** Set while a sample dimension builds its generator, which is when the map generators are created. */
    private static boolean building;

    static IChunkProvider building(Supplier<IChunkProvider> make) {
        building = true;
        try {
            return make.get();
        } finally {
            building = false;
        }
    }

    @SubscribeEvent
    public void onInitMapGen(InitMapGenEvent event) {
        if (!building) return;
        // The generators cast to these types, so each stand-in is the same class that never starts a structure.
        // A generator another mod has already replaced may be cast to that mod's own type; leave it alone.
        if (event.newGen == null || !VANILLA.contains(event.newGen.getClass())) return;
        switch (event.type) {
            case VILLAGE:
                event.newGen = new MapGenVillage() {

                    @Override
                    protected boolean canSpawnStructureAtCoords(int x, int z) {
                        return false;
                    }
                };
                break;
            case MINESHAFT:
                event.newGen = new MapGenMineshaft() {

                    @Override
                    protected boolean canSpawnStructureAtCoords(int x, int z) {
                        return false;
                    }
                };
                break;
            case STRONGHOLD:
                event.newGen = new MapGenStronghold() {

                    @Override
                    protected boolean canSpawnStructureAtCoords(int x, int z) {
                        return false;
                    }
                };
                break;
            case SCATTERED_FEATURE:
                event.newGen = new MapGenScatteredFeature() {

                    @Override
                    protected boolean canSpawnStructureAtCoords(int x, int z) {
                        return false;
                    }
                };
                break;
            case NETHER_BRIDGE:
                event.newGen = new MapGenNetherBridge() {

                    @Override
                    protected boolean canSpawnStructureAtCoords(int x, int z) {
                        return false;
                    }
                };
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onPopulate(PopulateChunkEvent.Populate event) {
        if (event.type == PopulateChunkEvent.Populate.EventType.DUNGEON && SampleWorlds.isSample(event.world))
            event.setResult(Event.Result.DENY);
    }
}
