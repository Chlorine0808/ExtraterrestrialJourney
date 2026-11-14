package chlorine.etjourney.content.rank.ability;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;

/** Doubles the mob's base attack. */
public final class BruteAbility implements RankAbility {

    public static final String ID = "brute";
    private static final UUID MODIFIER = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b02");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onGive(EntityLiving self) {
        AttributeBoost.add(self, SharedMonsterAttributes.attackDamage, MODIFIER, 1.0);
    }
}
