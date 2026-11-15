package chlorine.etjourney.content.end.spawncheck;

import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;

import chlorine.etjourney.content.end.EntitySporeZombie;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.content.rank.ability.ReviveAbility;

/** revive: a killed mob leaves no items, and a new ranked one without the ability stands where it fell. */
final class ReviveTrial extends AbilityTrial {

    private EntityLiving mob;
    private double x, y, z;

    ReviveTrial() {
        super(ReviveAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(Rank.EPIC, 4, 0, ReviveAbility.ID);
            x = mob.posX;
            y = mob.posY;
            z = mob.posZ;
            mob.attackEntityFrom(c.byPlayer(), 1000);
            return false;
        }
        if (tick == 50) {
            List<?> items = c.end.getEntitiesWithinAABB(EntityItem.class, box(3));
            c.verdict(items.isEmpty(), name + ": " + items.size() + " items around the corpse");
            return false;
        }
        if (tick < 110) return false;
        EntitySporeZombie back = null;
        for (Object o : c.end.getEntitiesWithinAABB(EntitySporeZombie.class, box(3))) {
            EntitySporeZombie e = (EntitySporeZombie) o;
            if (e != mob && e.isEntityAlive()) back = e;
        }
        if (back == null) {
            c.verdict(false, name + ": nothing rose where the mob fell");
            return true;
        }
        c.made.add(back);
        c.verdict(
            RankData.ranked(back) && !RankData.abilities(back)
                .contains(ReviveAbility.ID),
            name + ": a ranked mob rose without the revive ability");
        return true;
    }

    private AxisAlignedBB box(double r) {
        return AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r + 2, z + r);
    }
}
