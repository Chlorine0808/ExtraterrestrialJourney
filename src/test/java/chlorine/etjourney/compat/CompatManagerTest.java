package chlorine.etjourney.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class CompatManagerTest {

    private static final class Recorder implements CompatModule {

        private final String modId;
        private final List<String> calls;

        Recorder(String modId, List<String> calls) {
            this.modId = modId;
            this.calls = calls;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public void preInit() {
            calls.add(modId + ":preInit");
        }

        @Override
        public void init() {
            calls.add(modId + ":init");
        }

        @Override
        public void postInit() {
            calls.add(modId + ":postInit");
        }
    }

    @Test
    void onlyLoadedModsReceiveStages() {
        List<String> calls = new ArrayList<>();
        CompatManager manager = new CompatManager(
            Arrays.<CompatModule>asList(new Recorder("Thaumcraft", calls), new Recorder("netherlicious", calls)),
            "netherlicious"::equals);

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Arrays.asList("netherlicious:preInit", "netherlicious:init", "netherlicious:postInit"), calls);
    }

    @Test
    void noLoadedModsMeansNoCallsAndNoErrors() {
        List<String> calls = new ArrayList<>();
        CompatManager manager = new CompatManager(
            Collections.<CompatModule>singletonList(new Recorder("Thaumcraft", calls)),
            id -> false);

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
    void detectionHappensOncePerManager() {
        List<String> asked = new ArrayList<>();
        CompatManager manager = new CompatManager(
            Collections.<CompatModule>singletonList(new Recorder("Thaumcraft", new ArrayList<>())),
            id -> {
                asked.add(id);
                return true;
            });

        manager.preInit();
        manager.init();
        manager.postInit();

        assertEquals(Collections.singletonList("Thaumcraft"), asked);
    }
}
