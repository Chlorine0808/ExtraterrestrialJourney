package chlorine.etjourney.world.end.building;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.building.Placement;
import chlorine.etjourney.world.building.Rotation;
import chlorine.etjourney.world.building.Template;
import chlorine.etjourney.world.building.TemplateParser;
import chlorine.etjourney.world.end.reserve.Area;

/** A building keeps off the coast an HEE island lowers, not only off the island itself. */
class FadedGroundTest {

    private static final List<Area> ISLAND = Collections.singletonList(new Area("test", 0, 0, 100));

    private static Placement fiveByFiveAt(int x, int y) throws Exception {
        String row = "\"AAAAA\"";
        String json = "[{\"layers\":[{\"rows\":[" + row
            + ","
            + row
            + ","
            + row
            + ","
            + row
            + ","
            + row
            + "]}],\"mappings\":{\"A\":\"m:a\"}}]";
        Template t = TemplateParser.parse("t", new StringReader(json), true);
        return new Placement(t, Rotation.R0, x, y, 0, null, 0, 0);
    }

    @Test
    void coastNextToTheIslandIsFaded() throws Exception {
        assertTrue(EndBuildings.onFadedGround(ISLAND, fiveByFiveAt(127, 64)));
    }

    @Test
    void farAwayIsNot() throws Exception {
        assertFalse(EndBuildings.onFadedGround(ISLAND, fiveByFiveAt(1000, 64)));
    }

    @Test
    void highAboveTheIslandsIsNot() throws Exception {
        assertFalse(EndBuildings.onFadedGround(ISLAND, fiveByFiveAt(127, 200)));
    }
}
