package chlorine.etjourney.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import chlorine.etjourney.compat.hee.HeeCompat;
import chlorine.etjourney.compat.netherlicious.NetherliciousCompat;
import chlorine.etjourney.compat.novacraft.NovaCraftCompat;
import chlorine.etjourney.compat.thaumcraft.ThaumcraftCompat;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.Loader;

/** Detects installed integration targets once, then creates and drives only their modules. */
public final class CompatManager {

    private final Map<String, Supplier<CompatModule>> factories;
    private final Predicate<String> isLoaded;
    private List<CompatModule> active;

    public CompatManager(Map<String, Supplier<CompatModule>> factories, Predicate<String> isLoaded) {
        this.factories = factories;
        this.isLoaded = isLoaded;
    }

    public static CompatManager createDefault() {
        // Lambdas rather than ::new so a module class is not resolved until its mod is known to be present.
        Map<String, Supplier<CompatModule>> factories = new LinkedHashMap<>();
        factories.put("Thaumcraft", () -> new ThaumcraftCompat());
        factories.put("netherlicious", () -> new NetherliciousCompat());
        factories.put("HardcoreEnderExpansion", () -> new HeeCompat());
        factories.put("nova_craft", () -> new NovaCraftCompat());
        return new CompatManager(factories, Loader::isModLoaded);
    }

    public List<CompatModule> active() {
        if (active == null) {
            List<CompatModule> found = new ArrayList<>();
            for (Map.Entry<String, Supplier<CompatModule>> entry : factories.entrySet()) {
                if (isLoaded.test(entry.getKey())) {
                    found.add(
                        entry.getValue()
                            .get());
                    ModLog.LOG.info("Compat enabled: {}", entry.getKey());
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
