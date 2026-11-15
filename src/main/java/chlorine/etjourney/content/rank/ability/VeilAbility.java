package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.core.damage.model.ElementRegistry;

/** Neutral hits, vanilla ones included, hurt twice as much; every other element only a quarter. */
public final class VeilAbility implements RankAbility {

    public static final String ID = "veil";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Rank least() {
        return Rank.EPIC;
    }

    @Override
    public float takes(EntityLiving self, DamageSource source, float amount) {
        return scaled(
            HitElement.of(source)
                .equals(ElementRegistry.NEUTRAL),
            amount);
    }

    static float scaled(boolean neutral, float amount) {
        return neutral ? amount * 2 : amount * 0.25f;
    }
}
