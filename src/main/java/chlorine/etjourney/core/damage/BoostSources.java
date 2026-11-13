package chlorine.etjourney.core.damage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.accessory.WornAccessories;
import chlorine.etjourney.core.damage.model.Boosts;

/** Where an attacker's damage boosts come from; creatures and accessories are registered from the start. */
public final class BoostSources {

    /** Adds what one source grants to the attacker. */
    public interface Source {

        void collect(EntityLivingBase attacker, Boosts.Builder out);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    /** Counted accessories only, so two copies of one accessory boost once. */
    static final Source ACCESSORIES = (attacker, out) -> {
        for (ItemStack stack : WornAccessories.of(attacker)) {
            if (stack.getItem() instanceof ElementalAccessory)
                ((ElementalAccessory) stack.getItem()).addBoosts(stack, attacker, out);
        }
    };

    static {
        register(
            (attacker, out) -> {
                if (attacker instanceof ElementalBooster) ((ElementalBooster) attacker).addBoosts(out);
            });
        register(ACCESSORIES);
    }

    private BoostSources() {}

    public static void register(Source source) {
        SOURCES.add(source);
    }

    /** No boosts when there is no living attacker. */
    public static Boosts of(EntityLivingBase attacker) {
        if (attacker == null) return Boosts.NONE;
        Boosts.Builder out = Boosts.builder();
        for (Source source : SOURCES) source.collect(attacker, out);
        return out.build();
    }
}
