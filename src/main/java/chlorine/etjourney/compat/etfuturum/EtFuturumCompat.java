package chlorine.etjourney.compat.etfuturum;

import org.apache.commons.lang3.ArrayUtils;

import chlorine.etjourney.compat.CompatModule;
import chlorine.etjourney.core.config.ETJConfig;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;

/** Et Futurum Requiem integration; its classes may be touched only from here. */
public final class EtFuturumCompat implements CompatModule {

    /**
     * Et Futurum picks the worlds for amethyst geodes by dimension ID, and the Nether sample's ID is not the
     * Nether's, so geodes would grow in Nether biospheres; leave that dimension out like the Nether itself.
     */
    @Override
    public void postInit() {
        int dim = ETJConfig.netherSampleDimension;
        if (ConfigWorld.amethystDimensionBlacklistAsWhitelist
            || ArrayUtils.contains(ConfigWorld.amethystDimensionBlacklist, dim)) return;
        ConfigWorld.amethystDimensionBlacklist = ArrayUtils.add(ConfigWorld.amethystDimensionBlacklist, dim);
    }
}
