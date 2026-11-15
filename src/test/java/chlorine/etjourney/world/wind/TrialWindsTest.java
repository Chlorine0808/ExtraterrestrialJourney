package chlorine.etjourney.world.wind;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.wind.model.Wind;

/** Winds put by command are saved with the world, box, direction and strength alike, and in their dimension. */
class TrialWindsTest {

    private static final double EPSILON = 1e-9;

    @Test
    void aSavedWindComesBackAsItWas() {
        TrialWinds saved = new TrialWinds();
        saved.put(1, new Wind(-1.5, 60, 3, 7.5, 69, 12, 0.6, -0.2, 0.8, 0.15, 0.6));
        saved.put(-1, new Wind(0, 10, 0, 5, 34, 5, 0.1, 0.4));
        NBTTagCompound tag = new NBTTagCompound();
        saved.writeToNBT(tag);

        TrialWinds loaded = new TrialWinds();
        loaded.readFromNBT(tag);
        List<TrialWinds.Entry> entries = loaded.entries();
        assertEquals(2, entries.size());
        assertEquals(1, entries.get(0).dimension);
        Wind aimed = entries.get(0).wind;
        assertEquals(-1.5, aimed.minX, EPSILON);
        assertEquals(69, aimed.maxY, EPSILON);
        assertEquals(new Wind(0, 0, 0, 1, 1, 1, 0.6, -0.2, 0.8, 1, 1).dirX, aimed.dirX, EPSILON);
        assertEquals(0.15, aimed.lift, EPSILON);
        assertEquals(0.6, aimed.maxSpeed, EPSILON);
        assertEquals(-1, entries.get(1).dimension);
        assertEquals(1, entries.get(1).wind.dirY, EPSILON);
    }

    @Test
    void removingAWindForgetsOnlyIt() {
        TrialWinds saved = new TrialWinds();
        TrialWinds.Entry first = saved.put(0, new Wind(0, 0, 0, 1, 1, 1, 0.1, 0.4));
        saved.put(0, new Wind(5, 0, 0, 6, 1, 1, 0.1, 0.4));
        saved.forget(first);
        assertEquals(
            1,
            saved.entries()
                .size());
        assertEquals(
            5,
            saved.entries()
                .get(0).wind.minX,
            EPSILON);
    }

    @Test
    void clearingADimensionLeavesTheOthers() {
        TrialWinds saved = new TrialWinds();
        saved.put(0, new Wind(0, 0, 0, 1, 1, 1, 0.1, 0.4));
        saved.put(1, new Wind(0, 0, 0, 1, 1, 1, 0.1, 0.4));
        saved.forgetDimension(0);
        assertEquals(
            1,
            saved.entries()
                .size());
        assertEquals(
            1,
            saved.entries()
                .get(0).dimension);
    }
}
