package chlorine.etjourney.content.rank;

/** How far above the common herd an enemy stands; every value is provisional. */
public enum Rank {

    NORMAL('f', 0, 0, 0, 0, 0, 0, 1, 0, 1, 1),
    RARE('9', 0x5555FF, 1, 0.5, 0.3, 0.05, 0.2, 1.5, 0, 2, 3),
    EPIC('5', 0xAA00AA, 2, 1.2, 0.6, 0.10, 0.3, 2.0, 1, 3, 5),
    LEGENDARY('6', 0xFFAA00, 3, 2.5, 1.0, 0.15, 0.5, 2.5, 1, 4, 8);

    /** Minecraft's formatting code for the name. */
    public final char color;
    public final int rgb;
    public final int abilities;
    /** Parts of the base value added: health, attack and speed; knockback resistance is added as is. */
    public final double health, attack, speed, knockback;
    /** How the status effects its attacks give are strengthened. */
    public final double effectLength;
    public final int effectLevels;
    public final int loot, experience;

    Rank(char color, int rgb, int abilities, double health, double attack, double speed, double knockback,
        double effectLength, int effectLevels, int loot, int experience) {
        this.color = color;
        this.rgb = rgb;
        this.abilities = abilities;
        this.health = health;
        this.attack = attack;
        this.speed = speed;
        this.knockback = knockback;
        this.effectLength = effectLength;
        this.effectLevels = effectLevels;
        this.loot = loot;
        this.experience = experience;
    }

    /** NORMAL for an unknown ordinal, so a damaged tag never crashes. */
    public static Rank of(int ordinal) {
        Rank[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NORMAL;
    }
}
