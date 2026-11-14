package chlorine.etjourney.core.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import chlorine.etjourney.core.status.model.StubbornMarks;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Keeps milk-proof effects milk-proof across saves, which drop an effect's curative items. */
public final class StubbornEffects {

    static final String TAG = "etjourney.stubborn";

    private StubbornEffects() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new StubbornEffects());
    }

    /** Empties the effect's curative items and marks it until it ends. */
    static void makeStubborn(EntityLivingBase living, PotionEffect active) {
        active.setCurativeItems(new ArrayList<>());
        Map<Integer, Long> marks = read(living);
        StubbornMarks.mark(marks, active.getPotionID(), now(living) + active.getDuration());
        write(living, marks);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.world.isRemote || !(event.entity instanceof EntityLivingBase)) return;
        EntityLivingBase living = (EntityLivingBase) event.entity;
        // Skip the many creatures without effects, so none gains an empty ForgeData tag.
        if (living.getActivePotionEffects()
            .isEmpty()) return;
        Map<Integer, Long> marks = StubbornMarks.live(read(living), now(living));
        for (int id : marks.keySet()) {
            Potion potion = id < Potion.potionTypes.length ? Potion.potionTypes[id] : null;
            PotionEffect active = potion == null ? null : living.getActivePotionEffect(potion);
            if (active != null) active.setCurativeItems(new ArrayList<>());
        }
        write(living, marks);
    }

    private static long now(EntityLivingBase living) {
        return living.worldObj.getTotalWorldTime();
    }

    private static Map<Integer, Long> read(EntityLivingBase living) {
        Map<Integer, Long> marks = new HashMap<>();
        NBTTagCompound tag = living.getEntityData()
            .getCompoundTag(TAG);
        for (Object key : tag.func_150296_c()) marks.put(Integer.valueOf((String) key), tag.getLong((String) key));
        return marks;
    }

    private static void write(EntityLivingBase living, Map<Integer, Long> marks) {
        NBTTagCompound data = living.getEntityData();
        if (marks.isEmpty()) {
            data.removeTag(TAG);
            return;
        }
        NBTTagCompound tag = new NBTTagCompound();
        for (Map.Entry<Integer, Long> e : marks.entrySet()) tag.setLong(String.valueOf(e.getKey()), e.getValue());
        data.setTag(TAG, tag);
    }
}
