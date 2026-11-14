package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.attributes.IAttribute;

import chlorine.etjourney.content.rank.Rank;

/** Checks an ability's attribute modifier against a rare mob that has none. */
abstract class ModifierTrial extends AbilityTrial {

    private final String id;
    private final IAttribute attribute;
    private final double part;
    private EntityLiving with, without;

    ModifierTrial(String id, IAttribute attribute, double part) {
        super(id);
        this.id = id;
        this.attribute = attribute;
        this.part = part;
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            with = c.spawn(Rank.RARE, 4, 0, id);
            without = c.spawn(Rank.RARE, -4, 0);
            return false;
        }
        double base = with.getEntityAttribute(attribute)
            .getBaseValue();
        double got = with.getEntityAttribute(attribute)
            .getAttributeValue()
            - without.getEntityAttribute(attribute)
                .getAttributeValue();
        c.verdict(Math.abs(got - base * part) < 0.001, id + ": attribute +" + got + ", want +" + base * part);
        return true;
    }
}
