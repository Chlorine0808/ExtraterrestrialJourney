package chlorine.etjourney.core.damage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/** The way in for elemental attacks. */
public final class ElementalDamages {

    private ElementalDamages() {}

    /** Whether piercing damage may get past the target's immunities: never creative players or invulnerables. */
    public static boolean pierces(EntityLivingBase target) {
        if (target.isEntityInvulnerable()) return false;
        return !(target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.disableDamage);
    }
}
