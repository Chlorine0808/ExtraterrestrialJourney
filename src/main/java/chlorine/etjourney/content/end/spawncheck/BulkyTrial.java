package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.SharedMonsterAttributes;

import chlorine.etjourney.content.rank.ability.BulkyAbility;

/** bulky: the attribute rises by the ability's part of the base. */
final class BulkyTrial extends ModifierTrial {

    BulkyTrial() {
        super(BulkyAbility.ID, SharedMonsterAttributes.maxHealth, 1.0);
    }
}
