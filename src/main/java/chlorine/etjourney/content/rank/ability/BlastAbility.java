package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** When it is killed, the mob smokes for 1.5 seconds, then bursts and hurts every living thing within 4 blocks. */
public final class BlastAbility implements RankAbility {

    public static final String ID = "blast";
    static final int FUSE = 30;
    static final double RADIUS = 4, MAX = 10, PUSH = 0.8, LIFT = 0.3;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onDeath(EntityLiving self, DamageSource source) {
        World world = self.worldObj;
        double x = self.posX, y = self.posY + self.height / 2, z = self.posZ;
        world.playSoundEffect(x, y, z, "creeper.primed", 1.0F, 0.5F);
        Warning.at(world, "smoke", x, y, z, FUSE, () -> burst(world, self, x, y, z));
    }

    /** What the blast deals at that distance from its centre: MAX there, none from RADIUS out. */
    static float damage(double distance) {
        return distance >= RADIUS ? 0 : (float) (MAX * (1 - distance / RADIUS));
    }

    /** No vanilla Explosion, so no block breaks and no item is lost; only living things are hurt and pushed. */
    private static void burst(World world, EntityLiving self, double x, double y, double z) {
        world.playSoundEffect(x, y, z, "random.explode", 4.0F, 0.9F);
        if (world instanceof WorldServer) ((WorldServer) world).func_147487_a("hugeexplosion", x, y, z, 1, 0, 0, 0, 0);
        AxisAlignedBB box = AxisAlignedBB
            .getBoundingBox(x - RADIUS, y - RADIUS, z - RADIUS, x + RADIUS, y + RADIUS, z + RADIUS);
        for (Object o : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            EntityLivingBase e = (EntityLivingBase) o;
            if (e == self || !e.isEntityAlive()) continue;
            double dx = e.posX - x, dy = e.posY + e.height / 2 - y, dz = e.posZ - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float amount = damage(distance);
            if (amount <= 0) continue;
            e.attackEntityFrom(new DamageSource("explosion").setExplosion(), amount);
            push(e, dx, dz, amount / MAX);
        }
    }

    /** Away from the centre along the ground, and a little up, by the share of the full blast the hit carried. */
    private static void push(EntityLivingBase e, double dx, double dz, double share) {
        double flat = Math.hypot(dx, dz);
        if (flat > 1e-6) {
            e.motionX += dx / flat * PUSH * share;
            e.motionZ += dz / flat * PUSH * share;
        }
        e.motionY += LIFT * share;
        e.velocityChanged = true;
    }
}
