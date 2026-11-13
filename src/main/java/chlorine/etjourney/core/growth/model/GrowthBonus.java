package chlorine.etjourney.core.growth.model;

/** What levels add, and how much experience a hit gives. */
public final class GrowthBonus {

    public static final float WEAPON_PER_LEVEL = 0.02f;
    public static final float ACCESSORY_PER_LEVEL = 0.01f;
    public static final float ARMOR_PER_LEVEL = 0.005f;

    private GrowthBonus() {}

    /** The damage bonus of a held weapon, such as 0.2 for +20%. */
    public static float weapon(int level) {
        return WEAPON_PER_LEVEL * level;
    }

    public static float accessory(int level) {
        return ACCESSORY_PER_LEVEL * level;
    }

    /** The factor on damage taken through every worn armor piece; pieces multiply. */
    public static float armor(int... levels) {
        float factor = 1;
        for (int level : levels) factor *= Math.max(0, 1 - ARMOR_PER_LEVEL * level);
        return factor;
    }

    /** The amount rounded, at least 1. */
    public static int xpForHit(float amount) {
        return Math.max(1, Math.round(amount));
    }
}
