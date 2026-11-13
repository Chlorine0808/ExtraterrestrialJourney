package chlorine.etjourney.content.end;

import java.util.EnumSet;

import chlorine.etjourney.content.entity.ModEntities;
import chlorine.etjourney.content.entity.SpawnRule;
import chlorine.etjourney.world.end.feature.EndBiome;

/** The End's mobs; names are provisional until the enemy roster is settled. */
public final class EndEntities {

    public static final String SPORE_ZOMBIE = "spore_zombie";
    /** Solar's worm; no zone yet, so it only comes from /summon etjourney.flame_worm. */
    public static final String FLAME_WORM = "flame_worm";

    private EndEntities() {}

    public static void preInit() {
        // Weight 4 against the enderman's 10; the zombie's own cap keeps it near a fifth of MYCELIA's mobs.
        ModEntities.register(
            SPORE_ZOMBIE,
            EntitySporeZombie.class,
            SpawnRule.builder(EnumSet.of(EndBiome.MYCELIA), false)
                .weight(4, 1, 4)
                .build());
        ModEntities.register(FLAME_WORM, EntityFlameWorm.class, null);
    }
}
