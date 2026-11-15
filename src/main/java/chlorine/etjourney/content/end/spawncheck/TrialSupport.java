package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import chlorine.etjourney.content.projectile.EntityEtjProjectile;

/** What several ability trials need: spotting a shot in flight and keeping a mob from fighting by itself. */
final class TrialSupport {

    private TrialSupport() {}

    /** Whether a shot of that spec is in the world now. */
    static boolean shotExists(World world, String specName) {
        for (Object o : world.loadedEntityList)
            if (o instanceof EntityEtjProjectile && ((EntityEtjProjectile) o).spec() != null
                && ((EntityEtjProjectile) o).spec().name.equals(specName)) return true;
        return false;
    }

    /** The mob keeps its target but its blows do nothing, so health lost is the ability's. */
    static void harmless(EntityLiving mob) {
        mob.getEntityAttribute(SharedMonsterAttributes.attackDamage)
            .setBaseValue(0);
    }
}
