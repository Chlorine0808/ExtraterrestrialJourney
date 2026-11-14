package chlorine.etjourney.core.combat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import chlorine.etjourney.core.combat.model.PendingHits;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Passes each hit on a tick later, its amount set to the health it really took; hits on target dummies and on
 * creatures that heal right back never arrive.
 */
public final class LandedHits {

    public interface Listener {

        void landed(CombatHit hit);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static final PendingHits<CombatHit> PENDING = new PendingHits<>();
    private static long tick;

    private LandedHits() {}

    public static void register() {
        LandedHits events = new LandedHits();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance()
            .bus()
            .register(events);
    }

    public static void listen(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Lands every waiting hit now, for in-game checks that look right after a hit. */
    public static void flush() {
        PENDING.flush(LandedHits::health, LandedHits::land);
    }

    /** Last, so the amount is final; absorption counts as health. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving.worldObj.isRemote || event.ammount <= 0) return;
        PENDING.add(CombatHit.of(event), health(event.entityLiving), event.ammount, tick);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) tick++;
        else PENDING.resolve(tick, LandedHits::health, LandedHits::land);
    }

    private static float health(CombatHit hit) {
        return health(hit.target);
    }

    private static float health(EntityLivingBase living) {
        return living.getHealth() + living.getAbsorptionAmount();
    }

    private static void land(CombatHit hit, float lost) {
        hit.setAmount(lost);
        for (Listener listener : LISTENERS) listener.landed(hit);
    }
}
