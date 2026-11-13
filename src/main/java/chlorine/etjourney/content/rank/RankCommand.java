package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

import chlorine.etjourney.content.rank.ability.RankAbilities;

/** /etjrank: spawns a ranked mob in front of the player, with chosen or rolled abilities. */
public final class RankCommand extends CommandBase {

    private static final String USAGE = "/etjrank <rare|epic|legendary> [mob] [ability...]";
    private static final String DEFAULT_MOB = "etjourney.spore_zombie";

    @Override
    public String getCommandName() {
        return "etjrank";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        Rank rank = args.length > 0 ? parse(args[0]) : null;
        if (rank == null || rank == Rank.NORMAL) {
            say(player, "Usage: " + USAGE);
            return;
        }
        String name = args.length > 1 ? args[1] : DEFAULT_MOB;
        Entity e = EntityList.createEntityByName(name, player.worldObj);
        if (!(e instanceof EntityLiving)) {
            say(player, "Not a mob: " + name);
            return;
        }
        EntityLiving mob = (EntityLiving) e;
        double yaw = Math.toRadians(player.rotationYaw);
        int x = MathHelper.floor_double(player.posX - 3 * Math.sin(yaw)),
            z = MathHelper.floor_double(player.posZ + 3 * Math.cos(yaw));
        mob.setLocationAndAngles(x + 0.5, player.posY, z + 0.5, player.rotationYaw + 180, 0);
        mob.onSpawnWithEgg(null);
        if (args.length > 2) EnemyRanks.give(
            mob,
            rank,
            Arrays.asList(args)
                .subList(2, args.length));
        else EnemyRanks.give(mob, rank);
        player.worldObj.spawnEntityInWorld(mob);
        say(player, "Spawned " + rank + " " + name + " " + RankAbilities.idsOf(mob));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        String last = args[args.length - 1];
        List<String> options;
        if (args.length == 1) options = Arrays.asList("rare", "epic", "legendary");
        else if (args.length == 2) options = names();
        else options = RankAbilities.ids();
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.startsWith(last)) out.add(o);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<String> names() {
        return new ArrayList<>((Set<String>) EntityList.stringToClassMapping.keySet());
    }

    private static Rank parse(String s) {
        try {
            return Rank.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void say(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText(text));
    }
}
