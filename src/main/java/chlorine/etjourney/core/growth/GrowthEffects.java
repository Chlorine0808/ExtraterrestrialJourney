package chlorine.etjourney.core.growth;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.core.accessory.WornAccessories;
import chlorine.etjourney.core.combat.CombatHook;
import chlorine.etjourney.core.combat.CombatHookSources;
import chlorine.etjourney.core.combat.TriggeredHook;
import chlorine.etjourney.core.damage.BoostSources;
import chlorine.etjourney.core.damage.model.BoostFilter;
import chlorine.etjourney.core.damage.model.Boosts;
import chlorine.etjourney.core.growth.model.AffixRoll;
import chlorine.etjourney.core.growth.model.GearSlot;
import chlorine.etjourney.core.growth.model.GrowthBonus;
import chlorine.etjourney.core.growth.model.GrowthData;

/** Plugs gear levels and affixes into the damage boosts and combat hooks; only gear in use counts. */
public final class GrowthEffects {

    /** One piece of gear in use: where it is worn and its growth. */
    private static final class InUse {

        final ItemStack stack;
        final GearSlot slot;
        final int place;
        final GrowthData data;

        InUse(ItemStack stack, GearSlot slot, int place, GrowthData data) {
            this.stack = stack;
            this.slot = slot;
            this.place = place;
            this.data = data;
        }
    }

    private GrowthEffects() {}

    public static void register() {
        BoostSources.register(GrowthEffects::addBoosts);
        CombatHookSources.register(GrowthEffects::addHooks);
    }

    /** The held weapon, worn armor and counted accessories that carry growth. */
    private static List<InUse> inUse(EntityLivingBase owner) {
        List<InUse> out = new ArrayList<>();
        add(out, owner.getHeldItem(), GearSlot.WEAPON, 0);
        for (int slot = 1; slot <= 4; slot++) add(out, owner.getEquipmentInSlot(slot), GearSlot.ARMOR, slot);
        int place = 0;
        for (ItemStack stack : WornAccessories.of(owner)) add(out, stack, GearSlot.ACCESSORY, place++);
        return out;
    }

    private static void add(List<InUse> out, ItemStack stack, GearSlot wanted, int place) {
        if (Gear.slot(stack) != wanted) return;
        GrowthData data = GrowthNbt.read(stack);
        if (data != null) out.add(new InUse(stack, wanted, place, data));
    }

    private static void addBoosts(EntityLivingBase owner, Boosts.Builder out) {
        for (InUse gear : inUse(owner)) {
            if (gear.slot == GearSlot.WEAPON) out.add(BoostFilter.ALL, GrowthBonus.weapon(gear.data.level()));
            if (gear.slot == GearSlot.ACCESSORY) out.add(BoostFilter.ALL, GrowthBonus.accessory(gear.data.level()));
            for (AffixRoll roll : gear.data.affixes) {
                Affix affix = Affixes.get(roll.id);
                if (affix != null) affix.addBoosts(gear.stack, owner, affix.value(roll.roll), out);
            }
        }
    }

    private static void addHooks(EntityLivingBase owner, List<CombatHook> out) {
        List<InUse> gear = inUse(owner);
        int n = 0;
        for (InUse g : gear) if (g.slot == GearSlot.ARMOR) n++;
        int[] levels = new int[n];
        int i = 0;
        for (InUse g : gear) {
            if (g.slot == GearSlot.ARMOR) levels[i++] = g.data.level();
            for (AffixRoll roll : g.data.affixes) {
                Affix affix = Affixes.get(roll.id);
                if (affix != null)
                    affix.addHooks(g.stack, owner, affix.value(roll.roll), affix.hookId(g.slot, g.place), out);
            }
        }
        float factor = GrowthBonus.armor(levels);
        if (factor < 1)
            out.add(new TriggeredHook("etjourney:growth/armor", TriggeredHook.When.HURT, null, null, (o, hit) -> {
                if (!reducible(hit.source)) return false;
                hit.setAmount(hit.amount() * factor);
                return true;
            }));
    }

    /** Piercing damage and the void pass every reduction. */
    public static boolean reducible(DamageSource source) {
        return !source.isDamageAbsolute() && !source.canHarmInCreative();
    }
}
