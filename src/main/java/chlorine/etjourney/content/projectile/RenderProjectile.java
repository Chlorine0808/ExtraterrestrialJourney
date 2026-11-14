package chlorine.etjourney.content.projectile;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A provisional look: the spec's vanilla item icon on a board that faces the camera, as snowballs are drawn. */
@SideOnly(Side.CLIENT)
public class RenderProjectile extends Render {

    private static final float MIN_SCALE = 0.5F;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        ProjectileSpec spec = ((EntityEtjProjectile) entity).spec();
        if (spec == null) return;
        IIcon icon = icon(spec.look);
        if (icon == null) return;
        float scale = Math.max(MIN_SCALE, spec.size);
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glScalef(scale, scale, scale);
        bindEntityTexture(entity);
        GL11.glRotatef(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setNormal(0.0F, 1.0F, 0.0F);
        t.addVertexWithUV(-0.5, -0.25, 0, icon.getMinU(), icon.getMaxV());
        t.addVertexWithUV(0.5, -0.25, 0, icon.getMaxU(), icon.getMaxV());
        t.addVertexWithUV(0.5, 0.75, 0, icon.getMaxU(), icon.getMinV());
        t.addVertexWithUV(-0.5, 0.75, 0, icon.getMinU(), icon.getMinV());
        t.draw();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    private static IIcon icon(String look) {
        Object item = Item.itemRegistry.getObject(look);
        return (item instanceof Item ? (Item) item : Items.snowball).getIconFromDamage(0);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationItemsTexture;
    }
}
