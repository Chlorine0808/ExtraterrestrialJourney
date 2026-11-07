package chlorine.etjourney.core.damage;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import chlorine.etjourney.core.damage.model.DamageMath;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Applies elemental resistance to the hurt amount, and undoes other mods' cancels for piercing damage. */
public final class ElementalDamageEvents {

    /** The amount set at HIGH for the hit in progress, so LOWEST can restore it for piercing damage. */
    private final Map<EntityLivingBase, Float> resolved = new WeakHashMap<>();

    private ElementalDamageEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ElementalDamageEvents());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onHurt(LivingHurtEvent event) {
        if (!(event.source instanceof ElementalDamageSource)) return;
        ElementalDamageSource source = (ElementalDamageSource) event.source;
        event.ammount = DamageMath.scale(event.ammount, source.damage(), ResistanceSources.of(event.entityLiving));
        resolved.put(event.entityLiving, event.ammount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onAttackLast(LivingAttackEvent event) {
        if (piercing(event.source, event.entityLiving)) event.setCanceled(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurtLast(LivingHurtEvent event) {
        Float amount = resolved.remove(event.entityLiving);
        if (!piercing(event.source, event.entityLiving)) return;
        event.setCanceled(false);
        if (amount != null) event.ammount = amount;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDeathLast(LivingDeathEvent event) {
        if (!piercing(event.source, event.entityLiving)) return;
        event.setCanceled(false);
        // Avaritia refills health when it cancels a death.
        event.entityLiving.setHealth(0);
    }

    private static boolean piercing(Object source, EntityLivingBase target) {
        return source instanceof ElementalDamageSource && ((ElementalDamageSource) source).damage()
            .isPiercing() && ElementalDamages.pierces(target);
    }
}
