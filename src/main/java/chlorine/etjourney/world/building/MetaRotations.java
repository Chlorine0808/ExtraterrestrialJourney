package chlorine.etjourney.world.building;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Metadata rotations registered by block name, for blocks the vanilla shapes do not cover. */
public final class MetaRotations {

    private static final Map<String, MetaRotation> BY_NAME = new ConcurrentHashMap<>();

    private MetaRotations() {}

    public static void register(String blockName, MetaRotation rotation) {
        BY_NAME.put(blockName, rotation);
    }

    /** The registered rotation, or null. */
    public static MetaRotation forName(String blockName) {
        return BY_NAME.get(blockName);
    }
}
