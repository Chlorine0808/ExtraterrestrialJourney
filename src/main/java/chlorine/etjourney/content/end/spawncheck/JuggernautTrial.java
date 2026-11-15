package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.JuggernautAbility;

/** juggernaut: health rises by twice the base and speed falls by 30% of it, against a rare mob without it. */
final class JuggernautTrial extends AbilityTrial {

    private EntityLiving with, without;

    JuggernautTrial() {
        super(JuggernautAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            with = c.spawn(Rank.RARE, 4, 0, JuggernautAbility.ID);
            without = c.spawn(Rank.RARE, -4, 0);
            return false;
        }
        check(c, SharedMonsterAttributes.maxHealth, 2.0);
        check(c, SharedMonsterAttributes.movementSpeed, -0.3);
        return true;
    }

    private void check(Ctx c, IAttribute attribute, double part) {
        double base = with.getEntityAttribute(attribute)
            .getBaseValue();
        double got = with.getEntityAttribute(attribute)
            .getAttributeValue()
            - without.getEntityAttribute(attribute)
                .getAttributeValue();
        c.verdict(
            Math.abs(got - base * part) < 0.001,
            name + ": " + attribute.getAttributeUnlocalizedName() + " " + got + ", want " + base * part);
    }
}
