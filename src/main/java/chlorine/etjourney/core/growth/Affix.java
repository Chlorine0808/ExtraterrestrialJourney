package chlorine.etjourney.core.growth;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.combat.CombatHook;
import chlorine.etjourney.core.damage.model.Boosts;
import chlorine.etjourney.core.growth.model.GearSlot;

/** An affix: a rolled effect on gear that plugs into the combat hooks and damage boosts. */
public abstract class Affix {

    public final String id;
    public final Set<GearSlot> slots;
    public final int weight;
    /** The value at roll 0 and at roll 1. */
    private final float low, high;

    protected Affix(String id, int weight, float low, float high, GearSlot... slots) {
        this.id = id;
        this.weight = weight;
        this.low = low;
        this.high = high;
        this.slots = EnumSet.copyOf(Arrays.asList(slots));
    }

    public float value(float roll) {
        return low + (high - low) * roll;
    }

    /** Hook ids should come from {@link #hookId(GearSlot, int)} so each worn place keeps its own gate. */
    public void addHooks(ItemStack stack, EntityLivingBase owner, float value, String hookId, List<CombatHook> out) {}

    public void addBoosts(ItemStack stack, EntityLivingBase owner, float value, Boosts.Builder out) {}

    /** The tooltip line, already translated. */
    public abstract String describe(float value);

    public String hookId(GearSlot slot, int place) {
        return "etjourney:affix/" + id
            + "/"
            + slot.name()
                .toLowerCase()
            + place;
    }
}
