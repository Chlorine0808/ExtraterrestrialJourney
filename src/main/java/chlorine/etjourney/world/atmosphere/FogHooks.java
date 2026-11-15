package chlorine.etjourney.world.atmosphere;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.world.end.EndTerrain;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/** Client: tints the End fog by the air around the camera and pulls it in where the air is dense. */
public final class FogHooks {

    private static final AtmosphereBlend.Easing EASING = new AtmosphereBlend.Easing();
    private static volatile Atmosphere current;
    private static long lastNanos;

    private FogHooks() {}

    public static void register() {
        if (!ETJConfig.biomeFog) return;
        FogHooks hooks = new FogHooks();
        MinecraftForge.EVENT_BUS.register(hooks);
        FMLCommonHandler.instance()
            .bus()
            .register(hooks);
    }

    /** The eased air of this frame, or null outside the End or before the first grid. */
    static Atmosphere current() {
        return current;
    }

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        Atmosphere air = step(event.entity);
        if (air == null || !inOpenAir(event)) return;
        Atmosphere vanilla = new Atmosphere(event.red, event.green, event.blue, 1, 0);
        // Re-blend with this frame's vanilla colour for the bare share; the eased mix carries the biome share.
        AtmosphereGrid grid = AtmosphereView.grid(EndTerrain.DIMENSION);
        Atmosphere now = AtmosphereBlend.at(grid, event.entity.posX, event.entity.posZ, vanilla);
        Atmosphere bare = AtmosphereBlend.at(grid, event.entity.posX, event.entity.posZ);
        event.red = air.r + now.r - bare.r;
        event.green = air.g + now.g - bare.g;
        event.blue = air.b + now.b - bare.b;
    }

    /** Runs before ShootHarness (normal priority), which pushes the fog away while shooting. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        Atmosphere air = current;
        if (air == null || air.density <= 1 || !inOpenAir(event)) return;
        GL11.glFogf(GL11.GL_FOG_START, FogMath.start(event.farPlaneDistance, air.density));
        GL11.glFogf(GL11.GL_FOG_END, FogMath.end(event.farPlaneDistance, air.density));
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        AtmosphereView.clear();
        EASING.reset();
        current = null;
    }

    /** Eases toward the mix at the camera once per frame; FogColors fires once per frame. */
    private static Atmosphere step(EntityLivingBase camera) {
        AtmosphereGrid grid = camera.dimension == EndTerrain.DIMENSION ? AtmosphereView.grid(EndTerrain.DIMENSION)
            : null;
        if (grid == null) {
            EASING.reset();
            return current = null;
        }
        long now = System.nanoTime();
        double seconds = lastNanos == 0 ? 0 : Math.min(1, (now - lastNanos) / 1e9);
        lastNanos = now;
        return current = EASING.step(AtmosphereBlend.at(grid, camera.posX, camera.posZ), seconds);
    }

    /** Water, lava and blindness keep vanilla's own fog. */
    private static boolean inOpenAir(EntityViewRenderEvent event) {
        if (event.block != null && event.block.getMaterial() != Material.air) return false;
        return !event.entity.isPotionActive(Potion.blindness);
    }
}
