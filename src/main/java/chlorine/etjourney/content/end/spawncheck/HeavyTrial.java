package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.SharedMonsterAttributes;

import chlorine.etjourney.content.rank.ability.HeavyAbility;

/** heavy: the attribute rises by the ability's part of the base. */
final class HeavyTrial extends ModifierTrial {

    HeavyTrial() {
        super(HeavyAbility.ID, SharedMonsterAttributes.attackDamage, 1.5);
    }
}
