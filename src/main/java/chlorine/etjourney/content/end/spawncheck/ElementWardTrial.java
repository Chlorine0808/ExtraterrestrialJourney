package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/** An ability set to fire: a fire hit is warded, an ice hit hurts. */
abstract class ElementWardTrial extends AbilityTrial {

    private final Rank rank;

    ElementWardTrial(String id, Rank rank) {
        super(id);
        this.rank = rank;
    }

    static ElementalDamageSource hit(Ctx c, Element element) {
        return new ElementalDamageSource(
            ElementalDamage.builder(DamageKind.MAGIC)
                .add(element, 5)
                .build(),
            c.player,
            c.player);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick != 1) return false;
        EntityLiving mob = c.spawn(rank, 4, 0, name + ":fire");
        mob.setHealth(mob.getMaxHealth() / 2);
        float before = mob.getHealth();
        mob.attackEntityFrom(hit(c, Elements.FIRE), 5);
        c.verdict(
            mob.getHealth() >= before,
            name + ": the fire hit did no harm (" + before + " -> " + mob.getHealth() + ")");
        float after = mob.getHealth();
        mob.attackEntityFrom(hit(c, Elements.ICE), 5);
        c.verdict(mob.getHealth() < after, name + ": the ice hit hurt (" + after + " -> " + mob.getHealth() + ")");
        return true;
    }
}
