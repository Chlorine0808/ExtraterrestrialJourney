package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import chlorine.etjourney.core.damage.AttackProfiles;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Calls the hooks of the hurt creature, then of its attacker, after ETJ's boosts and resistance. */
public final class CombatHookEvents {

    private CombatHookEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CombatHookEvents());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.entityLiving;
        if (target.worldObj.isRemote) return;
        CombatHit hit = hit(event);
        for (CombatHook hook : CombatHookSources.of(target)) hook.onHurt(target, hit);
        event.ammount = hit.amount();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDealt(LivingHurtEvent event) {
        if (event.entityLiving.worldObj.isRemote || event.ammount <= 0) return;
        CombatHit hit = hit(event);
        if (hit.attacker == null || hit.attacker == hit.target) return;
        for (CombatHook hook : CombatHookSources.of(hit.attacker)) hook.onDamageDealt(hit.attacker, hit);
    }

    private static CombatHit hit(LivingHurtEvent event) {
        EntityLivingBase attacker = event.source.getEntity() instanceof EntityLivingBase
            ? (EntityLivingBase) event.source.getEntity()
            : null;
        return new CombatHit(
            event.entityLiving,
            attacker,
            event.source,
            AttackProfiles.of(event.source),
            event.ammount);
    }
}
