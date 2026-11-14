package chlorine.etjourney.content.area;

import chlorine.etjourney.content.entity.EntityRenderers;
import chlorine.etjourney.content.entity.ModEntities;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Registers the one area entity, the table of areas and the slow they hold creatures at. */
public final class AreaContent {

    public static final String AREA = "lingering_area";

    private AreaContent() {}

    /** After the projectile table, which the spore pod joins. */
    public static void preInit() {
        AreaTypes.register();
        ModEntities.registerProjectile(AREA, EntityLingeringArea.class);
        AreaSlow.register();
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        EntityRenderers.bind(EntityLingeringArea.class, new RenderArea());
    }
}
