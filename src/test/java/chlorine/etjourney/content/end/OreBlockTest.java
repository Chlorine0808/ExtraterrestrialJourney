package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * Midgame and endgame ores must survive explosions and the Wither, so a creeper cannot mine them for a player who
 * lacks the pickaxe. Early ores keep the stone-like resistance they had.
 */
class OreBlockTest {

    // Block.getExplosionResistance gives back 3/5 of what setResistance took; bedrock sets 6000000 and these ores 15.
    // Vanilla blocks are not registered in unit tests, so the figures stand here.
    private static final float BEDROCK = 6000000.0F * 3 / 5;
    private static final float STONE_LIKE = 15.0F * 3 / 5;

    private static OreBlock ore(boolean blastproof) {
        return new OreBlock(
            new OreBlockTable.Entry(
                "test_ore",
                EndBlock.ENDER_GOLD_ORE,
                3.0F,
                1,
                "raw_test",
                1,
                1,
                "oreTest",
                "rawTest",
                blastproof),
            Items.stick);
    }

    @Test
    void blastproofOreResistsLikeBedrock() {
        OreBlock block = ore(true);
        assertTrue(block.getExplosionResistance(null) >= BEDROCK);
        assertFalse(block.canEntityDestroy(null, 0, 0, 0, null));
    }

    @Test
    void plainOreGivesWay() {
        OreBlock block = ore(false);
        assertEquals(STONE_LIKE, block.getExplosionResistance(null), 1e-4);
        assertTrue(block.canEntityDestroy(null, 0, 0, 0, null));
    }

    @Test
    void everyOreSoFarIsAnEarlyOne() {
        for (OreBlockTable.Entry e : OreBlockTable.ENTRIES) assertFalse(e.blastproof, e.name);
    }
}
