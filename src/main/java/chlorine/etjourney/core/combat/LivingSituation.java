package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import chlorine.etjourney.core.combat.model.Situation;
import chlorine.etjourney.core.damage.model.AttackProfile;

/** The situation of a living creature, for conditions. */
public final class LivingSituation implements Situation {

    private final EntityLivingBase living;
    private final AttackProfile attack;

    public LivingSituation(EntityLivingBase living, AttackProfile attack) {
        this.living = living;
        this.attack = attack == null ? AttackProfile.UNKNOWN : attack;
    }

    @Override
    public float healthFraction() {
        float max = living.getMaxHealth();
        return max <= 0 ? 0 : Math.min(1, living.getHealth() / max);
    }

    @Override
    public boolean isNight() {
        World world = living.worldObj;
        return !world.provider.hasNoSky && !world.isDaytime();
    }

    @Override
    public AttackProfile attack() {
        return attack;
    }
}
