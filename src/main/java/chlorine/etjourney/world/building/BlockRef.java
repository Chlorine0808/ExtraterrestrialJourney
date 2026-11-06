package chlorine.etjourney.world.building;

/** A block by registry name and metadata, with the chest loot to fill it with if any. */
public final class BlockRef {

    public final String name;
    public final int meta;
    /** ChestGenHooks category, or null. */
    public final String loot;

    public BlockRef(String name, int meta, String loot) {
        this.name = name;
        this.meta = meta;
        this.loot = loot;
    }

    /** Parses "mod:block" or "mod:block:meta"; a missing or "*" meta is 0. */
    public static BlockRef parse(String id, String loot) throws TemplateException {
        String[] parts = id.split(":");
        if (parts.length < 2 || parts.length > 3 || parts[0].isEmpty() || parts[1].isEmpty())
            throw new TemplateException("bad block id '" + id + "'");
        int meta = 0;
        if (parts.length == 3 && !parts[2].equals("*")) {
            try {
                meta = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                throw new TemplateException("bad metadata in '" + id + "'");
            }
            if (meta < 0 || meta > 15) throw new TemplateException("metadata out of range in '" + id + "'");
        }
        return new BlockRef(parts[0] + ":" + parts[1], meta, loot);
    }

    public BlockRef withMeta(int meta) {
        return new BlockRef(name, meta, loot);
    }

    @Override
    public String toString() {
        return name + ":" + meta;
    }
}
