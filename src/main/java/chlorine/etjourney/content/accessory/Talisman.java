package chlorine.etjourney.content.accessory;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.accessory.AccessoryModifiers;
import chlorine.etjourney.core.combat.CombatHook;
import chlorine.etjourney.core.damage.model.Boosts;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.Resistance;

/**
 * The early accessories worn in ETJ's talisman slots, each kept small against vanilla: Sakura Gold attacks, Golden
 * Palladium defends, Drift moves, and Rhenium gives a stronger taste of all three until it wears out. The last four
 * are trials of hooks and boosts.
 */
public enum Talisman {

    SAKURA_GOLD("sakura_gold_talisman", 1, 0, 0, 0, 0),
    GOLDEN_PALLADIUM("golden_palladium_talisman", 0, 2, 0, 0.5f, 0),
    DRIFT("drift_talisman", 0, 0, 0.05, 0, 0),
    RHENIUM("rhenium_talisman", 2, 4, 0.10, 0, 300),
    VAMPIRE("vampire_talisman", TalismanPower.VAMPIRE),
    BUBBLE("bubble_talisman", TalismanPower.BUBBLE),
    WRATH("wrath_talisman", TalismanPower.WRATH),
    EMBER("ember_talisman", TalismanPower.EMBER);

    public final String name;
    final double attack;
    final double health;
    /** A share of the base speed. */
    final double speed;
    /** Taken off neutral elemental damage. */
    final float neutralFlat;
    /** Worn seconds before it breaks; 0 lasts forever. */
    public final int lifetime;
    final TalismanPower power;

    Talisman(String name, TalismanPower power) {
        this(name, 0, 0, 0, 0, 0, power);
    }

    Talisman(String name, double attack, double health, double speed, float neutralFlat, int lifetime) {
        this(name, attack, health, speed, neutralFlat, lifetime, TalismanPower.NONE);
    }

    Talisman(String name, double attack, double health, double speed, float neutralFlat, int lifetime,
        TalismanPower power) {
        this.power = power;
        this.name = name;
        this.attack = attack;
        this.health = health;
        this.speed = speed;
        this.neutralFlat = neutralFlat;
        this.lifetime = lifetime;
    }

    public void addModifiers(AccessoryModifiers out) {
        if (attack != 0) out.add(SharedMonsterAttributes.attackDamage, attack, 0);
        if (health != 0) out.add(SharedMonsterAttributes.maxHealth, health, 0);
        if (speed != 0) out.add(SharedMonsterAttributes.movementSpeed, speed, 1);
    }

    public void addResistance(Resistance.Builder out) {
        if (neutralFlat != 0) out.subtract(ElementRegistry.NEUTRAL, neutralFlat);
    }

    /** Hooks share one gate per talisman, named by its registry name. */
    public void addHooks(List<CombatHook> out) {
        power.addHooks(ModInfo.MODID + ":" + name, out);
    }

    public void addBoosts(EntityLivingBase wearer, Boosts.Builder out) {
        power.addBoosts(wearer, out);
    }

    /** The stack after one more worn second: the same stack, a more worn copy, or null once spent. */
    public ItemStack wear(ItemStack stack) {
        if (lifetime <= 0) return stack;
        int used = WearOut.afterOneSecond(stack.getItemDamage(), lifetime);
        if (used == WearOut.SPENT) return null;
        ItemStack after = stack.copy();
        after.setItemDamage(used);
        return after;
    }

    public void addInformation(ItemStack stack, List<String> lines) {
        if (attack != 0) lines.add(line("attack", format(attack)));
        if (health != 0) lines.add(line("health", format(health)));
        if (speed != 0) lines.add(line("speed", format(Math.round(speed * 100))));
        if (neutralFlat != 0) {
            String element = StatCollector.translateToLocal("etjourney.element." + ElementRegistry.NEUTRAL.name());
            lines.add(line("resist_flat", element, format(neutralFlat)));
        }
        power.addInformation(lines);
        if (lifetime > 0) lines.add(
            EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(
                "etjourney.tooltip.talisman.wears",
                Math.max(0, lifetime - stack.getItemDamage())));
    }

    private static String line(String key, Object... args) {
        return EnumChatFormatting.BLUE
            + StatCollector.translateToLocalFormatted("etjourney.tooltip.talisman." + key, args);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
