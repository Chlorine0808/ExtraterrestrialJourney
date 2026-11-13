package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

/** A special ability a ranked mob may carry, apart from any one kind of mob; every hook runs on the server. */
public interface RankAbility {

    /** The name saved with the mob. */
    String id();

    default boolean fits(EntityLiving mob) {
        return true;
    }

    /** After a hit has survived every handler that might cancel it. */
    default void onHurt(EntityLiving self, DamageSource source) {}

    default void onDeath(EntityLiving self, DamageSource source) {}

    /** Once a second while the mob lives. */
    default void onSecond(EntityLiving self) {}
}
