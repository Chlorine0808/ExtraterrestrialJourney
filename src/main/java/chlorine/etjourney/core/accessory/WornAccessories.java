package chlorine.etjourney.core.accessory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.accessory.model.Distinct;

/** The one place that lists the accessories a creature counts as wearing; every effect reads through it. */
public final class WornAccessories {

    /** Where the slots live, such as Baubles; installed by compat. */
    public interface Provider {

        /** Every stack in the creature's accessory slots, in slot order; empty slots may be null. */
        List<ItemStack> worn(EntityLivingBase living);

        /** Puts the replacement, or nothing when null, where the worn stack is; false if it is gone. */
        boolean replace(EntityLivingBase living, ItemStack worn, ItemStack replacement);
    }

    private static final Provider NONE = new Provider() {

        @Override
        public List<ItemStack> worn(EntityLivingBase living) {
            return Collections.emptyList();
        }

        @Override
        public boolean replace(EntityLivingBase living, ItemStack worn, ItemStack replacement) {
            return false;
        }
    };

    private static volatile Provider provider = NONE;

    private WornAccessories() {}

    /** Null puts back the empty provider. */
    public static void install(Provider slots) {
        provider = slots == null ? NONE : slots;
    }

    public static List<ItemStack> of(EntityLivingBase living) {
        return counted(provider.worn(living));
    }

    public static boolean replace(EntityLivingBase living, ItemStack worn, ItemStack replacement) {
        return provider.replace(living, worn, replacement);
    }

    static List<ItemStack> counted(List<ItemStack> worn) {
        List<ItemStack> ours = new ArrayList<>();
        for (ItemStack stack : worn) if (stack != null && stack.getItem() instanceof Accessory) ours.add(stack);
        return Distinct.of(ours, WornAccessories::identity, s -> ((Accessory) s.getItem()).stacksWithCopies(s));
    }

    /** Damage is wear rather than a different accessory unless the item has subtypes. */
    private static Object identity(ItemStack stack) {
        Item item = stack.getItem();
        return item.getHasSubtypes() ? Arrays.asList(item, stack.getItemDamage()) : item;
    }
}
