package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.content.rank.RankStats;

/** On death the mob breaks into smaller unranked mobs of its kind. */
public final class SplitAbility implements RankAbility {

    public static final String ID = "split";
    private static final int CHILDREN = 2;
    /** Parts of health and attack a child loses. */
    private static final double HEALTH = 0.5, ATTACK = 0.25;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onDeath(EntityLiving self, DamageSource source) {
        for (int i = 0; i < CHILDREN; i++) {
            EntityLiving child = Offspring.sameKind(self);
            if (child == null) return;
            Offspring.bind(child, self, RankData.SPLIT_CHILD);
            double angle = 2 * Math.PI * i / CHILDREN;
            child.setLocationAndAngles(
                self.posX + 0.4 * Math.cos(angle),
                self.posY,
                self.posZ + 0.4 * Math.sin(angle),
                self.getRNG()
                    .nextFloat() * 360,
                0);
            RankStats.weaken(child, HEALTH, ATTACK);
            child.setAttackTarget(self.getAttackTarget());
            self.worldObj.spawnEntityInWorld(child);
        }
        Offspring.puff(self, "largesmoke", 12);
    }
}
