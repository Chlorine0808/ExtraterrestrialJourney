package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.SharedMonsterAttributes;

import chlorine.etjourney.content.rank.ability.BruteAbility;

/** brute: the attribute rises by the ability's part of the base. */
final class BruteTrial extends ModifierTrial {

    BruteTrial() {
        super(BruteAbility.ID, SharedMonsterAttributes.attackDamage, 1.0);
    }
}
