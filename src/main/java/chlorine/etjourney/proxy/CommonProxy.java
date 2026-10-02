package chlorine.etjourney.proxy;

import chlorine.etjourney.Tags;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Calls each layer in order for every FML lifecycle stage. */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModLog.LOG.info("preInit done (version {})", Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ModLog.LOG.info("init done");
    }

    public void postInit(FMLPostInitializationEvent event) {
        ModLog.LOG.info("postInit done");
    }
}
