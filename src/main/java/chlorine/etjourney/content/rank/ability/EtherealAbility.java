package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.ElementRegistry;

/** Physical damage is warded off. */
public final class EtherealAbility implements RankAbility {

    public static final String ID = "ethereal";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Rank least() {
        return Rank.LEGENDARY;
    }

    @Override
    public boolean clashes(String other) {
        return MagicWardAbility.ID.equals(other);
    }

    @Override
    public boolean wards(EntityLiving self, DamageSource source, float amount) {
        boolean elemental = source instanceof ElementalDamageSource;
        boolean plain = elemental && ((ElementalDamageSource) source).damage()
            .kind() == DamageKind.PHYSICAL
            && ((ElementalDamageSource) source).damage()
                .dominant()
                .equals(ElementRegistry.NEUTRAL);
        return isPhysical(
            source.isMagicDamage(),
            source.isFireDamage(),
            source.isExplosion(),
            source.isUnblockable(),
            elemental,
            plain);
    }

    /** An elemental hit counts only when it is physical and neutral; the rest is told by the vanilla flags. */
    static boolean isPhysical(boolean magic, boolean fire, boolean explosion, boolean unblockable, boolean elemental,
        boolean plainPhysical) {
        if (magic || fire || explosion || unblockable) return false;
        return !elemental || plainPhysical;
    }
}
