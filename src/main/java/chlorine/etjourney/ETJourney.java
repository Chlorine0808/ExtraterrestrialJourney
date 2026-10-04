package chlorine.etjourney;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.proxy.CommonProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

// After HEE and NovaCraft, so their End generators and decorators exist when compat modules hook them.
@Mod(
    modid = ModInfo.MODID,
    name = ModInfo.NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "after:HardcoreEnderExpansion;after:nova_craft")
public class ETJourney {

    @SidedProxy(
        clientSide = "chlorine.etjourney.proxy.ClientProxy",
        serverSide = "chlorine.etjourney.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
