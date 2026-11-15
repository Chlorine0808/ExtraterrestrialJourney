package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** The mob rises after a target more than 1.5 blocks above it and takes no fall damage. */
public final class FlightAbility implements RankAbility {

    public static final String ID = "flight";
    static final double GAP = 1.5;
    private static final double LIFT = 0.22, PUSH = 0.04, TOP = 0.3;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onTick(EntityLiving self) {
        self.fallDistance = 0;
        EntityLivingBase target = Near.target(self);
        if (target == null || !target.isEntityAlive() || !rises(self.posY, target.posY)) return;
        self.motionY = Math.max(self.motionY, LIFT);
        double dx = target.posX - self.posX, dz = target.posZ - self.posZ, flat = Math.hypot(dx, dz);
        if (flat > 0.5 && Math.hypot(self.motionX, self.motionZ) < TOP) {
            self.motionX += dx / flat * PUSH;
            self.motionZ += dz / flat * PUSH;
        }
        if (self.ticksExisted % 4 == 0 && self.worldObj instanceof WorldServer)
            ((WorldServer) self.worldObj).func_147487_a("cloud", self.posX, self.posY, self.posZ, 2, 0.2, 0, 0.2, 0.01);
    }

    /** Whether a mob at selfY climbs toward a target at targetY. */
    static boolean rises(double selfY, double targetY) {
        return targetY > selfY + GAP;
    }
}
