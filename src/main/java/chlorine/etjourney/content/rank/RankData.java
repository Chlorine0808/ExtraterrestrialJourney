package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/** A mob's rank, abilities and ability state, kept in its Forge entity data so they survive reloads. */
public final class RankData {

    private static final String KEY = "etjRank", RANK = "rank", ABILITIES = "abilities", USED = "used.";
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
