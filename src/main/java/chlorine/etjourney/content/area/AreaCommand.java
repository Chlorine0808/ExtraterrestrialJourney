package chlorine.etjourney.content.area;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/** /etjarea: leaves an area where the player looks or stands, as theirs or as an enemy's. */
public final class AreaCommand extends CommandBase {

    private static final String USAGE = "/etjarea <type> [enemy] [here]";
    private static final double REACH = 32;

    @Override
    public String getCommandName() {
        return "etjarea";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        AreaSpec spec = args.length > 0 ? AreaTypes.get(args[0]) : null;
        if (spec == null) {
            player.addChatMessage(new ChatComponentText("Usage: " + USAGE + " " + AreaTypes.names()));
            return;
        }
        List<String> flags = Arrays.asList(args)
            .subList(1, args.length);
        double[] at = flags.contains("here") ? new double[] { player.posX, player.boundingBox.minY + 0.1, player.posZ }
            : lookedAt(player);
        if (at == null) {
            player.addChatMessage(new ChatComponentText("No block within " + (int) REACH + " blocks"));
            return;
        }
        EntityLingeringArea area = Areas
            .placeOnGround(spec, player.worldObj, flags.contains("enemy") ? null : player, at[0], at[1], at[2]);
        if (area == null) player.addChatMessage(new ChatComponentText("No ground under that spot"));
    }

    /** Just above the face of the block the player looks at. */
    private static double[] lookedAt(EntityPlayerMP player) {
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        Vec3 end = eye.addVector(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH);
        MovingObjectPosition hit = player.worldObj.func_147447_a(eye, end, false, true, false);
        if (hit == null) return null;
        return new double[] { hit.hitVec.xCoord - look.xCoord * 0.05, hit.hitVec.yCoord - look.yCoord * 0.05,
            hit.hitVec.zCoord - look.zCoord * 0.05 };
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        String last = args[args.length - 1];
        List<String> options = args.length == 1 ? AreaTypes.names() : Arrays.asList("enemy", "here");
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.startsWith(last)) out.add(o);
        return out;
    }
}
