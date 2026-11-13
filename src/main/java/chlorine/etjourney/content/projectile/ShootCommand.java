package chlorine.etjourney.content.projectile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;

/** /etjshoot: fires a shot from the player's eyes, or from ahead of them back at them as an enemy shot. */
public final class ShootCommand extends CommandBase {

    private static final String USAGE = "/etjshoot <type> [me]";
    /** How far ahead an enemy shot starts. */
    private static final double AHEAD = 12;

    @Override
    public String getCommandName() {
        return "etjshoot";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ProjectileSpec spec = args.length > 0 ? ProjectileTypes.get(args[0]) : null;
        if (spec == null) {
            player.addChatMessage(new ChatComponentText("Usage: " + USAGE + " " + ProjectileTypes.names()));
            return;
        }
        Vec3 look = player.getLookVec();
        double[] dir = { look.xCoord, look.yCoord, look.zCoord };
        double eyeY = player.posY + player.getEyeHeight();
        if (args.length > 1 && args[1].equals("me")) Projectiles.shootAt(
            spec,
            player.worldObj,
            null,
            player.posX + dir[0] * AHEAD,
            eyeY + dir[1] * AHEAD,
            player.posZ + dir[2] * AHEAD,
            player);
        else Projectiles.shootAlong(spec, player.worldObj, player, player.posX, eyeY - 0.1, player.posZ, dir);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        String last = args[args.length - 1];
        List<String> options = args.length == 1 ? ProjectileTypes.names() : Arrays.asList("me");
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.startsWith(last)) out.add(o);
        return out;
    }
}
