package chlorine.etjourney.world.building;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Writing a placed building window by window, the way populate does. */
class BuildingWriterTest {

    /** Records what was written; air is "air". Unwritten positions are replaceable. */
    static final class MemoryTarget implements BlockTarget {

        final Map<String, String> blocks = new HashMap<>();
        int minWrittenY = Integer.MAX_VALUE, maxWrittenY = Integer.MIN_VALUE;
        int calls;

        private void record(int x, int y, int z, String what) {
            blocks.put(x + "," + y + "," + z, what);
            minWrittenY = Math.min(minWrittenY, y);
            maxWrittenY = Math.max(maxWrittenY, y);
            calls++;
        }

        @Override
        public void set(int x, int y, int z, BlockRef ref, Rotation rotation) {
            MetaRotation turn = MetaRotations.forName(ref.name);
            int meta = turn == null ? ref.meta : turn.rotate(ref.meta, rotation);
            record(x, y, z, ref.name + ":" + meta);
        }

        @Override
        public void setAir(int x, int y, int z) {
            record(x, y, z, "air");
        }

        @Override
        public boolean replaceable(int x, int y, int z) {
            return !blocks.containsKey(x + "," + y + "," + z);
        }

        String at(int x, int y, int z) {
            return blocks.get(x + "," + y + "," + z);
        }
    }

    private static Template template(String layers, String mappings, boolean blankIsAir) throws TemplateException {
        String json = "[{\"layers\":[" + layers + "],\"mappings\":{" + mappings + "}}]";
        return TemplateParser.parse("t", new StringReader(json), blankIsAir);
    }

    /** A solid sizeX x sizeY x sizeZ block of two random choices. */
    private static Template solid(int sx, int sy, int sz) throws TemplateException {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < sx; i++) row.append('A');
        StringBuilder rows = new StringBuilder("[");
        for (int z = 0; z < sz; z++) rows.append(z == 0 ? "" : ",")
            .append('"')
            .append(row)
            .append('"');
        rows.append(']');
        StringBuilder layers = new StringBuilder();
        for (int y = 0; y < sy; y++) layers.append(y == 0 ? "" : ",")
            .append("{\"rows\":")
            .append(rows)
            .append('}');
        return template(layers.toString(), "\"A\":{\"blocks\":[\"m:a\",\"m:b\"]}", true);
    }

    private static Placement at(Template t, Rotation r, int x, int y, int z, long salt) {
        return new Placement(t, r, x, y, z, null, 0, salt);
    }

    private static MemoryTarget byWindows(Placement p) {
        MemoryTarget target = new MemoryTarget();
        for (int wx = Math.floorDiv(p.minX() - 8, 16) * 16 + 8; wx <= p.maxX(); wx += 16)
            for (int wz = Math.floorDiv(p.minZ() - 8, 16) * 16 + 8; wz <= p.maxZ(); wz += 16)
                BuildingWriter.write(p, wx, wz, target);
        return target;
    }

    @Test
    void windowsTileToWholeBuilding() throws Exception {
        Placement p = at(solid(40, 30, 40), Rotation.R90, 1003, 70, -517, 42);
        MemoryTarget windows = byWindows(p);
        MemoryTarget whole = new MemoryTarget();
        BuildingWriter.writeAll(p, whole);
        assertEquals(40 * 30 * 40, whole.blocks.size());
        assertEquals(whole.blocks, windows.blocks);
    }

    @Test
    void writesNothingOutsideWindow() throws Exception {
        Placement p = at(solid(40, 3, 40), Rotation.R270, 100, 70, 100, 1);
        MemoryTarget target = new MemoryTarget();
        BuildingWriter.write(p, 88, 104, target);
        assertFalse(target.blocks.isEmpty());
        for (String key : target.blocks.keySet()) {
            String[] c = key.split(",");
            int x = Integer.parseInt(c[0]), z = Integer.parseInt(c[2]);
            assertTrue(x >= 88 && x < 104 && z >= 104 && z < 120, key);
        }
    }

    @Test
    void keepCellsAreNotTouched() throws Exception {
        Template t = template("{\"rows\":[\"A.A\"]}", "\"A\":\"m:a\"", true);
        MemoryTarget target = new MemoryTarget();
        BuildingWriter.writeAll(at(t, Rotation.R0, 0, 64, 0, 0), target);
        assertEquals(2, target.calls);
        assertEquals(null, target.at(0, 64, 0));
    }

    @Test
    void choicesAreStableAcrossWindowsAndFollowTheSalt() throws Exception {
        Template t = solid(40, 2, 40);
        Placement p = at(t, Rotation.R0, 500, 64, 500, 7);
        assertEquals(byWindows(p).blocks, byWindows(p).blocks);
        MemoryTarget a = new MemoryTarget(), b = new MemoryTarget();
        BuildingWriter.writeAll(p, a);
        BuildingWriter.writeAll(at(t, Rotation.R0, 500, 64, 500, 8), b);
        assertNotEquals(a.blocks, b.blocks);
        assertTrue(a.blocks.containsValue("m:a:0") && a.blocks.containsValue("m:b:0"));
    }

    @Test
    void clipsOutsideWorldHeight() throws Exception {
        MemoryTarget tower = new MemoryTarget();
        BuildingWriter.writeAll(at(solid(1, 20, 1), Rotation.R0, 0, 250, 0, 0), tower);
        assertEquals(255, tower.maxWrittenY);
        assertEquals(6, tower.calls);
        MemoryTarget sunk = new MemoryTarget();
        BuildingWriter
            .writeAll(new Placement(solid(3, 5, 3), Rotation.R0, 0, -3, 0, BlockRef.parse("m:f", null), 8, 0), sunk);
        assertEquals(1, sunk.minWrittenY);
    }

    @Test
    void fillsFoundationOnlyUnderSolidBottomCells() throws Exception {
        // Bottom row: A, blank (air), A, keep.
        Template t = template("{\"rows\":[\"AAAA\"]},{\"rows\":[\"A A.\"]}", "\"A\":\"m:a\"", true);
        MemoryTarget target = new MemoryTarget();
        // A block already below x 2 stops its foundation after two.
        target.blocks.put("2,61,0", "ground");
        BuildingWriter.writeAll(new Placement(t, Rotation.R0, 2, 64, 0, BlockRef.parse("m:f", null), 3, 0), target);
        // Anchor (2, 0, 0) at x 2: template x 0..3 lands on world x 0..3.
        for (int y = 61; y <= 63; y++) assertEquals("m:f:0", target.at(0, y, 0));
        assertEquals(null, target.at(0, 60, 0));
        assertEquals("m:f:0", target.at(2, 63, 0));
        assertEquals("m:f:0", target.at(2, 62, 0));
        assertEquals("ground", target.at(2, 61, 0));
        assertEquals(null, target.at(1, 63, 0));
        assertEquals(null, target.at(3, 63, 0));
    }

    @Test
    void noFoundationWhenNull() throws Exception {
        MemoryTarget target = new MemoryTarget();
        BuildingWriter.writeAll(at(solid(3, 1, 3), Rotation.R0, 0, 64, 0, 0), target);
        assertEquals(64, target.minWrittenY);
    }

    @Test
    void facingBlocksTurnWithTheBuilding() throws Exception {
        MetaRotations.register("m:stairs", MetaRotation.STAIRS);
        Template t = template("{\"rows\":[\"S\"]}", "\"S\":\"m:stairs:0\"", true);
        MemoryTarget target = new MemoryTarget();
        BuildingWriter.writeAll(at(t, Rotation.R90, 0, 64, 0, 0), target);
        assertEquals("m:stairs:2", target.at(0, 64, 0));
    }
}
