package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

import chlorine.etjourney.core.combat.model.GateState;

/** Keeps each hook's gate state in its owner's entity data, so a player keeps it across saves but not death. */
public final class GateStore {

    static final String KEY = "etjourney.gates";

    private GateStore() {}

    public static GateState get(EntityLivingBase owner, String id) {
        NBTTagCompound all = owner.getEntityData()
            .getCompoundTag(KEY);
        if (!all.hasKey(id)) return GateState.FRESH;
        NBTTagCompound t = all.getCompoundTag(id);
        return new GateState(t.getInteger("spent"), t.getLong("since"), t.getLong("ready"), t.getLong("last"));
    }

    public static void put(EntityLivingBase owner, String id, GateState state) {
        NBTTagCompound data = owner.getEntityData();
        NBTTagCompound all = data.getCompoundTag(KEY);
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("spent", state.spent);
        t.setLong("since", state.since);
        t.setLong("ready", state.readyAt);
        t.setLong("last", state.lastFired);
        all.setTag(id, t);
        data.setTag(KEY, all);
    }
}
