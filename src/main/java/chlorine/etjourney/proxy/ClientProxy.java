package chlorine.etjourney.proxy;

import chlorine.etjourney.world.end.debug.shoot.ShootHarness;
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
        ShootHarness.startIfRequested();
    }
}
