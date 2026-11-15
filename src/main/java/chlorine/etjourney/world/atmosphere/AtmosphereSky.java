package chlorine.etjourney.world.atmosphere;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import chlorine.etjourney.world.end.EndTerrain;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** The vanilla End sky box, then the same box in the fog colour at the air's sky share on top. */
final class AtmosphereSky extends IRenderHandler {

    private static final ResourceLocation END_SKY = new ResourceLocation("textures/environment/end_sky.png");
    private static final int VANILLA_TINT = 0x282828;

    static void register() {
        MinecraftForge.EVENT_BUS.register(new Installer());
    }

    /** Client worlds are made anew on every join and dimension change, so the sky goes on each End load. */
    public static final class Installer {

        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            if (event.world.isRemote && event.world.provider.dimensionId == EndTerrain.DIMENSION)
                event.world.provider.setSkyRenderer(new AtmosphereSky());
        }
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        RenderHelper.disableStandardItemLighting();
        GL11.glDepthMask(false);
        mc.renderEngine.bindTexture(END_SKY);
        drawBox(true, VANILLA_TINT, 255);

        Atmosphere air = FogHooks.current();
        if (air != null && air.skyMix > 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            int rgb = (int) (air.r * 255) << 16 | (int) (air.g * 255) << 8 | (int) (air.b * 255);
            drawBox(false, rgb, Math.round(air.skyMix * 255));
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    /** Six faces of a 200-block cube around the camera, as RenderGlobal#renderSky draws the End. */
    private static void drawBox(boolean textured, int rgb, int alpha) {
        Tessellator t = Tessellator.instance;
        for (int i = 0; i < 6; ++i) {
            GL11.glPushMatrix();
            if (i == 1) GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            if (i == 2) GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            if (i == 3) GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
            if (i == 4) GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
            if (i == 5) GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);
            t.startDrawingQuads();
            t.setColorRGBA_I(rgb, alpha);
            if (textured) {
                t.addVertexWithUV(-100.0D, -100.0D, -100.0D, 0.0D, 0.0D);
                t.addVertexWithUV(-100.0D, -100.0D, 100.0D, 0.0D, 16.0D);
                t.addVertexWithUV(100.0D, -100.0D, 100.0D, 16.0D, 16.0D);
                t.addVertexWithUV(100.0D, -100.0D, -100.0D, 16.0D, 0.0D);
            } else {
                t.addVertex(-100.0D, -100.0D, -100.0D);
                t.addVertex(-100.0D, -100.0D, 100.0D);
                t.addVertex(100.0D, -100.0D, 100.0D);
                t.addVertex(100.0D, -100.0D, -100.0D);
            }
            t.draw();
            GL11.glPopMatrix();
        }
    }
}
