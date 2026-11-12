package chlorine.etjourney.content.rank.ability;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Struck by a living attacker, the mob blinks to a nearby spot it can stand on, then waits. */
public final class WarpAbility implements RankAbility {

    public static final String ID = "warp";
    static final int COOLDOWN = 100;
    private static final int TRIES = 16, REACH = 8, LEAST = 3, RISE = 4, FALL = 8;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onHurt(EntityLiving self, DamageSource source) {
        if (!(source.getEntity() instanceof EntityLivingBase)) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        Random random = self.getRNG();
        for (int i = 0; i < TRIES; i++) {
            double dx = offset(random), dz = offset(random);
            if (dx * dx + dz * dz < LEAST * LEAST) continue;
            if (blink(self, self.posX + dx, self.posY + random.nextInt(2 * RISE + 1) - RISE, self.posZ + dz)) {
                RankData.markUsed(self, ID, now);
                return;
            }
        }
    }

    private static double offset(Random random) {
        return (random.nextDouble() * 2 - 1) * REACH;
    }

    /** Drops the target onto the first solid block within FALL below it, as an enderman's teleport does. */
    private static boolean blink(EntityLiving self, double x, double y, double z) {
        int bx = MathHelper.floor_double(x), by = MathHelper.floor_double(y), bz = MathHelper.floor_double(z);
        if (!self.worldObj.blockExists(bx, by, bz)) return false;
        int floor = by;
        while (floor > by - FALL && floor > 1
            && !self.worldObj.getBlock(bx, floor - 1, bz)
                .getMaterial()
                .blocksMovement())
            floor--;
        if (!self.worldObj.getBlock(bx, floor - 1, bz)
            .getMaterial()
            .blocksMovement()) return false;
        double ox = self.posX, oy = self.posY, oz = self.posZ;
        self.setPosition(x, floor, z);
        if (!self.worldObj.getCollidingBoundingBoxes(self, self.boundingBox)
            .isEmpty() || self.worldObj.isAnyLiquid(self.boundingBox)) {
            self.setPosition(ox, oy, oz);
            return false;
        }
        self.worldObj.playSoundEffect(ox, oy, oz, "mob.endermen.portal", 1.0F, 1.0F);
        self.playSound("mob.endermen.portal", 1.0F, 1.0F);
        Offspring.puff(self, "portal", 32);
        return true;
    }
}
