package chlorine.etjourney.content.worm;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Draws a worm as a chain of textured cubes: the head, then each segment where it sits on the trail. */
@SideOnly(Side.CLIENT)
public class RenderWorm extends RenderLiving {

    private final ResourceLocation head, body, tail;

    public RenderWorm(ResourceLocation head, ResourceLocation body, ResourceLocation tail) {
        // An empty model: the living renderer still runs (name tags, render events) but the cubes are drawn here.
        super(new ModelBase() {}, 0.0F);
        this.head = head;
        this.body = body;
        this.tail = tail;
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        super.doRender(entity, x, y, z, yaw, partialTicks);
        EntityWorm worm = (EntityWorm) entity;
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        if (worm.hurtTime > 0 || worm.deathTime > 0) GL11.glColor4f(1.0F, 0.55F, 0.55F, 1.0F);
        else GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        double baseX = lerp(worm.lastTickPosX, worm.posX, partialTicks);
        double baseY = lerp(worm.lastTickPosY, worm.posY, partialTicks);
        double baseZ = lerp(worm.lastTickPosZ, worm.posZ, partialTicks);
        bindTexture(head);
        cube(
            x,
            y + worm.height / 2,
            z,
            lerpAngle(worm.prevRotationYaw, worm.rotationYaw, partialTicks),
            lerp(worm.prevRotationPitch, worm.rotationPitch, partialTicks),
            worm.height);
        for (Entity part : worm.getParts()) {
            WormSegment s = (WormSegment) part;
            bindTexture(s.tail ? tail : body);
            cube(
                x + lerp(s.prevPosX, s.posX, partialTicks) - baseX,
                y + lerp(s.prevPosY, s.posY, partialTicks) - baseY + s.height / 2,
                z + lerp(s.prevPosZ, s.posZ, partialTicks) - baseZ,
                lerpAngle(s.prevRotationYaw, s.rotationYaw, partialTicks),
                lerp(s.prevRotationPitch, s.rotationPitch, partialTicks),
                s.height);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    /** A cube of the given edge centred on (x, y, z), turned to the yaw and pitch, the texture on every face. */
    private static void cube(double x, double y, double z, double yaw, double pitch, double edge) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotated(-yaw, 0, 1, 0);
        GL11.glRotated(pitch, 1, 0, 0);
        double h = edge / 2;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        // -y, +y
        t.addVertexWithUV(-h, -h, -h, 0, 0);
        t.addVertexWithUV(h, -h, -h, 1, 0);
        t.addVertexWithUV(h, -h, h, 1, 1);
        t.addVertexWithUV(-h, -h, h, 0, 1);
        t.addVertexWithUV(-h, h, h, 0, 1);
        t.addVertexWithUV(h, h, h, 1, 1);
        t.addVertexWithUV(h, h, -h, 1, 0);
        t.addVertexWithUV(-h, h, -h, 0, 0);
        // -z, +z
        t.addVertexWithUV(-h, h, -h, 0, 0);
        t.addVertexWithUV(h, h, -h, 1, 0);
        t.addVertexWithUV(h, -h, -h, 1, 1);
        t.addVertexWithUV(-h, -h, -h, 0, 1);
        t.addVertexWithUV(-h, -h, h, 0, 1);
        t.addVertexWithUV(h, -h, h, 1, 1);
        t.addVertexWithUV(h, h, h, 1, 0);
        t.addVertexWithUV(-h, h, h, 0, 0);
        // -x, +x
        t.addVertexWithUV(-h, -h, -h, 0, 1);
        t.addVertexWithUV(-h, -h, h, 1, 1);
        t.addVertexWithUV(-h, h, h, 1, 0);
        t.addVertexWithUV(-h, h, -h, 0, 0);
        t.addVertexWithUV(h, h, -h, 0, 0);
        t.addVertexWithUV(h, h, h, 1, 0);
        t.addVertexWithUV(h, -h, h, 1, 1);
        t.addVertexWithUV(h, -h, -h, 0, 1);
        t.draw();
        GL11.glPopMatrix();
    }

    private static double lerp(double from, double to, float t) {
        return from + (to - from) * t;
    }

    private static double lerpAngle(float from, float to, float t) {
        float d = to - from;
        while (d < -180) d += 360;
        while (d >= 180) d -= 360;
        return from + d * t;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return head;
    }
}
