package chlorine.etjourney.world.end.debug.damagecheck;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;

import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client half of the damage check run by -Detj.damageCheck: opens a fresh world, waits for the server, quits. */
public final class DamageCheck {

    private static final String SAVE = "etj-damagecheck";
    private static final long BUDGET_MS = 3 * 60_000L;
    /** Client ticks to wait after the checks so the log is flushed. */
    private static final int LINGER_TICKS = 40;

    /** Client ticks the local player must have lived before the client-side check, before the server checks start. */
    private static final int CLIENT_CHECK_TICKS = 20;

    /** The client-side result line, for the server half to record. */
    static volatile String clientResult;

    private final long started = System.currentTimeMillis();
    private boolean launched;
    private int lingered;

    private DamageCheck() {}

    public static void startIfRequested() {
        if (!Boolean.getBoolean("etj.damageCheck")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new DamageCheck());
        DamageCheckServer.register();
    }

    /** Damage must never be applied on the client copy of a player, piercing or not. */
    private static String checkClientSide(Minecraft mc) {
        Element chaos = ElementRegistry.get("chaos");
        if (chaos == null) return "FAIL client side: chaos is not registered";
        float before = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
        ElementalDamage damage = ElementalDamage.builder(DamageKind.MAGIC)
            .add(chaos, 5)
            .build();
        ElementalDamages.deal(mc.thePlayer, new ElementalDamageSource(damage, null, null));
        float lost = before - (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount());
        mc.thePlayer.setHealth(before);
        return (Math.abs(lost) <= 0.01f ? "PASS" : "FAIL") + " client side untouched: lost " + lost;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (System.currentTimeMillis() - started > BUDGET_MS) {
            ModLog.LOG.error("[damagecheck] FAIL budget of {} ms spent", BUDGET_MS);
            mc.shutdown();
            return;
        }
        if (!launched) {
            if (!(mc.currentScreen instanceof GuiMainMenu)) return;
            ISaveFormat saves = mc.getSaveLoader();
            saves.flushCache();
            saves.deleteWorldDirectory(SAVE);
            WorldSettings settings = new WorldSettings(
                0L,
                WorldSettings.GameType.SURVIVAL,
                false,
                false,
                WorldType.FLAT).enableCommands();
            mc.launchIntegratedServer(SAVE, SAVE, settings);
            launched = true;
            return;
        }
        if (clientResult == null && mc.thePlayer != null && mc.thePlayer.ticksExisted >= CLIENT_CHECK_TICKS) {
            clientResult = checkClientSide(mc);
        }
        if (DamageCheckServer.done && ++lingered >= LINGER_TICKS) {
            ModLog.LOG.info("[damagecheck] finished");
            mc.shutdown();
        }
    }
}
