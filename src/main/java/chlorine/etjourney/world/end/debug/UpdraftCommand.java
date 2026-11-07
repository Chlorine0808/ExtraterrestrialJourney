package chlorine.etjourney.world.end.debug;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

import chlorine.etjourney.world.updraft.UpdraftWorld;
import chlorine.etjourney.world.updraft.model.Updraft;

/** /etj updraft add|remove|list: trial updrafts at the player's feet, until the server stops. */
final class UpdraftCommand {

    static final String USAGE = "/etj updraft <add [height] [lift] [max]|remove <id|all>|list>";
    /** Half the width of a trial column, around the block the player stands in. */
    private static final int HALF = 2;
    private static final int HEIGHT = 24;
    private static final double LIFT = 0.15;
    private static final double MAX_SPEED = 0.6;

    private UpdraftCommand() {}

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
        if (args.length == 2) return Arrays.asList("add", "remove", "list");
        if (args.length == 3 && args[1].equals("remove")) return Collections.singletonList("all");
        return Collections.emptyList();
    }

    private static void add(EntityPlayerMP player, int height, double lift, double maxSpeed) {
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        Updraft u;
        try {
            u = new Updraft(x - HALF, y, z - HALF, x + HALF + 1, y + height, z + HALF + 1, lift, maxSpeed);
        } catch (IllegalArgumentException e) {
            say(player, e.getMessage());
            return;
        }
        int id = UpdraftWorld.SERVER.add(player.dimension, u);
        say(player, "Updraft " + id + " " + u);
    }

    private static void remove(EntityPlayerMP player, String which) {
        if (which.equals("all")) {
            UpdraftWorld.SERVER.clear(player.dimension);
            say(player, "Removed every updraft in this dimension");
            return;
        }
        int id = Integer.parseInt(which);
        say(
            player,
            UpdraftWorld.SERVER.remove(player.dimension, id) ? "Removed updraft " + id
                : "No updraft " + id + " in this dimension");
    }

    private static void list(EntityPlayerMP player) {
        Map<Integer, Updraft> here = UpdraftWorld.SERVER.in(player.dimension);
        if (here.isEmpty()) say(player, "No updrafts in this dimension");
        for (Map.Entry<Integer, Updraft> e : here.entrySet()) say(player, "Updraft " + e.getKey() + " " + e.getValue());
    }

    private static void say(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText(text));
    }
}
