package chlorine.etjourney.proxy;

import chlorine.etjourney.content.craft.CraftRenderers;
import chlorine.etjourney.content.entity.EntityRenderers;
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
        UpdraftLift.register();
        ShootHarness.startIfRequested();
    }
}
