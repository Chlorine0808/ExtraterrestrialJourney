package chlorine.etjourney.content.area;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Draws nothing: an area shows only by its particles, which it scatters itself. */
@SideOnly(Side.CLIENT)
public final class RenderArea extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {}

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
