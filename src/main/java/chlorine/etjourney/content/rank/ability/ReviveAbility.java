package chlorine.etjourney.content.rank.ability;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.content.rank.RankTasks;

/** Once, when it is killed, the mob drops nothing and gives no experience, then rises again 5 seconds later. */
public final class ReviveAbility implements RankAbility {

    public static final String ID = "revive";
    static final int DELAY = 100;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Rank least() {
        return Rank.EPIC;
    }

    @Override
    public boolean clashes(String other) {
        return SplitAbility.ID.equals(other);
    }

    @Override
    public void onDeath(EntityLiving self, DamageSource source) {
        if (!(self.worldObj instanceof WorldServer)) return;
        NBTTagCompound tag = new NBTTagCompound();
        if (!self.writeToNBTOptional(tag)) return;
        // Without its UUID the copy gets a fresh one, so it never clashes with the corpse.
        tag.removeTag("UUIDMost");
        tag.removeTag("UUIDLeast");
        WorldServer world = (WorldServer) self.worldObj;
        double x = self.posX, y = self.posY, z = self.posZ;
        RankData.markNoLoot(self);
        RankTasks.later(DELAY, () -> rise(world, tag, x, y, z));
    }

    /** Stands the copy up where the mob fell; nothing happens if that world or place is no longer loaded. */
    private static void rise(WorldServer world, NBTTagCompound tag, double x, double y, double z) {
        if (DimensionManager.getWorld(world.provider.dimensionId) != world) return;
        if (!world.getChunkProvider()
            .chunkExists(MathHelper.floor_double(x) >> 4, MathHelper.floor_double(z) >> 4)) return;
        Entity made = EntityList.createEntityFromNBT(tag, world);
        if (!(made instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving) made;
        mob.deathTime = 0;
        mob.hurtTime = 0;
        mob.clearActivePotions();
        mob.extinguish();
        mob.setHealth(mob.getMaxHealth());
        List<String> left = new ArrayList<>(RankData.abilities(mob));
        left.remove(ID);
        RankData.set(mob, RankData.rank(mob), left);
        RankData.clearPaid(mob);
        world.spawnEntityInWorld(mob);
        Offspring.puff(mob, "largesmoke", 24);
        mob.playSound("mob.zombie.unfect", 1.0F, 1.0F);
    }
}
