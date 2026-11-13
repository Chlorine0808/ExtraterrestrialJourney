package chlorine.etjourney.core.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.accessory.Accessory;
import chlorine.etjourney.core.accessory.WornAccessories;

/** Where a creature's combat hooks come from; counted accessories are registered from the start. */
public final class CombatHookSources {

    /** Adds the hooks one source gives the owner. */
    public interface Source {

        void collect(EntityLivingBase owner, List<CombatHook> out);
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();

    static {
        register(
            (owner, out) -> {
                for (ItemStack stack : WornAccessories.of(owner))
                    ((Accessory) stack.getItem()).addHooks(stack, owner, out);
            });
    }

    private CombatHookSources() {}

    public static void register(Source source) {
        SOURCES.add(source);
    }

    public static List<CombatHook> of(EntityLivingBase owner) {
        List<CombatHook> out = new ArrayList<>();
        for (Source source : SOURCES) source.collect(owner, out);
        return out;
    }
}
