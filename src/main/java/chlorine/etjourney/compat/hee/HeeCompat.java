package chlorine.etjourney.compat.hee;

import java.lang.reflect.Field;

import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;

import chlorine.etjourney.compat.CompatModule;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.reserve.Reservations;

/**
 * Hardcore Ender Expansion: our End terrain leaves room for HEE islands, and HEE's spheres skip our terrain. HEE
 * replaces the sky biome in its own postInit, hence after:HardcoreEnderExpansion on the mod.
 */
public final class HeeCompat implements CompatModule {

    private static final String DECORATOR = "chylex.hee.world.biome.BiomeDecoratorHardcoreEnd";

    @Override
    public void postInit() {
        Reservations.register(new HeeIslandReservations());
        BiomeDecorator decorator = BiomeGenBase.sky.theBiomeDecorator;
        if (decorator == null || !decorator.getClass()
            .getName()
            .equals(DECORATOR)) {
            ModLog.LOG.warn("HEE End decorator not found ({}); its spheres stay as they are", decorator);
            return;
        }
        replace(decorator, "blobGen", new IslandAwareBlob());
        replace(decorator, "meteoroidGen", new IslandAwareMeteoroid());
    }

    private static void replace(BiomeDecorator decorator, String fieldName, Object replacement) {
        try {
            Field field = decorator.getClass()
                .getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(decorator, replacement);
        } catch (ReflectiveOperationException | RuntimeException e) {
            ModLog.LOG.warn("Could not replace HEE {}; its spheres stay as they are: {}", fieldName, e.toString());
        }
    }
}
