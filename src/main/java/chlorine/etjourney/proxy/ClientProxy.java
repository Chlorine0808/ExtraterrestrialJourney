package chlorine.etjourney.proxy;

import org.lwjgl.opengl.Display;

import chlorine.etjourney.content.area.AreaContent;
import chlorine.etjourney.content.area.check.AreaCheck;
import chlorine.etjourney.content.craft.CraftRenderers;
import chlorine.etjourney.content.end.EndEntityRenderers;
import chlorine.etjourney.content.end.FurRenderer;
import chlorine.etjourney.content.end.spawncheck.SpawnCheck;
import chlorine.etjourney.content.end.wormcheck.WormCheck;
import chlorine.etjourney.content.entity.EntityRenderers;
import chlorine.etjourney.content.projectile.ProjectileContent;
import chlorine.etjourney.content.projectile.check.ProjectileCheck;
import chlorine.etjourney.content.rank.client.RankClient;
import chlorine.etjourney.core.growth.GrowthTooltip;
import chlorine.etjourney.core.util.WindowTitle;
import chlorine.etjourney.world.atmosphere.FogHooks;
import chlorine.etjourney.world.end.debug.damagecheck.DamageCheck;
import chlorine.etjourney.world.end.debug.shoot.ShootHarness;
import chlorine.etjourney.world.wind.WindPush;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Client-only registration such as renderers. */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        EndEntityRenderers.init();
        ProjectileContent.initClient();
        AreaContent.initClient();
        EntityRenderers.init();
        RankClient.init();
        CraftRenderers.init();
        FurRenderer.init();
        WindPush.register();
        FogHooks.register();
        GrowthTooltip.register();
        ShootHarness.startIfRequested();
        DamageCheck.startIfRequested();
        SpawnCheck.startIfRequested();
        WormCheck.startIfRequested();
        ProjectileCheck.startIfRequested();
        AreaCheck.startIfRequested();
        tagWindow();
    }

    /** Dev runs only; runs after HEE sets its own dev title in preInit. */
    private static void tagWindow() {
        String tag = System.getProperty("etj.windowTag"), shoot = System.getProperty("etj.shoot");
        if (tag == null && shoot == null) return;
        Display.setTitle(WindowTitle.compose("Minecraft 1.7.10", tag, shoot));
    }
}
