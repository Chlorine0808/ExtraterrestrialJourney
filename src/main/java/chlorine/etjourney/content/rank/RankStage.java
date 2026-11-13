package chlorine.etjourney.content.rank;

import net.minecraft.world.World;

/** The part of the journey a place belongs to, which sets the rank odds. */
public enum RankStage {

    EARLY,
    MID,
    LATE;

    /** Always EARLY until the Zones exist; they will answer MID or LATE here. */
    public static RankStage at(World world, double x, double z) {
        return EARLY;
    }
}
