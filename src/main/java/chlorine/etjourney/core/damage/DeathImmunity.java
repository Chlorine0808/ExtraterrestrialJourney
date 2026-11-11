package chlorine.etjourney.core.damage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** ETJ's own death immunity: only piercing damage and the void get through, as with Avaritia's armor. */
public final class DeathImmunity {

    /** Implemented by a creature that is immune. */
    public interface Creature {
    }

    /** Implemented by an armor item; the wearer is immune with all four pieces on. */
    public interface Armor {
    }

    private DeathImmunity() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new DeathImmunity());
    }

    public static boolean isImmune(EntityLivingBase target) {
        if (target instanceof Creature) return true;
        Item[] worn = new Item[4];
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = target.getEquipmentInSlot(slot);
            worn[slot - 1] = stack == null ? null : stack.getItem();
        }
        return fullSet(worn);
    }

    static boolean fullSet(Item... worn) {
        for (Item item : worn) if (!(item instanceof Armor)) return false;
        return true;
    }

    /** The void stays lethal so nobody falls forever; /kill uses it too. */
    static boolean letsThrough(DamageSource source) {
        if (source == DamageSource.outOfWorld) return true;
        return source instanceof ElementalDamageSource && ((ElementalDamageSource) source).damage()
            .isPiercing();
    }

    private static boolean blocks(EntityLivingBase target, DamageSource source) {
        return !target.worldObj.isRemote && !letsThrough(source) && isImmune(target);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(LivingAttackEvent event) {
        if (blocks(event.entityLiving, event.source)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (blocks(event.entityLiving, event.source)) event.setCanceled(true);
    }

    /** Also catches kills that skip the hurt events, such as setHealth(0) from another mod. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!blocks(event.entityLiving, event.source)) return;
        event.setCanceled(true);
        event.entityLiving.setHealth(event.entityLiving.getMaxHealth());
    }
}
