package chlorine.etjourney.world.end.debug.shoot;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import chlorine.etjourney.world.end.shoot.Camera;

/** Server-side moves of the screenshot player: into the End, flying with night vision, and onto a camera. */
public final class CameraPlacement {

    private static final int END = 1;
    private static final int NIGHT_VISION_TICKS = 1_000_000;

    private CameraPlacement() {}

    public static boolean inEnd(EntityPlayerMP p) {
        return p.dimension == END;
    }

    public static void toEnd(EntityPlayerMP p) {
        if (!inEnd(p)) p.travelToDimension(END);
    }

    /** Lets the player fly and keeps them flying, and gives night vision so caves and the far side are lit. */
    public static void prepare(EntityPlayerMP p) {
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        p.addPotionEffect(new PotionEffect(Potion.nightVision.id, NIGHT_VISION_TICKS, 0, true));
    }

    public static void place(EntityPlayerMP p, Camera c) {
        p.playerNetServerHandler.setPlayerLocation(c.x, c.y, c.z, c.yaw, c.pitch);
    }
}
