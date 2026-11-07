package chlorine.etjourney.proxy;

import org.lwjgl.opengl.Display;

import chlorine.etjourney.content.craft.CraftRenderers;
import chlorine.etjourney.content.end.FurRenderer;
import chlorine.etjourney.content.entity.EntityRenderers;
import chlorine.etjourney.core.util.WindowTitle;
import chlorine.etjourney.world.end.debug.shoot.ShootHarness;
import chlorine.etjourney.world.updraft.UpdraftLift;
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
        EntityRenderers.init();
        CraftRenderers.init();
        FurRenderer.init();
        UpdraftLift.register();
        ShootHarness.startIfRequested();
        tagWindow();
    }

    /** Dev runs only; runs after HEE sets its own dev title in preInit. */
    private static void tagWindow() {
        String tag = System.getProperty("etj.windowTag"), shoot = System.getProperty("etj.shoot");
        if (tag == null && shoot == null) return;
        Display.setTitle(WindowTitle.compose("Minecraft 1.7.10", tag, shoot));
    }
}
