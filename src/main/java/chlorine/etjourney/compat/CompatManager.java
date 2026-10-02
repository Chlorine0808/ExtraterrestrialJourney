package chlorine.etjourney.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import chlorine.etjourney.compat.netherlicious.NetherliciousCompat;
import chlorine.etjourney.compat.thaumcraft.ThaumcraftCompat;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.Loader;

/** Detects installed integration targets once and forwards lifecycle stages to them. */
public final class CompatManager {

    private final List<CompatModule> modules;
    private final Predicate<String> isLoaded;
    private List<CompatModule> active;

    public CompatManager(List<CompatModule> modules, Predicate<String> isLoaded) {
        this.modules = modules;
        this.isLoaded = isLoaded;
    }

    public static CompatManager createDefault() {
        return new CompatManager(Arrays.asList(new ThaumcraftCompat(), new NetherliciousCompat()), Loader::isModLoaded);
    }

    public List<CompatModule> active() {
        if (active == null) {
            List<CompatModule> found = new ArrayList<>();
            for (CompatModule module : modules) {
                if (isLoaded.test(module.modId())) {
                    found.add(module);
                    ModLog.LOG.info("Compat enabled: {}", module.modId());
                }
            }
            active = Collections.unmodifiableList(found);
        }
        return active;
    }

    public void preInit() {
        for (CompatModule module : active()) module.preInit();
    }

    public void init() {
        for (CompatModule module : active()) module.init();
    }

    public void postInit() {
        for (CompatModule module : active()) module.postInit();
    }
}
