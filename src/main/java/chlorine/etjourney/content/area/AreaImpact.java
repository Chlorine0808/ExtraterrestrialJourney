package chlorine.etjourney.content.area;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import chlorine.etjourney.content.projectile.EntityEtjProjectile;
import chlorine.etjourney.content.projectile.ImpactEffect;

/** Shots that leave an area where they land: on the block, under the creature, or where they ran out. */
public final class AreaImpact {

    private static final String LEFT = "etjourney.leftArea";
    /** How far back along its flight a shot steps off the face it hit before looking for ground. */
    private static final double STEP_BACK = 0.05;

    private AreaImpact() {}

    /** Leaves the area once per shot, owned by whoever fired it. */
    public static ImpactEffect leave(AreaSpec spec) {
        return new ImpactEffect() {

            @Override
            public void onEntity(EntityEtjProjectile shot, EntityLivingBase target, boolean hurt) {
                put(shot, target.posX, target.boundingBox.minY + target.height / 2, target.posZ);
            }

            @Override
            public void onBlock(EntityEtjProjectile shot, MovingObjectPosition hit) {
                double speed = Math
                    .sqrt(shot.motionX * shot.motionX + shot.motionY * shot.motionY + shot.motionZ * shot.motionZ);
                double back = speed > 0 ? STEP_BACK / speed : 0;
                put(
                    shot,
                    hit.hitVec.xCoord - shot.motionX * back,
                    hit.hitVec.yCoord - shot.motionY * back,
                    hit.hitVec.zCoord - shot.motionZ * back);
            }

            @Override
            public void onExpire(EntityEtjProjectile shot) {
                put(shot, shot.posX, shot.posY, shot.posZ);
            }

            private void put(EntityEtjProjectile shot, double x, double y, double z) {
                if (shot.getEntityData()
                    .getBoolean(LEFT)) return;
                shot.getEntityData()
                    .setBoolean(LEFT, true);
                Areas.placeOnGround(spec, shot.worldObj, shot.shooter(), x, y, z);
            }
        };
    }
}
