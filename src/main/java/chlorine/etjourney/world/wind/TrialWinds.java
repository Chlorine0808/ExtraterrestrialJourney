package chlorine.etjourney.world.wind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.wind.model.Wind;

/** The winds put by /etj wind, saved with the world and put back when it loads; other winds are not saved. */
public final class TrialWinds extends WorldSavedData {

    public static final String NAME = "etjourney_trial_winds";
    private static final int TAG_COMPOUND = 10;

    public static final class Entry {

        public final int dimension;
        public final Wind wind;

        Entry(int dimension, Wind wind) {
            this.dimension = dimension;
            this.wind = wind;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    /** Each live wind's entry by dimension and ID in WindWorld.SERVER; IDs are handed out anew on each load. */
    private final Map<Long, Entry> live = new HashMap<>();

    public TrialWinds() {
        this(NAME);
    }

    /** For the map storage, which makes saved data by name. */
    public TrialWinds(String name) {
        super(name);
    }

    /** Puts the wind into the world and saves it; returns its ID. */
    public static int add(int dimension, Wind wind) {
        TrialWinds saved = get();
        int id = WindWorld.SERVER.add(dimension, wind);
        if (saved != null) saved.live.put(key(dimension, id), saved.put(dimension, wind));
        return id;
    }

    public static boolean remove(int dimension, int id) {
        TrialWinds saved = get();
        if (saved != null) {
            Entry entry = saved.live.remove(key(dimension, id));
            if (entry != null) saved.forget(entry);
        }
        return WindWorld.SERVER.remove(dimension, id);
    }

    public static void clear(int dimension) {
        TrialWinds saved = get();
        if (saved != null) {
            saved.forgetDimension(dimension);
            saved.live.keySet()
                .removeIf(k -> (int) (k >> 32) == dimension);
        }
        WindWorld.SERVER.clear(dimension);
    }

    /** Puts every saved wind back; the server's winds must have been cleared first. */
    public static void restore() {
        TrialWinds saved = get();
        if (saved == null) return;
        saved.live.clear();
        for (Entry e : saved.entries) saved.live.put(key(e.dimension, WindWorld.SERVER.add(e.dimension, e.wind)), e);
    }

    /** The overworld's copy, which holds every dimension's; null before the overworld loads. */
    private static TrialWinds get() {
        World overworld = DimensionManager.getWorld(0);
        if (overworld == null) return null;
        TrialWinds saved = (TrialWinds) overworld.mapStorage.loadData(TrialWinds.class, NAME);
        if (saved == null) {
            saved = new TrialWinds();
            overworld.mapStorage.setData(NAME, saved);
        }
        return saved;
    }

    private static long key(int dimension, int id) {
        return (long) dimension << 32 | id & 0xFFFFFFFFL;
    }

    Entry put(int dimension, Wind wind) {
        Entry entry = new Entry(dimension, wind);
        entries.add(entry);
        markDirty();
        return entry;
    }

    void forget(Entry entry) {
        if (entries.remove(entry)) markDirty();
    }

    void forgetDimension(int dimension) {
        for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
            if (it.next().dimension != dimension) continue;
            it.remove();
            markDirty();
        }
    }

    List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        entries.clear();
        NBTTagList list = tag.getTagList("winds", TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound w = list.getCompoundTagAt(i);
            try {
                entries.add(
                    new Entry(
                        w.getInteger("dim"),
                        new Wind(
                            w.getDouble("minX"),
                            w.getDouble("minY"),
                            w.getDouble("minZ"),
                            w.getDouble("maxX"),
                            w.getDouble("maxY"),
                            w.getDouble("maxZ"),
                            w.getDouble("dirX"),
                            w.getDouble("dirY"),
                            w.getDouble("dirZ"),
                            w.getDouble("lift"),
                            w.getDouble("maxSpeed"))));
            } catch (IllegalArgumentException e) {
                ModLog.LOG.warn("Skipping a saved trial wind that no longer makes sense: {}", e.getMessage());
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Entry e : entries) {
            Wind u = e.wind;
            NBTTagCompound w = new NBTTagCompound();
            w.setInteger("dim", e.dimension);
            w.setDouble("minX", u.minX);
            w.setDouble("minY", u.minY);
            w.setDouble("minZ", u.minZ);
            w.setDouble("maxX", u.maxX);
            w.setDouble("maxY", u.maxY);
            w.setDouble("maxZ", u.maxZ);
            w.setDouble("dirX", u.dirX);
            w.setDouble("dirY", u.dirY);
            w.setDouble("dirZ", u.dirZ);
            w.setDouble("lift", u.lift);
            w.setDouble("maxSpeed", u.maxSpeed);
            list.appendTag(w);
        }
        tag.setTag("winds", list);
    }
}
