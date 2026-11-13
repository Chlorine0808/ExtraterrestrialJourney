package chlorine.etjourney.content.growth;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.core.combat.CombatHit;
import chlorine.etjourney.core.combat.CombatHook;
import chlorine.etjourney.core.combat.TriggeredHook;
import chlorine.etjourney.core.combat.model.Conditions;
import chlorine.etjourney.core.combat.model.Gate;
import chlorine.etjourney.core.damage.model.BoostFilter;
import chlorine.etjourney.core.damage.model.Boosts;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Reach;
import chlorine.etjourney.core.growth.Affix;
import chlorine.etjourney.core.growth.Affixes;
import chlorine.etjourney.core.growth.GrowthEffects;
import chlorine.etjourney.core.growth.model.GearSlot;

/** The trial affixes; ranges and weights are placeholders. */
public final class GearAffixes {

    /** Health share at or below which second wind heals. */
    static final float SECOND_WIND_HEALTH = 0.3f;
    static final long SECOND_WIND_RECHARGE = 1200;

    private GearAffixes() {}

    public static void register() {
        Affixes.register(boost("keen", 0.05f, 0.15f, BoostFilter.ALL, GearSlot.WEAPON, GearSlot.ACCESSORY));
        Affixes.register(
            boost(
                "brutal",
                0.10f,
                0.20f,
                BoostFilter.ALL.kind(DamageKind.PHYSICAL)
                    .reach(Reach.MELEE),
                GearSlot.WEAPON));
        Affixes.register(boost("searing", 0.10f, 0.25f, BoostFilter.element(Elements.FIRE), GearSlot.WEAPON));
        Affixes.register(boost("arcane", 0.05f, 0.15f, BoostFilter.ALL.kind(DamageKind.MAGIC), GearSlot.ACCESSORY));
        Affixes.register(boost("fierce", 0.05f, 0.15f, BoostFilter.ALL.kind(DamageKind.PHYSICAL), GearSlot.ACCESSORY));
        Affixes.register(new Affix("vampiric", 10, 0.03f, 0.08f, GearSlot.WEAPON) {

            @Override
            public void addHooks(ItemStack stack, EntityLivingBase owner, float value, String hookId,
                List<CombatHook> out) {
                out.add(
                    new TriggeredHook(
                        hookId,
                        TriggeredHook.When.DEALT,
                        Conditions.reach(Reach.MELEE),
                        null,
                        (o, hit) -> {
                            o.heal(hit.amount() * value);
                            return true;
                        }));
            }

            @Override
            public String describe(float value) {
                return text(id, percent(value));
            }
        });
        Affixes.register(new Affix("sturdy", 10, 0.02f, 0.06f, GearSlot.ARMOR) {

            @Override
            public void addHooks(ItemStack stack, EntityLivingBase owner, float value, String hookId,
                List<CombatHook> out) {
                out.add(new TriggeredHook(hookId, TriggeredHook.When.HURT, null, null, (o, hit) -> cut(hit, value)));
            }

            @Override
            public String describe(float value) {
                return text(id, percent(value));
            }
        });
        Affixes.register(new Affix("warded", 10, 0.05f, 0.10f, GearSlot.ARMOR) {

            @Override
            public void addHooks(ItemStack stack, EntityLivingBase owner, float value, String hookId,
                List<CombatHook> out) {
                out.add(
                    new TriggeredHook(
                        hookId,
                        TriggeredHook.When.HURT,
                        Conditions.kind(DamageKind.MAGIC),
                        null,
                        (o, hit) -> cut(hit, value)));
            }

            @Override
            public String describe(float value) {
                return text(id, percent(value));
            }
        });
        Affixes.register(new Affix("thorned", 10, 0.05f, 0.15f, GearSlot.ARMOR) {

            @Override
            public void addHooks(ItemStack stack, EntityLivingBase owner, float value, String hookId,
                List<CombatHook> out) {
                out.add(
                    new TriggeredHook(
                        hookId,
                        TriggeredHook.When.HURT,
                        Conditions.reach(Reach.MELEE),
                        null,
                        (o, hit) -> {
                            // Thorns never answer thorns, so two thorned wearers do not trade forever.
                            if (hit.attacker == null || "thorns".equals(hit.source.damageType) || hit.amount() <= 0)
                                return false;
                            hit.attacker.attackEntityFrom(DamageSource.causeThornsDamage(o), hit.amount() * value);
                            return true;
                        }));
            }

            @Override
            public String describe(float value) {
                return text(id, percent(value));
            }
        });
        Affixes.register(new Affix("second_wind", 6, 2f, 4f, GearSlot.ACCESSORY) {

            @Override
            public void addHooks(ItemStack stack, EntityLivingBase owner, float value, String hookId,
                List<CombatHook> out) {
                out.add(
                    new TriggeredHook(
                        hookId,
                        TriggeredHook.When.HURT,
                        null,
                        Gate.every(SECOND_WIND_RECHARGE),
                        (o, hit) -> {
                            float after = (o.getHealth() - hit.amount()) / o.getMaxHealth();
                            if (after > SECOND_WIND_HEALTH || after <= 0) return false;
                            o.heal(value);
                            return true;
                        }));
            }

            @Override
            public String describe(float value) {
                return text(
                    id,
                    String.format("%.1f", value),
                    Math.round(SECOND_WIND_HEALTH * 100),
                    SECOND_WIND_RECHARGE / 20);
            }
        });
    }

    private static Affix boost(String id, float low, float high, BoostFilter filter, GearSlot... slots) {
        return new Affix(id, 10, low, high, slots) {

            @Override
            public void addBoosts(ItemStack stack, EntityLivingBase owner, float value, Boosts.Builder out) {
                out.add(filter, value);
            }

            @Override
            public String describe(float value) {
                return text(id, percent(value));
            }
        };
    }

    private static boolean cut(CombatHit hit, float share) {
        if (!GrowthEffects.reducible(hit.source) || hit.amount() <= 0) return false;
        hit.setAmount(hit.amount() * (1 - share));
        return true;
    }

    private static long percent(float value) {
        return Math.round(value * 100);
    }

    private static String text(String id, Object... args) {
        return StatCollector.translateToLocalFormatted("etjourney.affix." + id, args);
    }
}
