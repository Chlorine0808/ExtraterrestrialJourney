package chlorine.etjourney.world.end.debug.shoot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** Client-side capture helpers for the screenshot run. */
public final class ShootCapture {

    private ShootCapture() {}

    /** Resizes the game framebuffer to w by h, larger than the window if need be, through the private resize. */
    public static void enlarge(Minecraft mc, int w, int h) {
        Method resize = ReflectionHelper
            .findMethod(Minecraft.class, mc, new String[] { "resize", "func_71370_a" }, int.class, int.class);
        try {
            resize.invoke(mc, w, h);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Minecraft#resize failed", e);
        }
    }

    /**
     * The game framebuffer as ARGB rows from the top, framebufferWidth by framebufferHeight. Reads the colour
     * texture the way ScreenShotHelper does, so the image is not limited to the window.
     */
    public static int[] grab(Minecraft mc) {
        if (!OpenGlHelper.isFramebufferEnabled()) throw new IllegalStateException("framebuffers are off");
        Framebuffer fb = mc.getFramebuffer();
        int tw = fb.framebufferTextureWidth, th = fb.framebufferTextureHeight;
        int w = fb.framebufferWidth, h = fb.framebufferHeight;
        IntBuffer buf = BufferUtils.createIntBuffer(tw * th);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fb.framebufferTexture);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
        int[] tex = new int[tw * th];
        buf.get(tex);
        // GL rows run from the bottom; the picture is the bottom-left w x h of the texture.
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) System.arraycopy(tex, (h - 1 - y) * tw, out, y * w, w);
        return out;
    }

    public static void write(int[] argb, int w, int h, File png) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, w, h, argb, 0, w);
        try {
            if (!ImageIO.write(image, "png", png)) throw new IOException("no PNG writer");
        } catch (IOException e) {
            throw new IllegalStateException("cannot write " + png, e);
        }
    }
}
