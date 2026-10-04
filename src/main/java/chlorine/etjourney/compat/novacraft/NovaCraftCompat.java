package chlorine.etjourney.compat.novacraft;

import java.lang.reflect.Field;
import java.util.Set;

import chlorine.etjourney.compat.CompatModule;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.reserve.PredictedIslands;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/** NovaCraft: registers its End island generators so /etj end destitute can find their islands. */
public final class NovaCraftCompat implements CompatModule {

    private static final String PACKAGE = "com.NovaCraft.world.end.";
    /** NovaCraft only builds these islands this far out. */
    private static final int MIN_BLOCK = 1000;

    @Override
    @SuppressWarnings("unchecked")
    public void postInit() {
        try {
            Field registry = GameRegistry.class.getDeclaredField("worldGenerators");
            registry.setAccessible(true);
            for (IWorldGenerator generator : (Set<IWorldGenerator>) registry.get(null)) {
                String name = generator.getClass()
                    .getName();
                // Each starts an island when its first roll hits a fixed value (read from their bytecode).
                if (name.equals(PACKAGE + "DestitudeIslandWorldGen")) register(generator, "destitute", 17);
                if (name.equals(PACKAGE + "EndIslandWorldGen")) register(generator, "novacraft_island", 0);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.LOG.warn("Could not read NovaCraft End generators: {}", e.toString());
        }
    }

    private static void register(IWorldGenerator generator, String label, int hit) throws ReflectiveOperationException {
        Field rate = generator.getClass()
            .getDeclaredField("novacraft_random");
        rate.setAccessible(true);
        PredictedIslands.register(new PredictedIslands.Rule(label, rate.getInt(generator), hit, MIN_BLOCK));
    }
}
