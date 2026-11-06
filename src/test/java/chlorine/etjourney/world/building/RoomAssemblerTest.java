package chlorine.etjourney.world.building;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Joining rooms at their '@' joints into one building. */
class RoomAssemblerTest {

    private final Map<String, Template> rooms = new HashMap<>();

    private void room(String name, String... rows) throws TemplateException {
        StringBuilder json = new StringBuilder("[{\"layers\":[{\"rows\":[");
        for (int i = 0; i < rows.length; i++) json.append(i == 0 ? "" : ",")
            .append('"')
            .append(rows[i])
            .append('"');
        json.append("]}],\"mappings\":{\"A\":\"m:a\",\"@\":\"m:seal\"}}]");
        rooms.put(name, TemplateParser.parse(name, new StringReader(json.toString()), true));
    }

    private List<Placement> assemble(String start, int maxDepth, int maxRooms, long seed, String... pool) {
        RoomSet set = new RoomSet("set", start, Arrays.asList(pool), maxDepth, maxRooms);
        return RoomAssembler.assemble(set, rooms::get, Rotation.R0, 0, 64, 0, seed);
    }

    private static boolean overlap(Placement a, Placement b) {
        return a.minX() <= b.maxX() && b.minX() <= a.maxX()
            && a.bottomY() <= b.maxY()
            && b.bottomY() <= a.maxY()
            && a.minZ() <= b.maxZ()
            && b.minZ() <= a.maxZ();
    }

    private static BuildingWriterTest.MemoryTarget write(List<Placement> placed) {
        BuildingWriterTest.MemoryTarget target = new BuildingWriterTest.MemoryTarget();
        for (Placement p : placed) BuildingWriter.writeAll(p, target);
        return target;
    }

    @Test
    void joinsFaceToFaceTurningTheNewRoom() throws Exception {
        // Start opens east; the only room opens north, so it must turn to face west.
        room("start", "AAA", "AA@", "AAA");
        room("cap", "A@A", "AAA", "AAA");
        List<Placement> placed = assemble("start", 4, 10, 1, "cap");
        assertEquals(2, placed.size());
        assertFalse(overlap(placed.get(0), placed.get(1)));
        BuildingWriterTest.MemoryTarget target = write(placed);
        // Start covers x -1..1; its joint is at x 1 and the cap's joint right outside at x 2.
        assertEquals("air", target.at(1, 64, 0));
        assertEquals("air", target.at(2, 64, 0));
        assertEquals("m:a:0", target.at(3, 64, 0));
    }

    @Test
    void skipsRoomsThatWouldOverlap() throws Exception {
        room("hub", "A@A", "@A@", "A@A");
        // A wide dead end: hung on two neighbouring sides of the hub, two of them collide.
        room("wide", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAA@AAA");
        List<Placement> placed = assemble("hub", 4, 10, 3, "wide");
        assertEquals(3, placed.size());
        for (int i = 0; i < placed.size(); i++)
            for (int j = i + 1; j < placed.size(); j++) assertFalse(overlap(placed.get(i), placed.get(j)));
    }

    @Test
    void unjoinedJointsStaySealed() throws Exception {
        room("hub", "A@A", "@A@", "A@A");
        room("wide", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAA@AAA");
        BuildingWriterTest.MemoryTarget target = write(assemble("hub", 4, 10, 3, "wide"));
        int sealed = 0;
        for (int[] c : new int[][] { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } })
            if ("m:seal:0".equals(target.at(c[0], 64, c[1]))) sealed++;
        assertEquals(2, sealed);
    }

    @Test
    void stopsAtTheDepthLimit() throws Exception {
        room("start", "AAA", "AA@", "AAA");
        room("hall", "AAA", "@A@", "AAA");
        assertEquals(1 + 3, assemble("start", 3, 100, 5, "hall").size());
    }

    @Test
    void stopsAtTheRoomLimit() throws Exception {
        room("start", "AAA", "AA@", "AAA");
        room("hall", "AAA", "@A@", "AAA");
        assertEquals(5, assemble("start", 100, 5, 5, "hall").size());
    }

    @Test
    void sameSeedSameBuilding() throws Exception {
        room("hub", "A@A", "@A@", "A@A");
        room("hall", "AAA", "@A@", "AAA");
        room("wide", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAA@AAA");
        assertEquals(
            write(assemble("hub", 4, 12, 9, "hall", "wide")).blocks,
            write(assemble("hub", 4, 12, 9, "hall", "wide")).blocks);
    }

    @Test
    void missingStartGivesNothing() {
        assertTrue(assemble("nope", 4, 10, 1, "nope").isEmpty());
    }
}
