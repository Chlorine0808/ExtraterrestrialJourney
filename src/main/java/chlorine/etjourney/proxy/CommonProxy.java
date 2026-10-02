package chlorine.etjourney.proxy;

import chlorine.etjourney.Tags;
import chlorine.etjourney.compat.CompatManager;
import chlorine.etjourney.content.ModContent;
import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.network.ETJNetwork;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.ModWorld;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Calls each layer in order for every FML lifecycle stage. */
public class CommonProxy {

    private final CompatManager compat = CompatManager.createDefault();

    public void preInit(FMLPreInitializationEvent event) {
        ETJConfig.load(event.getSuggestedConfigurationFile());
        ModContent.preInit();
        ModWorld.preInit();
        compat.preInit();
        ModLog.LOG.info("preInit done (version {})", Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        ETJNetwork.init();
        ModWorld.init();
        compat.init();
        ModLog.LOG.info("init done");
    }

    public void postInit(FMLPostInitializationEvent event) {
        compat.postInit();
        ModLog.LOG.info("postInit done");
    }
}
