package chlorine.etjourney.content.projectile;

import chlorine.etjourney.content.entity.EntityRenderers;
import chlorine.etjourney.content.entity.ModEntities;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Registers the one shot entity and the table of shots. */
public final class ProjectileContent {

    public static final String SHOT = "shot";

    private ProjectileContent() {}

    public static void preInit() {
        ProjectileTypes.register();
        ModEntities.registerProjectile(SHOT, EntityEtjProjectile.class);
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        EntityRenderers.bind(EntityEtjProjectile.class, new RenderProjectile());
    }
}
