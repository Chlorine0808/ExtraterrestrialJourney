package chlorine.etjourney.content.craft;

/** One input of a recipe: a number of items named either by registry name or by ore dictionary name. */
public final class ItemInput {

    /** Matches any metadata; the same value as the ore dictionary's wildcard. */
    public static final int ANY_META = 32767;

    private final String id;
    private final int meta;
    private final String oreName;
    final int amount;

    private ItemInput(String id, int meta, String oreName, int amount) {
        if (amount < 1) throw new IllegalArgumentException("amount must be positive: " + amount);
        this.id = id;
        this.meta = meta;
        this.oreName = oreName;
        this.amount = amount;
    }

    public static ItemInput item(String id, int meta, int amount) {
        return new ItemInput(id, meta, null, amount);
    }

    public static ItemInput ore(String oreName, int amount) {
        return new ItemInput(null, 0, oreName, amount);
    }

    boolean accepts(ItemKey key) {
        if (oreName != null) return key.oreNames.contains(oreName);
        return id.equals(key.id) && (meta == ANY_META || meta == key.meta);
    }
}
