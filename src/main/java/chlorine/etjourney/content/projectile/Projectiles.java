package chlorine.etjourney.content.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/** The way to fire shots; an enemy, a player or nothing at all may be the shooter. */
public final class Projectiles {

    private Projectiles() {}

    /** Fires at a point by the spec's aim; target, if given, is what a homing shot chases. */
    public static EntityEtjProjectile shoot(ProjectileSpec spec, World world, EntityLivingBase shooter, double x,
        double y, double z, double tx, double ty, double tz, Entity target) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        double[] v = spec.aim == Aim.LOB ? Aim.lob(dx, dy, dz, spec.arcHeight, spec.gravity)
            : Aim.straight(dx, dy, dz, spec.speed);
        return spawn(spec, world, shooter, x, y, z, v, target);
    }

    /** Fires at an entity's middle. */
    public static EntityEtjProjectile shootAt(ProjectileSpec spec, World world, EntityLivingBase shooter, double x,
        double y, double z, Entity target) {
        return shoot(spec, world, shooter, x, y, z, target.posX, target.posY + target.height / 2, target.posZ, target);
    }

    /** Fires along a direction at the spec's speed. */
    public static EntityEtjProjectile shootAlong(ProjectileSpec spec, World world, EntityLivingBase shooter, double x,
        double y, double z, double[] dir) {
        return spawn(spec, world, shooter, x, y, z, Aim.straight(dir[0], dir[1], dir[2], spec.speed), null);
    }

    /** Fires count shots at random within halfAngle radians of dir. */
    public static List<EntityEtjProjectile> scatter(ProjectileSpec spec, World world, EntityLivingBase shooter,
        double x, double y, double z, double[] dir, int count, double halfAngle, Random random) {
        List<EntityEtjProjectile> out = new ArrayList<>();
        for (int i = 0; i < count; i++)
            out.add(shootAlong(spec, world, shooter, x, y, z, Spread.cone(dir, halfAngle, random)));
        return out;
    }

    private static EntityEtjProjectile spawn(ProjectileSpec spec, World world, EntityLivingBase shooter, double x,
        double y, double z, double[] v, Entity target) {
        EntityEtjProjectile shot = new EntityEtjProjectile(world, spec, shooter, x, y, z, v, target);
        world.spawnEntityInWorld(shot);
        return shot;
    }
}
