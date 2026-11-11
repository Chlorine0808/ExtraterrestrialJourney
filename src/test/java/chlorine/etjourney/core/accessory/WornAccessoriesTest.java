package chlorine.etjourney.core.accessory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

class WornAccessoriesTest {

    private static class Plain extends Item implements Accessory {
    }

    private static final class WithSubtypes extends Item implements Accessory {

        WithSubtypes() {
            setHasSubtypes(true);
        }
    }

    private static final class Stacking extends Item implements Accessory {

        @Override
        public boolean stacksWithCopies(ItemStack stack) {
            return true;
        }
    }

    private static List<ItemStack> counted(ItemStack... worn) {
        return WornAccessories.counted(Arrays.asList(worn));
    }

    @Test
    void leavesOutEmptySlotsAndOtherModsAccessories() {
        ItemStack ours = new ItemStack(new Plain());
        assertEquals(Collections.singletonList(ours), counted(null, new ItemStack(new Item()), ours));
    }

    @Test
    void wornDownCopiesAreStillTheSameAccessory() {
        Item plain = new Plain();
        ItemStack fresh = new ItemStack(plain, 1, 0), used = new ItemStack(plain, 1, 120);
        assertEquals(Collections.singletonList(fresh), counted(fresh, used));
    }

    @Test
    void subtypesAreDifferentAccessories() {
        Item item = new WithSubtypes();
        ItemStack red = new ItemStack(item, 1, 0), blue = new ItemStack(item, 1, 1), red2 = new ItemStack(item, 1, 0);
        assertEquals(Arrays.asList(red, blue), counted(red, blue, red2));
    }

    @Test
    void anAccessoryMayDeclareThatCopiesStack() {
        Item item = new Stacking();
        ItemStack first = new ItemStack(item), second = new ItemStack(item);
        assertEquals(Arrays.asList(first, second), counted(first, second));
    }
}
