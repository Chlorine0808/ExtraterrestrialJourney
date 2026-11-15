package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.Rank;

/** A summon of six weak minions of another kind, silverfish unless SummonAbility.hordeOf says otherwise. */
public final class HordeAbility extends SummonAbility {

    public static final String ID = "horde";

    public HordeAbility() {
        super(ID, 400, 6, 8, 0.75);
    }

    @Override
    public Rank least() {
        return Rank.EPIC;
    }

    @Override
    protected EntityLiving make(EntityLiving parent) {
        return hordeMinion(parent);
    }
}
