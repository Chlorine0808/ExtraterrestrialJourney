package chlorine.etjourney.world.end.debug.shoot;

import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;

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
}
