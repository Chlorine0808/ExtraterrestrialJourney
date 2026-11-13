package chlorine.etjourney.core.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.accessory.WornAccessories;
import chlorine.etjourney.core.status.model.StatusBoosts;
import chlorine.etjourney.core.status.model.StatusResistance;

/** Where status resistance and boosts come from; creatures, worn armor and accessories are registered from start. */
public final class StatusSources {

    public interface ResistanceSource {

        void collect(EntityLivingBase taker, StatusResistance.Builder out);
    }

    public interface BoostSource {

        void collect(EntityLivingBase giver, StatusBoosts.Builder out);
    }

    private static final List<ResistanceSource> RESISTANCES = new CopyOnWriteArrayList<>();
    private static final List<BoostSource> BOOSTS = new CopyOnWriteArrayList<>();

    static {
        registerResistance((taker, out) -> {
            if (taker instanceof StatusResistant) ((StatusResistant) taker).addStatusResistance(out);
            for (ItemStack stack : wards(taker)) ((StatusWard) stack.getItem()).addStatusResistance(stack, taker, out);
        });
        registerBoost((giver, out) -> {
            if (giver instanceof StatusBooster) ((StatusBooster) giver).addStatusBoosts(out);
            for (ItemStack stack : wards(giver)) ((StatusWard) stack.getItem()).addStatusBoosts(stack, giver, out);
        });
    }

    private StatusSources() {}

    public static void registerResistance(ResistanceSource source) {
        RESISTANCES.add(source);
    }

    public static void registerBoost(BoostSource source) {
        BOOSTS.add(source);
    }

    public static StatusResistance resistanceOf(EntityLivingBase taker) {
        StatusResistance.Builder out = StatusResistance.builder();
        for (ResistanceSource source : RESISTANCES) source.collect(taker, out);
        return out.build();
    }

    /** No boosts without a living giver. */
    public static StatusBoosts boostsOf(EntityLivingBase giver) {
        if (giver == null) return StatusBoosts.NONE;
        StatusBoosts.Builder out = StatusBoosts.builder();
        for (BoostSource source : BOOSTS) source.collect(giver, out);
        return out.build();
    }

    /** Worn armor, then counted accessories, that are wards. */
    private static List<ItemStack> wards(EntityLivingBase living) {
        List<ItemStack> out = new ArrayList<>();
        for (int slot = 1; slot <= 4; slot++) {
            ItemStack stack = living.getEquipmentInSlot(slot);
            if (stack != null && stack.getItem() instanceof StatusWard) out.add(stack);
        }
        for (ItemStack stack : WornAccessories.of(living)) if (stack.getItem() instanceof StatusWard) out.add(stack);
        return out;
    }
}
