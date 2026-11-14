package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Calls the hooks of the hurt creature after ETJ's boosts and resistance, and those of its attacker once the hit
 * has landed.
 */
public final class CombatHookEvents {

    private CombatHookEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CombatHookEvents());
        LandedHits.listen(CombatHookEvents::onDealt);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.entityLiving;
        if (target.worldObj.isRemote) return;
        CombatHit hit = CombatHit.of(event);
        for (CombatHook hook : CombatHookSources.of(target)) hook.onHurt(target, hit);
        event.ammount = hit.amount();
    }

    private static void onDealt(CombatHit hit) {
        if (hit.attacker == null || hit.attacker == hit.target) return;
        for (CombatHook hook : CombatHookSources.of(hit.attacker)) hook.onDamageDealt(hit.attacker, hit);
    }
}
