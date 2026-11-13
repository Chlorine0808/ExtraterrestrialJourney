package chlorine.etjourney.content.status;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.damage.ElementEffects;
import chlorine.etjourney.core.status.StatusOnHit;
import chlorine.etjourney.core.status.StatusPotion;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** ETJ's own status effects: Heavy lowers jumps, Frozen stops walking and jumping. Ice hits freeze briefly. */
public final class Ailments {

    /** Share of the jump each level of Heavy takes away. */
    static final float HEAVY_PER_LEVEL = 0.25f;
    /** How long an ice hit freezes. */
    static final int ICE_FREEZE_TICKS = 20;

    /** Null when no potion ID was free. */
    public static StatusPotion heavy, frozen;

    private Ailments() {}

    public static void preInit() {
        heavy = make("heavy", ETJConfig.heavyPotionId, 0x5A4A3A, 2, 1);
        frozen = make("frozen", ETJConfig.frozenPotionId, 0xA8E4F0, 1, 0);
        if (frozen != null) {
            frozen.func_111184_a(SharedMonsterAttributes.movementSpeed, "6F0C3E61-6C59-4B58-9E62-2A2C5F1B7D10", -1, 2);
            ElementEffects.register(Elements.ICE, new StatusOnHit(frozen, ICE_FREEZE_TICKS, 0, false));
        }
        MinecraftForge.EVENT_BUS.register(new Ailments());
    }

    private static StatusPotion make(String name, int preferred, int color, int iconX, int iconY) {
        String key = ModInfo.MODID + ".potion." + name;
        int id = StatusPotion.claimId(key, preferred);
        return id < 0 ? null : new StatusPotion(id, key, true, color, iconX, iconY);
    }

    /** What is left of a jump under Heavy of the given amplifier. */
    static float jumpLeft(int amplifier) {
        return Math.max(0, 1 - HEAVY_PER_LEVEL * (amplifier + 1));
    }

    /** Runs on both sides, since a player's jump happens on the client. */
    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        EntityLivingBase living = event.entityLiving;
        if (frozen != null && living.isPotionActive(frozen)) {
            living.motionY = 0;
            return;
        }
        PotionEffect weight = heavy == null ? null : living.getActivePotionEffect(heavy);
        if (weight != null) living.motionY *= jumpLeft(weight.getAmplifier());
    }
}
