package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.MenderAbility;

/** mender: a hurt plain zombie beside the mender heals within a few seconds. */
final class MenderTrial extends AbilityTrial {

    private EntityLiving patient;
    private float hurt;

    MenderTrial() {
        super(MenderAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            c.spawn(4, 0, MenderAbility.ID);
            patient = c.plain(4, 2);
            return false;
        }
        if (tick == 2) {
            patient.setHealth(patient.getMaxHealth() / 2);
            hurt = patient.getHealth();
            return false;
        }
        if (tick < 90) return false;
        c.verdict(patient.getHealth() > hurt, name + ": the patient healed from " + hurt + " to " + patient.getHealth());
        return true;
    }
}
