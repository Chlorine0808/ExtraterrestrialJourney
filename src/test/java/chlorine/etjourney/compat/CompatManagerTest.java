package chlorine.etjourney.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class CompatManagerTest {

    private static final class Recorder implements CompatModule {

        private final String name;
        private final List<String> calls;

        Recorder(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public void preInit() {
            calls.add(name + ":preInit");
        }

        @Override
        public void init() {
            calls.add(name + ":init");
        }

        @Override
        public void postInit() {
            calls.add(name + ":postInit");
        }
    }

    private static Map<String, Supplier<CompatModule>> recorders(List<String> calls, String... modIds) {
        Map<String, Supplier<CompatModule>> factories = new LinkedHashMap<>();
        for (String modId : modIds) factories.put(modId, () -> new Recorder(modId, calls));
        return factories;
    }

    @Test
    void onlyLoadedModsReceiveStages() {
        List<String> calls = new ArrayList<>();
        CompatManager manager = new CompatManager(
            recorders(calls, "Thaumcraft", "netherlicious"),
            "netherlicious"::equals);

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Arrays.asList("netherlicious:preInit", "netherlicious:init", "netherlicious:postInit"), calls);
    }

    @Test
    void noLoadedModsMeansNoCallsAndNoErrors() {
        List<String> calls = new ArrayList<>();
        CompatManager manager = new CompatManager(recorders(calls, "Thaumcraft"), id -> false);

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Collections.emptyList(), calls);
        assertEquals(
            0,
            manager.active()
                .size());
    }

    @Test
    void absentModFactoryIsNeverInvoked() {
        List<String> created = new ArrayList<>();
        Map<String, Supplier<CompatModule>> factories = new LinkedHashMap<>();
        factories.put("Thaumcraft", () -> {
            created.add("Thaumcraft");
            return new Recorder("Thaumcraft", new ArrayList<>());
        });
        CompatManager manager = new CompatManager(factories, id -> false);

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Collections.emptyList(), created);
    }

    @Test
    void detectionHappensOncePerManager() {
        List<String> asked = new ArrayList<>();
        CompatManager manager = new CompatManager(recorders(new ArrayList<>(), "Thaumcraft"), id -> {
            asked.add(id);
            return true;
        });

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Collections.singletonList("Thaumcraft"), asked);
    }
}
