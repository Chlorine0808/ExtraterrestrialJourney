package chlorine.etjourney.world.end.debug;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import chlorine.etjourney.world.wind.WindWorld;
import chlorine.etjourney.world.wind.model.Wind;

/** /etj wind add|aim|remove|list: trial winds at the player's feet, until the server stops. */
final class WindCommand {

    static final String USAGE = "/etj wind <add [height] [lift] [max]|aim [size] [lift] [max]|remove <id|all>|list>";
    /** Half the width of a trial column, around the block the player stands in. */
    private static final int HALF = 2;
    private static final int HEIGHT = 24;
    private static final double LIFT = 0.15;
    private static final double MAX_SPEED = 0.6;
    /** Side of a trial cube that blows where the player looks. */
    private static final int AIM_SIZE = 9;

    private WindCommand() {}

    static void run(EntityPlayerMP player, String[] args) {
        String what = args.length > 1 ? args[1] : "";
        try {
            switch (what) {
                case "add":
                    add(
                        player,
                        args.length > 2 ? Integer.parseInt(args[2]) : HEIGHT,
                        args.length > 3 ? Double.parseDouble(args[3]) : LIFT,
                        args.length > 4 ? Double.parseDouble(args[4]) : MAX_SPEED);
                    return;
                case "aim":
                    aim(
                        player,
                        args.length > 2 ? Integer.parseInt(args[2]) : AIM_SIZE,
                        args.length > 3 ? Double.parseDouble(args[3]) : LIFT,
                        args.length > 4 ? Double.parseDouble(args[4]) : MAX_SPEED);
                    return;
                case "remove":
                    if (args.length < 3) say(player, "Usage: " + USAGE);
                    else remove(player, args[2]);
                    return;
                case "list":
                    list(player);
                    return;
                default:
                    say(player, "Usage: " + USAGE);
            }
        } catch (NumberFormatException e) {
            say(player, "Not a number: " + e.getMessage());
        }
    }

    static List<String> complete(String[] args) {
        if (args.length == 2) return Arrays.asList("add", "aim", "remove", "list");
        if (args.length == 3 && args[1].equals("remove")) return Collections.singletonList("all");
        return Collections.emptyList();
    }

    private static void add(EntityPlayerMP player, int height, double lift, double maxSpeed) {
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        Wind u;
        try {
            u = new Wind(x - HALF, y, z - HALF, x + HALF + 1, y + height, z + HALF + 1, lift, maxSpeed);
        } catch (IllegalArgumentException e) {
            say(player, e.getMessage());
            return;
        }
        put(player, u);
    }

    /** A cube centred on the player's feet, blowing along their look. */
    private static void aim(EntityPlayerMP player, int size, double lift, double maxSpeed) {
        Vec3 look = player.getLookVec();
        double h = size / 2.0;
        Wind u;
        try {
            u = new Wind(
                player.posX - h,
                player.posY - h,
                player.posZ - h,
                player.posX + h,
                player.posY + h,
                player.posZ + h,
                look.xCoord,
                look.yCoord,
                look.zCoord,
                lift,
                maxSpeed);
        } catch (IllegalArgumentException e) {
            say(player, e.getMessage());
            return;
        }
        put(player, u);
    }

    private static void put(EntityPlayerMP player, Wind u) {
        int id = WindWorld.SERVER.add(player.dimension, u);
        say(player, "Wind " + id + " " + u);
    }

    private static void remove(EntityPlayerMP player, String which) {
        if (which.equals("all")) {
            WindWorld.SERVER.clear(player.dimension);
            say(player, "Removed every wind in this dimension");
            return;
        }
        int id = Integer.parseInt(which);
        say(
            player,
            WindWorld.SERVER.remove(player.dimension, id) ? "Removed wind " + id
                : "No wind " + id + " in this dimension");
    }

    private static void list(EntityPlayerMP player) {
        Map<Integer, Wind> here = WindWorld.SERVER.in(player.dimension);
        if (here.isEmpty()) say(player, "No winds in this dimension");
        for (Map.Entry<Integer, Wind> e : here.entrySet()) say(player, "Wind " + e.getKey() + " " + e.getValue());
    }

    private static void say(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText(text));
    }
}
