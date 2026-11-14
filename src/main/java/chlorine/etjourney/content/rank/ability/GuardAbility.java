package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.core.damage.ElementalDamageSource;

/** Hits from the front, 60 degrees to each side of the body, are warded off; piercing and explosions get through. */
public final class GuardAbility implements RankAbility {

    public static final String ID = "guard";
    private static final double HALF_ARC = 60, EDGE_SLACK = 1e-6;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean wards(EntityLiving self, DamageSource source, float amount) {
        if (source.isExplosion()) return false;
        if (source instanceof ElementalDamageSource && ((ElementalDamageSource) source).damage()
            .isPiercing()) return false;
        Entity from = source.getSourceOfDamage() != null ? source.getSourceOfDamage() : source.getEntity();
        if (from == null || !inFront(self.renderYawOffset, from.posX - self.posX, from.posZ - self.posZ)) return false;
        self.worldObj.playSoundAtEntity(self, "random.anvil_land", 0.3f, 1.5f);
        Offspring.puff(self, "crit", 6);
        return true;
    }

    /** Whether a source at (dx, dz) from the mob lies within 60 degrees of where a body at that yaw faces. */
    static boolean inFront(double yawDegrees, double dx, double dz) {
        if (dx == 0 && dz == 0) return false;
        double toSource = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = ((yawDegrees - toSource) % 360 + 540) % 360 - 180;
        return Math.abs(diff) <= HALF_ARC + EDGE_SLACK;
    }
}
