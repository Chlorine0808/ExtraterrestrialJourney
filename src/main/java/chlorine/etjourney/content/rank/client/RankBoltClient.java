package chlorine.etjourney.content.rank.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.effect.EntityLightningBolt;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Draws a bolt on the client's world; a client bolt starts no fire and does no damage. */
@SideOnly(Side.CLIENT)
public final class RankBoltClient {

    private RankBoltClient() {}

    /** Hops to the client thread, as the packet arrives on the network thread. */
    public static void strike(double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            WorldClient world = mc.theWorld;
            if (world != null) world.addWeatherEffect(new EntityLightningBolt(world, x, y, z));
        });
    }
}
