package chlorine.etjourney.content.accessory;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.combat.CombatHit;
import chlorine.etjourney.core.combat.CombatHook;
import chlorine.etjourney.core.combat.LivingSituation;
import chlorine.etjourney.core.combat.TriggeredHook;
import chlorine.etjourney.core.combat.model.Condition;
import chlorine.etjourney.core.combat.model.Conditions;
import chlorine.etjourney.core.combat.model.Gate;
import chlorine.etjourney.core.damage.model.BoostFilter;
import chlorine.etjourney.core.damage.model.Boosts;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Reach;

/** What a trial talisman does beyond attributes: hooks on hits and damage boosts. Values are placeholders. */
public enum TalismanPower {

    NONE,
    /** Heals a share of a melee hit dealt. */
    VAMPIRE {

        @Override
        void addHooks(String id, List<CombatHook> out) {
            out.add(
                new TriggeredHook(id, TriggeredHook.When.DEALT, Conditions.reach(Reach.MELEE), null, (owner, hit) -> {
                    owner.heal(hit.amount() * VAMPIRE_SHARE);
                    return true;
                }));
        }

        @Override
        void addInformation(List<String> lines) {
            lines.add(line("vampire", Math.round(VAMPIRE_SHARE * 100)));
        }
    },
    /** Takes the whole of the next hit from a creature, then needs time to come back. */
    BUBBLE {

        @Override
        void addHooks(String id, List<CombatHook> out) {
            out.add(
                new TriggeredHook(id, TriggeredHook.When.HURT, null, Gate.every(BUBBLE_RECHARGE), TalismanPower::pop));
        }

        @Override
        void addInformation(List<String> lines) {
            lines.add(line("bubble", BUBBLE_RECHARGE / 20));
        }
    },
    /** Hits harder while health is low. */
    WRATH {

        @Override
        void addBoosts(EntityLivingBase wearer, Boosts.Builder out) {
            if (WRATH_WHEN.test(new LivingSituation(wearer, null))) out.add(BoostFilter.ALL, WRATH_BONUS);
        }

        @Override
        void addInformation(List<String> lines) {
            lines.add(line("wrath", Math.round(WRATH_HEALTH * 100), Math.round(WRATH_BONUS * 100)));
        }
    },
    /** Raises fire, and physical fire further. */
    EMBER {

        @Override
        void addBoosts(EntityLivingBase wearer, Boosts.Builder out) {
            out.add(BoostFilter.element(Elements.FIRE), EMBER_FIRE);
            out.add(
                BoostFilter.element(Elements.FIRE)
                    .kind(DamageKind.PHYSICAL),
                EMBER_PHYSICAL_FIRE);
        }

        @Override
        void addInformation(List<String> lines) {
            lines.add(line("ember", Math.round(EMBER_FIRE * 100), Math.round(EMBER_PHYSICAL_FIRE * 100)));
        }
    };

    static final float VAMPIRE_SHARE = 0.15f;
    /** Ticks for the bubble to come back. */
    static final long BUBBLE_RECHARGE = 400;
    static final float WRATH_HEALTH = 0.5f;
    static final float WRATH_BONUS = 0.4f;
    static final Condition WRATH_WHEN = Conditions.healthAtMost(WRATH_HEALTH);
    static final float EMBER_FIRE = 0.3f;
    static final float EMBER_PHYSICAL_FIRE = 0.2f;

    void addHooks(String id, List<CombatHook> out) {}

    void addBoosts(EntityLivingBase wearer, Boosts.Builder out) {}

    void addInformation(List<String> lines) {}

    /** Blocks a hit a creature or projectile made, but never piercing damage or the void. */
    private static boolean pop(EntityLivingBase owner, CombatHit hit) {
        DamageSource source = hit.source;
        if (hit.amount() <= 0 || source.getEntity() == null || source.isDamageAbsolute() || source.canHarmInCreative())
            return false;
        hit.setAmount(0);
        owner.worldObj.playSoundAtEntity(owner, "random.orb", 0.8f, 0.5f);
        return true;
    }

    private static String line(String key, Object... args) {
        return EnumChatFormatting.BLUE
            + StatCollector.translateToLocalFormatted(ModInfo.MODID + ".tooltip.talisman." + key, args);
    }
}
