package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;

/** What an accessory or affix does when its owner hits or is hit, in the shape of Apotheosis affixes. */
public interface CombatHook {

    /** Names the hook's gate state on its owner; equal ids share one cooldown. */
    String id();

    /** The owner's hit landed with an amount above 0. */
    default void onDamageDealt(EntityLivingBase owner, CombatHit hit) {}

    /** The owner is about to be hurt; the amount may be changed. */
    default void onHurt(EntityLivingBase owner, CombatHit hit) {}
}
