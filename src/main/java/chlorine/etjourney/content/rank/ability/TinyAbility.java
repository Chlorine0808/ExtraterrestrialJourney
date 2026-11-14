package chlorine.etjourney.content.rank.ability;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;

/** Drawn and hit as 0.6 times the size, with less health and a faster walk. */
public final class TinyAbility implements RankAbility {

    public static final String ID = "tiny";
    static final float SCALE = 0.6f;
    private static final UUID HEALTH = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b0d");
    private static final UUID SPEED = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b0e");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean clashes(String other) {
        return GiantAbility.ID.equals(other);
    }

    @Override
    public float renderScale() {
        return SCALE;
    }

    @Override
    public void onGive(EntityLiving self) {
        AttributeBoost.add(self, SharedMonsterAttributes.maxHealth, HEALTH, -0.3);
        AttributeBoost.add(self, SharedMonsterAttributes.movementSpeed, SPEED, 0.25);
        self.setHealth(self.getMaxHealth());
        Resize.apply(self, ID, SCALE);
    }

    @Override
    public void onJoin(EntityLiving self) {
        Resize.apply(self, ID, SCALE);
    }
}
