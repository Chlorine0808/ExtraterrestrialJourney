package chlorine.etjourney.content.rank.ability;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;

/** Drawn and hit as 1.5 times the size, with half more health, knockback resistance and a slower walk. */
public final class GiantAbility implements RankAbility {

    public static final String ID = "giant";
    static final float SCALE = 1.5f;
    private static final UUID HEALTH = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b0a");
    private static final UUID KNOCKBACK = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b0b");
    private static final UUID SPEED = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b0c");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean clashes(String other) {
        return "tiny".equals(other);
    }

    @Override
    public float renderScale() {
        return SCALE;
    }

    @Override
    public void onGive(EntityLiving self) {
        AttributeBoost.add(self, SharedMonsterAttributes.maxHealth, HEALTH, 0.5);
        AttributeBoost.addAmount(self, SharedMonsterAttributes.knockbackResistance, KNOCKBACK, 0.3);
        AttributeBoost.add(self, SharedMonsterAttributes.movementSpeed, SPEED, -0.1);
        self.setHealth(self.getMaxHealth());
        Resize.apply(self, ID, SCALE);
    }

    @Override
    public void onJoin(EntityLiving self) {
        Resize.apply(self, ID, SCALE);
    }
}
