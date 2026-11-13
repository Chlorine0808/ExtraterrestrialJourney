package chlorine.etjourney.content.accessory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

class TalismanWearTest {

    private static ItemStack used(int seconds) {
        Item item = new Item();
        item.setMaxDamage(Talisman.RHENIUM.lifetime);
        return new ItemStack(item, 1, seconds);
    }

    @Test
    void aWearingTalismanIsReplacedByAMoreWornCopy() {
        ItemStack worn = used(10);
        ItemStack after = Talisman.RHENIUM.wear(worn);
        assertNotSame(worn, after);
        assertEquals(11, after.getItemDamage());
        assertEquals(10, worn.getItemDamage());
    }

    @Test
    void theLastSecondEmptiesTheSlot() {
        assertNull(Talisman.RHENIUM.wear(used(Talisman.RHENIUM.lifetime - 1)));
    }

    @Test
    void aLastingTalismanLeavesTheSlotAlone() {
        ItemStack worn = new ItemStack(new Item());
        assertSame(worn, Talisman.SAKURA_GOLD.wear(worn));
    }
}
