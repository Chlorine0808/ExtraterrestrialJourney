package chlorine.etjourney.core.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import chlorine.etjourney.core.accessory.Accessory;
import chlorine.etjourney.core.accessory.WornAccessories;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.Resistance;

class AccessoryResistanceTest {

    /** Halves neutral damage and takes 1 off it. */
    private static final class Ward extends Item implements Accessory, ElementalAccessory {

        @Override
        public void addResistance(ItemStack stack, EntityLivingBase wearer, Resistance.Builder out) {
            out.multiply(ElementRegistry.NEUTRAL, 0.5f)
                .subtract(ElementRegistry.NEUTRAL, 1f);
        }
    }

    private static Resistance wearing(ItemStack... worn) {
        List<ItemStack> slots = Arrays.asList(worn);
        WornAccessories.install(new WornAccessories.Provider() {

            @Override
            public List<ItemStack> worn(EntityLivingBase living) {
                return slots;
            }

            @Override
            public boolean replace(EntityLivingBase living, ItemStack old, ItemStack replacement) {
                return false;
            }
        });
        Resistance.Builder out = Resistance.builder();
        ResistanceSources.ACCESSORIES.collect(null, out);
        return out.build();
    }

    @AfterEach
    void uninstall() {
        WornAccessories.install(null);
    }

    @Test
    void differentAccessoriesMultiplyAndAdd() {
        Resistance r = wearing(new ItemStack(new Ward()), new ItemStack(new Ward()));
        assertEquals(0.25f, r.factor(ElementRegistry.NEUTRAL), 1e-6);
        assertEquals(2f, r.flat(ElementRegistry.NEUTRAL), 1e-6);
    }

    @Test
    void twoCopiesOfOneAccessoryCountOnce() {
        Item ward = new Ward();
        Resistance r = wearing(new ItemStack(ward), new ItemStack(ward));
        assertEquals(0.5f, r.factor(ElementRegistry.NEUTRAL), 1e-6);
        assertEquals(1f, r.flat(ElementRegistry.NEUTRAL), 1e-6);
    }

    @Test
    void anAccessoryWithoutResistanceAddsNothing() {
        Resistance r = wearing(new ItemStack(new Item() {}), new ItemStack(new PlainAccessory()));
        assertEquals(1f, r.factor(ElementRegistry.NEUTRAL), 1e-6);
        assertEquals(0f, r.flat(ElementRegistry.NEUTRAL), 1e-6);
    }

    private static final class PlainAccessory extends Item implements Accessory {
    }
}
