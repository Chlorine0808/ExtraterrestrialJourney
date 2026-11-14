package chlorine.etjourney.content.area;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/** Where areas are put down. A null owner leaves an enemy area. */
public final class Areas {

    /** How far a floor looks down for ground. */
    private static final int DROP = 8;

    private Areas() {}

    /** Puts the area exactly there, even a floor in the air; null on the client. */
    public static EntityLingeringArea place(AreaSpec spec, World world, EntityLivingBase owner, double x, double y,
        double z) {
        if (world.isRemote) return null;
        EntityLingeringArea area = new EntityLingeringArea(world, spec, owner, x, y, z);
        world.spawnEntityInWorld(area);
        return area;
    }

    /**
     * Lays a floor on the first solid top within a few blocks below, or leaves none; a cloud stays where it is put.
     */
    public static EntityLingeringArea placeOnGround(AreaSpec spec, World world, EntityLivingBase owner, double x,
        double y, double z) {
        if (spec.shape == AreaShape.CLOUD) return place(spec, world, owner, x, y, z);
        double ground = ground(world, x, y, z);
        return Double.isNaN(ground) ? null : place(spec, world, owner, x, ground, z);
    }

    /** The top of the first block with a collision box at or below y, or NaN. */
    static double ground(World world, double x, double y, double z) {
        int bx = MathHelper.floor_double(x), bz = MathHelper.floor_double(z), top = MathHelper.floor_double(y);
        for (int by = top; by >= top - DROP && by >= 0; by--) {
            AxisAlignedBB box = world.getBlock(bx, by, bz)
                .getCollisionBoundingBoxFromPool(world, bx, by, bz);
            if (box != null) return box.maxY;
        }
        return Double.NaN;
    }
}
