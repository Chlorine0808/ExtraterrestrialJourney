package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

class ShotsOutputTest {

    @Test
    void rootFallsBackToTheGameFolder() {
        File game = new File("run/client");
        assertEquals(new File(game, "etj-shots"), ShotsOutput.root(null, game));
        assertEquals(new File(game, "etj-shots"), ShotsOutput.root("  ", game));
    }

    @Test
    void relativeRootResolvesAgainstTheGameFolder() {
        File game = new File("run/client");
        assertEquals(
            new File(game, "../../../Main/run/client/etj-shots"),
            ShotsOutput.root(" ../../../Main/run/client/etj-shots ", game));
    }

    @Test
    void folderAppendsTheBranchAsOneSafeName() {
        assertEquals("20261104-120000", ShotsOutput.folder("20261104-120000", null));
        assertEquals("20261104-120000", ShotsOutput.folder("20261104-120000", " "));
        assertEquals(
            "20261104-120000_feature-end-lantern-woods",
            ShotsOutput.folder("20261104-120000", "feature/end-lantern-woods"));
        assertEquals("20261104-120000_a-b", ShotsOutput.folder("20261104-120000", "a:b"));
    }
}
