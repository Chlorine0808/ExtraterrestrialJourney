package chlorine.etjourney.core.damage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.damage.model.Resistance;

/** Where a defender's resistance comes from; creatures and worn armor are registered from the start. */
public final class ResistanceSources {

    /** Adds what one source grants to the target. */
    public interface Source {

        void collect(EntityLivingBase target, Resistance.Builder out);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    static {
        register(
            (target, out) -> {
                if (target instanceof ElementalResistant) ((ElementalResistant) target).addResistance(out);
            });
        register((target, out) -> {
            for (int slot = 1; slot <= 4; slot++) {
                ItemStack stack = target.getEquipmentInSlot(slot);
                if (stack != null && stack.getItem() instanceof ElementalArmor)
                    ((ElementalArmor) stack.getItem()).addResistance(stack, target, out);
            }
        });
    }

    private ResistanceSources() {}

    public static void register(Source source) {
        SOURCES.add(source);
    }

    public static Resistance of(EntityLivingBase target) {
        Resistance.Builder out = Resistance.builder();
        for (Source source : SOURCES) source.collect(target, out);
        return out.build();
    }
}
