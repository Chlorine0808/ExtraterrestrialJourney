package chlorine.etjourney.content.craft;

/** What a recipe hands back: a number of one item. */
public final class ItemOutput {

    public final String id;
    public final int meta;
    public final int amount;

    public ItemOutput(String id, int meta, int amount) {
        if (amount < 1) throw new IllegalArgumentException("amount must be positive: " + amount);
        this.id = id;
        this.meta = meta;
        this.amount = amount;
    }
}
