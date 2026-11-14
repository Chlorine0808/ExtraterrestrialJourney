package chlorine.etjourney.content.rank.ability;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;

/** Triples the mob's base health and slows it by 30%. */
public final class JuggernautAbility implements RankAbility {

    public static final String ID = "juggernaut";
    private static final UUID HEALTH = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b03"),
        SPEED = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b04");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onGive(EntityLiving self) {
        AttributeBoost.add(self, SharedMonsterAttributes.maxHealth, HEALTH, 2.0);
        AttributeBoost.add(self, SharedMonsterAttributes.movementSpeed, SPEED, -0.3);
        self.setHealth(self.getMaxHealth());
    }
}
