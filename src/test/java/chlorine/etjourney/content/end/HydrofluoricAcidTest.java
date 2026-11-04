package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Coltan splits into Tantalum and Niobium, what the acid makes or cannot eat stays, and the rest dissolves. */
class HydrofluoricAcidTest {

    @Test
    void oneColtanSplitsIntoOneTantalumAndOneNiobium() {
        for (String coltan : new String[] { "etjourney:raw_coltan", "etjourney:coltan_ore" }) {
            assertEquals(HydrofluoricAcid.Outcome.SPLIT, HydrofluoricAcid.outcome(coltan));
            assertEquals(
                Arrays.asList("etjourney:raw_tantalum", "etjourney:raw_niobium"),
                HydrofluoricAcid.products(coltan));
        }
    }

    @Test
    void whatTheAcidMakesStays() {
        // Otherwise the products would dissolve in the pool they were split in.
        for (String product : HydrofluoricAcid.products("etjourney:raw_coltan")) {
            assertEquals(HydrofluoricAcid.Outcome.KEEP, HydrofluoricAcid.outcome(product));
        }
    }

    @Test
    void theBucketsThatHoldItStay() {
        assertEquals(HydrofluoricAcid.Outcome.KEEP, HydrofluoricAcid.outcome("etjourney:tantalum_bucket"));
        assertEquals(
            HydrofluoricAcid.Outcome.KEEP,
            HydrofluoricAcid.outcome("etjourney:tantalum_bucket_hydrofluoric_acid"));
    }

    @Test
    void everythingElseDissolves() {
        for (String other : new String[] { "minecraft:dirt", "minecraft:bucket", "etjourney:raw_osmium",
            "othermod:raw_coltan", "etjourney:ingot_niobium" }) {
            assertEquals(HydrofluoricAcid.Outcome.DISSOLVE, HydrofluoricAcid.outcome(other), other);
            assertTrue(
                HydrofluoricAcid.products(other)
                    .isEmpty(),
                other);
        }
    }

    @Test
    void itHurtsOnceASecond() {
        int hurts = 0;
        for (long tick = 0; tick < 200; tick++) if (HydrofluoricAcid.hurtsOn(tick)) hurts++;
        assertEquals(10, hurts);
        assertTrue(HydrofluoricAcid.hurtsOn(40));
        assertEquals(2.0F, HydrofluoricAcid.DAMAGE);
    }
}
