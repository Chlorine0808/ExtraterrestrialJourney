package chlorine.etjourney.content.craft;

import java.util.List;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ClientRegistry;

/** Client only: draws the items a device holds, spinning slowly above its top face. */
public final class CraftRenderers extends TileEntitySpecialRenderer {

    /** Several items spread on a small ring so the Forge's pile stays readable. */
    private static final double RING = 0.25;

    private CraftRenderers() {}

    public static void init() {
        CraftRenderers renderer = new CraftRenderers();
        ClientRegistry.bindTileEntitySpecialRenderer(ForgeTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(AltarTile.class, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(PedestalTile.class, renderer);
    }

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        List<ItemStack> items = ((ItemHolderTile) tile).items();
        if (items.isEmpty()) return;
        float spin = (tile.getWorldObj()
            .getTotalWorldTime() + partialTicks) * 2.0F;
        // The dispatcher lights a tile by its own block, which is opaque and so always dark; light the items by the
        // air they float in instead.
        int light = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(tile.xCoord, tile.yCoord + 1, tile.zCoord, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        boolean wasInFrame = RenderItem.renderInFrame;
        RenderItem.renderInFrame = true;
        for (int i = 0; i < items.size(); i++) {
            double angle = 2 * Math.PI * i / items.size();
            double r = items.size() == 1 ? 0 : RING;
            EntityItem entity = new EntityItem(tile.getWorldObj(), 0, 0, 0, items.get(i));
            entity.hoverStart = 0;
            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5 + r * Math.cos(angle), y + 1.15, z + 0.5 + r * Math.sin(angle));
            GL11.glRotatef(spin, 0, 1, 0);
            RenderManager.instance.renderEntityWithPosYaw(entity, 0, 0, 0, 0, 0);
            GL11.glPopMatrix();
        }
        RenderItem.renderInFrame = wasInFrame;
    }
}
