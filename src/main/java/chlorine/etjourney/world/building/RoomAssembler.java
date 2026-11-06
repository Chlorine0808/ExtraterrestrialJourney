package chlorine.etjourney.world.building;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Grows a building from a room set: breadth first from the start room, each open joint takes a random room whose
 * joint fits, turned to face it, unless the room would overlap one already placed.
 */
public final class RoomAssembler {

    private RoomAssembler() {}

    /** The rooms placed, the start first; empty when the start room cannot be read. */
    public static List<Placement> assemble(RoomSet set, Function<String, Template> templates, Rotation rotation, int x,
        int y, int z, long seed) {
        Template start = templates.apply(set.start);
        if (start == null) return Collections.emptyList();
        Random random = new Random(seed);
        List<Placement> placed = new ArrayList<>();
        placed.add(new Placement(start, rotation, x, y, z, null, 0, seed));
        ArrayDeque<Open> open = new ArrayDeque<>();
        for (Joint j : start.joints) open.add(new Open(placed.get(0), j, 1));

        List<Template> pool = new ArrayList<>();
        for (String name : set.pool) {
            Template t = templates.apply(name);
            if (t != null) pool.add(t);
        }
        while (!open.isEmpty() && placed.size() < set.maxRooms) {
            Open o = open.poll();
            if (o.depth > set.maxDepth) continue;
            Placement room = join(o, pool, placed, random, seed + placed.size());
            if (room == null) continue;
            placed.add(room);
            for (Joint j : room.template.joints) if (!room.isOpen(j)) open.add(new Open(room, j, o.depth + 1));
        }
        return placed;
    }

    private static Placement join(Open o, List<Template> pool, List<Placement> placed, Random random, long salt) {
        int[] facing = o.room.rotation.turn(o.joint.dx, 0, o.joint.dz);
        // The cells right outside the open joint, by their lowest corner.
        int[] target = lowest(o.room.rotation, o.room.template, o.joint);
        target[0] += o.room.anchorX + facing[0];
        target[1] += o.room.anchorY;
        target[2] += o.room.anchorZ + facing[2];

        List<Template> rooms = new ArrayList<>();
        List<Joint> joints = new ArrayList<>();
        for (Template t : pool) for (Joint j : t.joints) if (j.fits(o.joint)) {
            rooms.add(t);
            joints.add(j);
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rooms.size(); i++) order.add(i);
        Collections.shuffle(order, random);
        for (int i : order) {
            Template t = rooms.get(i);
            Joint j = joints.get(i);
            Rotation turn = facing(j, -facing[0], -facing[2]);
            int[] corner = lowest(turn, t, j);
            Placement room = new Placement(
                t,
                turn,
                target[0] - corner[0],
                target[1] - corner[1],
                target[2] - corner[2],
                null,
                0,
                salt);
            if (overlapsAny(room, placed)) continue;
            o.room.open(o.joint);
            room.open(j);
            return room;
        }
        return null;
    }

    /** The turn that makes the joint face (dx, dz). */
    private static Rotation facing(Joint j, int dx, int dz) {
        for (Rotation r : Rotation.values()) {
            int[] f = r.turn(j.dx, 0, j.dz);
            if (f[0] == dx && f[2] == dz) return r;
        }
        throw new IllegalStateException("no turn faces a joint along " + dx + "," + dz);
    }

    /** The lowest corner of the joint's turned cells, relative to the anchor. */
    private static int[] lowest(Rotation r, Template t, Joint j) {
        int[] min = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
        for (int[] c : j.cells) {
            int[] w = r.toWorld(t, c[0], c[1], c[2]);
            for (int a = 0; a < 3; a++) min[a] = Math.min(min[a], w[a]);
        }
        return min;
    }

    private static boolean overlapsAny(Placement p, List<Placement> placed) {
        for (Placement q : placed) if (p.minX() <= q.maxX() && q.minX() <= p.maxX()
            && p.bottomY() <= q.maxY()
            && q.bottomY() <= p.maxY()
            && p.minZ() <= q.maxZ()
            && q.minZ() <= p.maxZ()) return true;
        return false;
    }

    private static final class Open {

        final Placement room;
        final Joint joint;
        /** Depth of the room that would join here. */
        final int depth;

        Open(Placement room, Joint joint, int depth) {
            this.room = room;
            this.joint = joint;
            this.depth = depth;
        }
    }
}
