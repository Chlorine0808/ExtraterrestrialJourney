package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/** A mob's rank, abilities and ability state, kept in its Forge entity data so they survive reloads. */
public final class RankData {

    /** What an unranked mob was born as: nothing special, a split child, or a summoned minion. */
    public static final int FREE = 0, SPLIT_CHILD = 1, MINION = 2;

    private static final String KEY = "etjRank", RANK = "rank", ABILITIES = "abilities", USED = "used.", ROLE = "role",
        PARENT_MOST = "parentMost", PARENT_LEAST = "parentLeast", MISSES = "misses";
    private static final int TAG_STRING = 8;

    private RankData() {}

    /** Whether the mob has rolled, whatever it rolled. */
    public static boolean rolled(Entity e) {
        return e.getEntityData()
            .hasKey(KEY);
    }

    public static Rank rank(Entity e) {
        return rolled(e) ? Rank.of(tag(e).getByte(RANK)) : Rank.NORMAL;
    }

    public static boolean ranked(Entity e) {
        return rank(e) != Rank.NORMAL;
    }

    public static List<String> abilities(Entity e) {
        List<String> out = new ArrayList<>();
        if (!rolled(e)) return out;
        NBTTagList list = tag(e).getTagList(ABILITIES, TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) out.add(list.getStringTagAt(i));
        return out;
    }

    public static void set(Entity e, Rank rank, List<String> abilities) {
        NBTTagCompound tag = tag(e);
        tag.setByte(RANK, (byte) rank.ordinal());
        NBTTagList list = new NBTTagList();
        for (String a : abilities) list.appendTag(new NBTTagString(a));
        tag.setTag(ABILITIES, list);
    }

    public static int role(Entity e) {
        return rolled(e) ? tag(e).getByte(ROLE) : FREE;
    }

    /** The UUID of the mob this one split from or was summoned by, or null. */
    public static UUID parent(Entity e) {
        if (role(e) == FREE) return null;
        NBTTagCompound tag = tag(e);
        return new UUID(tag.getLong(PARENT_MOST), tag.getLong(PARENT_LEAST));
    }

    public static void setOffspring(Entity e, int role, UUID parent) {
        NBTTagCompound tag = tag(e);
        tag.setByte(ROLE, (byte) role);
        tag.setLong(PARENT_MOST, parent.getMostSignificantBits());
        tag.setLong(PARENT_LEAST, parent.getLeastSignificantBits());
    }

    /** Checks in a row a minion has not found its parent. */
    public static int misses(Entity e) {
        return tag(e).getInteger(MISSES);
    }

    public static void setMisses(Entity e, int misses) {
        tag(e).setInteger(MISSES, misses);
    }

    /** The total world time an ability was last used, or Cooldown.NEVER. */
    public static long lastUsed(Entity e, String ability) {
        NBTTagCompound tag = tag(e);
        return tag.hasKey(USED + ability) ? tag.getLong(USED + ability) : Cooldown.NEVER;
    }

    public static void markUsed(Entity e, String ability, long now) {
        tag(e).setLong(USED + ability, now);
    }

    /** The mob's own compound, made on first use. */
    static NBTTagCompound tag(Entity e) {
        NBTTagCompound data = e.getEntityData();
        if (!data.hasKey(KEY)) data.setTag(KEY, new NBTTagCompound());
        return data.getCompoundTag(KEY);
    }
}
