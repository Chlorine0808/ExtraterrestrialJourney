package chlorine.etjourney.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityAuraFX;
import net.minecraft.client.particle.EntityBreakingFX;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.particle.EntityPortalFX;
import net.minecraft.client.particle.EntitySnowShovelFX;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Spawns particles past vanilla's 16-block cut-off, for shots and areas seen from afar; client only. */
@SideOnly(Side.CLIENT)
public final class FarParticles {

    /** As far as shots and areas are tracked. */
    private static final double RANGE = 64;

    private FarParticles() {}

    /** Like World.spawnParticle; names it does not know go to vanilla, cut-off and all. */
    public static void spawn(World world, String name, double x, double y, double z, double vx, double vy, double vz) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity eye = mc.renderViewEntity;
        if (eye == null || mc.effectRenderer == null || eye.getDistanceSq(x, y, z) > RANGE * RANGE) return;
        // The same thinning as vanilla's particle setting: all, decreased (2 in 3) or minimal (none).
        int setting = mc.gameSettings.particleSetting;
        if (setting == 2 || setting == 1 && world.rand.nextInt(3) == 0) return;
        EntityFX fx = create(world, name, x, y, z, vx, vy, vz);
        if (fx != null) mc.effectRenderer.addEffect(fx);
        else world.spawnParticle(name, x, y, z, vx, vy, vz);
    }

    private static EntityFX create(World world, String name, double x, double y, double z, double vx, double vy,
        double vz) {
        switch (name) {
            case "portal":
                return new EntityPortalFX(world, x, y, z, vx, vy, vz);
            case "snowshovel":
                return new EntitySnowShovelFX(world, x, y, z, vx, vy, vz);
            case "townaura":
                return new EntityAuraFX(world, x, y, z, vx, vy, vz);
            case "flame":
                return new EntityFlameFX(world, x, y, z, vx, vy, vz);
            case "slime":
                return new EntityBreakingFX(world, x, y, z, Items.slime_ball);
            default:
                return null;
        }
    }
}
