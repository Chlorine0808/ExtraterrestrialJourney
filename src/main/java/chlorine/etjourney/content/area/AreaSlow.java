package chlorine.etjourney.content.area;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Slows creatures standing in areas through their movement speed, which the client also sees for players, and
 * holds back their jumps by the same factor. Areas mark whom they slow each tick; at the world tick's end the
 * strongest mark wins and the unmarked go free.
 */
public final class AreaSlow {

    private static final UUID MODIFIER = UUID.fromString("6f2a4d1e-8b3c-4e57-9a10-2c7d5e8f4b61");
    private static final String NAME = "etjourney area slow";
    /** Multiplies the total speed. */
    private static final int MULTIPLY_TOTAL = 2;
    /** The sideways push vanilla gives a sprinting jump. */
    private static final double SPRINT_JUMP_PUSH = 0.2;

    private static final Map<EntityLivingBase, Double> WANTED = new WeakHashMap<>();
    private static final Set<EntityLivingBase> SLOWED = Collections.newSetFromMap(new WeakHashMap<>());

    private AreaSlow() {}

    public static void register() {
        AreaSlow handler = new AreaSlow();
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
    }

    /** Asks for the creature to move at factor times its speed this tick. */
    public static void mark(EntityLivingBase e, double factor) {
        Double was = WANTED.get(e);
        if (was == null || factor < was) WANTED.put(e, factor);
    }

    /** The speed factor an area holds the creature at now, 1 when free. */
    public static double factor(EntityLivingBase e) {
        AttributeModifier m = speed(e).getModifier(MODIFIER);
        return m == null ? 1 : 1 + m.getAmount();
    }

    /** The share of the take-off speed left, so the jump reaches factor times its height. */
    static double jumpLeft(double factor) {
        return Math.sqrt(Math.max(0, Math.min(1, factor)));
    }

    /** Runs on both sides, since a player's jump happens on the client; also takes back the sprint push. */
    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        EntityLivingBase e = event.entityLiving;
        double factor = factor(e);
        if (factor >= 1) return;
        e.motionY *= jumpLeft(factor);
        if (!e.isSprinting()) return;
        double yaw = Math.toRadians(e.rotationYaw), back = SPRINT_JUMP_PUSH * (1 - factor);
        e.motionX += Math.sin(yaw) * back;
        e.motionZ -= Math.cos(yaw) * back;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        for (Iterator<EntityLivingBase> it = SLOWED.iterator(); it.hasNext();) {
            EntityLivingBase e = it.next();
            if (e.worldObj != event.world || WANTED.containsKey(e)) continue;
            release(e);
            it.remove();
        }
        for (Iterator<Map.Entry<EntityLivingBase, Double>> it = WANTED.entrySet()
            .iterator(); it.hasNext();) {
            Map.Entry<EntityLivingBase, Double> w = it.next();
            if (w.getKey().worldObj != event.world) continue;
            hold(w.getKey(), w.getValue());
            SLOWED.add(w.getKey());
            it.remove();
        }
    }

    /** The modifier is saved with the creature; one loaded back has left every area. */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (!event.world.isRemote && event.entity instanceof EntityLivingBase && !SLOWED.contains(event.entity))
            release((EntityLivingBase) event.entity);
    }

    private static void hold(EntityLivingBase e, double factor) {
        IAttributeInstance speed = speed(e);
        AttributeModifier m = speed.getModifier(MODIFIER);
        double amount = factor - 1;
        if (m != null && m.getAmount() == amount) return;
        if (m != null) speed.removeModifier(m);
        speed.applyModifier(new AttributeModifier(MODIFIER, NAME, amount, MULTIPLY_TOTAL));
    }

    private static void release(EntityLivingBase e) {
        IAttributeInstance speed = speed(e);
        AttributeModifier m = speed.getModifier(MODIFIER);
        if (m != null) speed.removeModifier(m);
    }

    private static IAttributeInstance speed(EntityLivingBase e) {
        return e.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
    }
}
