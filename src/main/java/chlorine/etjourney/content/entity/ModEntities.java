package chlorine.etjourney.content.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.common.registry.EntityRegistry;

/** The one place every region registers its entities through. */
public final class ModEntities {

    public static final EntityTable TABLE = new EntityTable();
    /** Vanilla zombie values: tracking range, update interval, velocity updates. */
    private static final int TRACKING_RANGE = 80, UPDATE_INTERVAL = 3;
    /** Shots move fast and may turn, so they report their place more often than mobs. */
    private static final int SHOT_TRACKING_RANGE = 64, SHOT_UPDATE_INTERVAL = 2;

    private ModEntities() {}

    /** Registers an entity with FML; a null rule keeps it out of natural spawning. */
    public static void register(String name, Class<? extends EntityLiving> type, SpawnRule rule) {
        EntityTable.Entry entry = TABLE.add(name, type, rule);
        // The mod ID stands in for the mod instance, which the layers may not reference.
        EntityRegistry.registerModEntity(type, name, entry.id, ModInfo.MODID, TRACKING_RANGE, UPDATE_INTERVAL, true);
    }

    /** Registers a projectile entity, which never spawns naturally. */
    public static void registerProjectile(String name, Class<? extends Entity> type) {
        EntityTable.Entry entry = TABLE.add(name, type, null);
        EntityRegistry
            .registerModEntity(type, name, entry.id, ModInfo.MODID, SHOT_TRACKING_RANGE, SHOT_UPDATE_INTERVAL, true);
    }
}
