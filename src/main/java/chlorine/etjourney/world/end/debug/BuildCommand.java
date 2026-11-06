package chlorine.etjourney.world.end.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import chlorine.etjourney.world.building.BuildingWriter;
import chlorine.etjourney.world.building.Placement;
import chlorine.etjourney.world.building.RoomAssembler;
import chlorine.etjourney.world.building.RoomSet;
import chlorine.etjourney.world.building.Rotation;
import chlorine.etjourney.world.building.Template;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.building.BuildingKind;
import chlorine.etjourney.world.end.building.EndBuildings;
import chlorine.etjourney.world.end.building.WorldTarget;
import chlorine.etjourney.world.end.reserve.Reservations;

/** /etj build place|list|find: trying templates in game and finding the generated buildings. */
final class BuildCommand {

    static final String USAGE = "/etj build <place <name> [0|90|180|270]|list|find [id]>";
    private static final int END = 1;
    private static final int FIND_RADIUS = 4096;
    /** Gap between the player and the near side of a placed building. */
    private static final int GAP = 3;

    private BuildCommand() {}

    static void run(EntityPlayerMP player, String[] args) {
        String what = args.length > 1 ? args[1] : "";
        switch (what) {
            case "place":
                if (args.length < 3) say(player, "Usage: " + USAGE);
                else place(player, args[2], args.length > 3 ? args[3] : "0");
                return;
            case "list":
                list(player);
                return;
            case "find":
                find(player, args.length > 2 ? args[2] : null);
                return;
            default:
                say(player, "Usage: " + USAGE);
        }
    }

    static List<String> complete(String[] args) {
        if (args.length == 2) return Arrays.asList("place", "list", "find");
        if (args.length == 3 && (args[1].equals("place") || args[1].equals("find"))) {
            List<String> names = new ArrayList<>();
            for (BuildingKind kind : EndBuildings.kinds())
                names.add(args[1].equals("place") ? kind.templateName : kind.id);
            if (args[1].equals("place")) for (RoomSet set : EndBuildings.roomSets()) names.add(set.id);
            return names;
        }
        if (args.length == 4 && args[1].equals("place")) return Arrays.asList("0", "90", "180", "270");
        return Collections.emptyList();
    }

    private static void place(EntityPlayerMP player, String name, String degrees) {
        Rotation rotation;
        try {
            rotation = Rotation.of(Integer.parseInt(degrees));
        } catch (IllegalArgumentException e) {
            say(player, "Turn must be 0, 90, 180 or 270");
            return;
        }
        // Read from disk again, so a fresh scan shows up without a restart.
        EndBuildings.scans()
            .clear();
        RoomSet rooms = EndBuildings.roomSet(name);
        if (rooms != null) {
            placeRooms(player, rooms, rotation);
            return;
        }
        boolean blankIsAir = true;
        for (BuildingKind kind : EndBuildings.kinds()) if (kind.templateName.equals(name)) blankIsAir = kind.blankIsAir;
        Template t = EndBuildings.scans()
            .get(name, blankIsAir);
        if (t == null) {
            say(
                player,
                "Cannot read " + name
                    + ": "
                    + EndBuildings.scans()
                        .failures()
                        .get(name));
            return;
        }
        // Facing 0 south, 1 west, 2 north, 3 east.
        int facing = MathHelper.floor_double(player.rotationYaw * 4 / 360 + 0.5) & 3;
        int[][] ahead = { { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 0 } };
        int distance = GAP + Math.max(t.sizeX, t.sizeZ) / 2 + 1;
        int x = MathHelper.floor_double(player.posX) + ahead[facing][0] * distance;
        int z = MathHelper.floor_double(player.posZ) + ahead[facing][1] * distance;
        int y = MathHelper.floor_double(player.posY) + t.anchorY;
        Placement p = new Placement(t, rotation, x, y, z, null, 0, x * 31L + z);
        int written = BuildingWriter.writeAll(p, new WorldTarget(player.worldObj));
        say(
            player,
            "Placed " + name
                + " ("
                + t.sizeX
                + "x"
                + t.sizeY
                + "x"
                + t.sizeZ
                + ", "
                + degrees
                + ") at "
                + x
                + " "
                + y
                + " "
                + z
                + "; "
                + written
                + " blocks");
    }

    /** Grows a fresh building from the room set each time, its start room ahead of the player. */
    private static void placeRooms(EntityPlayerMP player, RoomSet set, Rotation rotation) {
        int facing = MathHelper.floor_double(player.rotationYaw * 4 / 360 + 0.5) & 3;
        int[][] ahead = { { 0, 1 }, { -1, 0 }, { 0, -1 }, { 1, 0 } };
        Template start = EndBuildings.scans()
            .get(set.start, true);
        if (start == null) {
            say(
                player,
                "Cannot read " + set.start
                    + ": "
                    + EndBuildings.scans()
                        .failures()
                        .get(set.start));
            return;
        }
        int distance = GAP + Math.max(start.sizeX, start.sizeZ) / 2 + 1;
        int x = MathHelper.floor_double(player.posX) + ahead[facing][0] * distance;
        int z = MathHelper.floor_double(player.posZ) + ahead[facing][1] * distance;
        int y = MathHelper.floor_double(player.posY) + start.anchorY;
        long seed = player.worldObj.rand.nextLong();
        List<Placement> placed = RoomAssembler.assemble(
            set,
            name -> EndBuildings.scans()
                .get(name, true),
            rotation,
            x,
            y,
            z,
            seed);
        WorldTarget target = new WorldTarget(player.worldObj);
        int written = 0;
        for (Placement p : placed) written += BuildingWriter.writeAll(p, target);
        say(
            player,
            "Placed " + set.id
                + " ("
                + placed.size()
                + " rooms) at "
                + x
                + " "
                + y
                + " "
                + z
                + "; "
                + written
                + " blocks");
        for (Map.Entry<String, String> e : EndBuildings.scans()
            .failures()
            .entrySet()) say(player, "Failed: " + e.getKey() + ": " + e.getValue());
    }

    private static void list(EntityPlayerMP player) {
        for (BuildingKind kind : EndBuildings.kinds()) {
            Template t = EndBuildings.template(kind);
            say(
                player,
                kind.id + " <- "
                    + kind.templateName
                    + (t == null ? " (unreadable)" : " " + t.sizeX + "x" + t.sizeY + "x" + t.sizeZ));
        }
        for (Map.Entry<String, String> e : EndBuildings.store()
            .failures()
            .entrySet()) say(player, "Failed: " + e.getKey() + ": " + e.getValue());
    }

    private static void find(EntityPlayerMP player, String id) {
        if (player.dimension != END) {
            say(player, "Run this in the End");
            return;
        }
        if (id != null && EndBuildings.kind(id) == null) {
            say(player, "No building kind " + id);
            return;
        }
        Long seed = EndTerrain.seed();
        if (seed == null) return;
        WorldServer end = (WorldServer) player.worldObj;
        List<EndBuildings.Found> found = EndBuildings.nearest(
            (cx, cz) -> Reservations.forChunk(end.theChunkProviderServer.currentChunkProvider, end, seed, cx, cz),
            seed,
            MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.posZ),
            id,
            FIND_RADIUS,
            1);
        if (found.isEmpty()) {
            say(player, "No building within " + FIND_RADIUS + " blocks");
            return;
        }
        Placement p = found.get(0).placement;
        say(
            player,
            found.get(0).kind.id + " at "
                + p.anchorX
                + " "
                + p.anchorY
                + " "
                + p.anchorZ
                + ", turned "
                + p.rotation.quarterTurns() * 90);
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
