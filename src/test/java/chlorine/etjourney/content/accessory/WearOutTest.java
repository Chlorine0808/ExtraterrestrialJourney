package chlorine.etjourney.content.accessory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WearOutTest {

    @Test
    void usesOneSecondOfLifeEachSecond() {
        assertEquals(1, WearOut.afterOneSecond(0, 300));
        assertEquals(299, WearOut.afterOneSecond(298, 300));
    }

    @Test
    void isSpentWhenTheLastSecondIsUsed() {
        assertEquals(WearOut.SPENT, WearOut.afterOneSecond(299, 300));
    }

    @Test
    void anOverusedStackIsSpentToo() {
        assertEquals(WearOut.SPENT, WearOut.afterOneSecond(450, 300));
    }

    @Test
    void anAccessoryWithoutALifetimeNeverWears() {
        assertEquals(0, WearOut.afterOneSecond(0, 0));
    }
}
