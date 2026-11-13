package chlorine.etjourney.core.growth.model;

/** How lucky a piece of gear came out: its draw weight, affix count, lowest affix roll and colour code. */
public enum Rarity {

    COMMON("common", 60, 0, 0f, 'f'),
    UNCOMMON("uncommon", 25, 1, 0f, 'a'),
    RARE("rare", 10, 2, 0.25f, '9'),
    EPIC("epic", 4, 3, 0.5f, '5'),
    LEGENDARY("legendary", 1, 4, 0.75f, '6');

    public final String id;
    public final int weight;
    public final int affixes;
    public final float floor;
    /** A Minecraft formatting code, without the section sign. */
    public final char color;

    Rarity(String id, int weight, int affixes, float floor, char color) {
        this.id = id;
        this.weight = weight;
        this.affixes = affixes;
        this.floor = floor;
        this.color = color;
    }

    /** Null for a name no rarity has. */
    public static Rarity byName(String id) {
        for (Rarity r : values()) if (r.id.equals(id)) return r;
        return null;
    }
}
