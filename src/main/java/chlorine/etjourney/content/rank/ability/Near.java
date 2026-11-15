package chlorine.etjourney.content.rank.ability;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;

/** What is around a ranked mob that an ability may act on. */
final class Near {

    private Near() {}

    /** What the mob fights: its attack target, else whoever last hit it. */
    static EntityLivingBase target(EntityLiving self) {
        return self.getAttackTarget() != null ? self.getAttackTarget() : self.getAITarget();
    }

    /** The target when it is a living player within reach that the game lets mobs hurt. */
    static EntityPlayer playerTarget(EntityLiving self, double reach) {
        EntityLivingBase t = target(self);
        if (!(t instanceof EntityPlayer) || !t.isEntityAlive() || ((EntityPlayer) t).capabilities.disableDamage)
            return null;
        return self.getDistanceSqToEntity(t) <= reach * reach ? (EntityPlayer) t : null;
    }

    /** Living players within the radius that the game lets mobs hurt. */
    static List<EntityPlayer> players(EntityLiving self, double radius) {
        List<EntityPlayer> out = new ArrayList<>();
        for (Object o : self.worldObj.playerEntities) {
            EntityPlayer p = (EntityPlayer) o;
            if (p.isEntityAlive() && !p.capabilities.disableDamage && self.getDistanceSqToEntity(p) <= radius * radius)
                out.add(p);
        }
        return out;
    }

    /** Other living hostile mobs within the radius, bosses left out. */
    static List<EntityLiving> hostiles(EntityLiving self, double radius) {
        List<EntityLiving> out = new ArrayList<>();
        for (Object o : self.worldObj
            .getEntitiesWithinAABB(EntityLiving.class, self.boundingBox.expand(radius, radius, radius))) {
            EntityLiving e = (EntityLiving) o;
            if (e != self && e.isEntityAlive()
                && e instanceof IMob
                && !(e instanceof IBossDisplayData)
                && self.getDistanceSqToEntity(e) <= radius * radius) out.add(e);
        }
        return out;
    }
}
