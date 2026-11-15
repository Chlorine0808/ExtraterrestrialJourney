package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;

/** A special ability a ranked mob may carry, apart from any one kind of mob; every hook runs on the server. */
public interface RankAbility {

    /** The name saved with the mob. */
    String id();

    default boolean fits(EntityLiving mob) {
        return true;
    }

    /** The lowest rank that may roll the ability. */
    default Rank least() {
        return Rank.RARE;
    }

    /** Whether the two may not roll together; checked both ways round. */
    default boolean clashes(String other) {
        return false;
    }

    /** How much larger the mob is drawn; the factors of all its abilities multiply. */
    default float renderScale() {
        return 1;
    }

    /** Once, when the rank is given; attribute modifiers set here stay with the mob. */
    default void onGive(EntityLiving self) {}

    /** Each time the mob joins a world, a reload included. */
    default void onJoin(EntityLiving self) {}

    /** Before the hit lands; true wards it off. */
    default boolean wards(EntityLiving self, DamageSource source, float amount) {
        return false;
    }

    /** The amount the mob takes from a hit that got through; called before onHurt. */
    default float takes(EntityLiving self, DamageSource source, float amount) {
        return amount;
    }

    /** After a hit has survived every handler that might cancel it. */
    default void onHurt(EntityLiving self, DamageSource source) {}

    /** Before the mob's own hit lands; false holds it back. */
    default boolean swings(EntityLiving self, EntityLivingBase target, DamageSource source) {
        return true;
    }

    /** After the mob's own hit got through. */
    default void onStrike(EntityLiving self, EntityLivingBase target, DamageSource source, float amount) {}

    /** True keeps the mob alive; it must leave the mob with health. */
    default boolean cheatsDeath(EntityLiving self, DamageSource source) {
        return false;
    }

    default void onDeath(EntityLiving self, DamageSource source) {}

    /** Every tick while the mob lives. */
    default void onTick(EntityLiving self) {}

    /** Once a second while the mob lives. */
    default void onSecond(EntityLiving self) {}
}
