package chlorine.etjourney.world.end.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;

import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;

/** /etj damage: hits the creature in view, or the player, with one elemental attack. */
final class DamageCommand {

    static final String USAGE = "/etj damage <element> <amount> [physical|magic] [pierce] [self]";
    private static final double REACH = 16;

    private DamageCommand() {}

    static void run(EntityPlayerMP player, String[] args) {
        if (args.length < 3) {
            say(player, "Usage: " + USAGE);
            return;
        }
        Element element = ElementRegistry.get(args[1]);
        if (element == null) {
            say(player, "Unknown element: " + args[1]);
            return;
        }
        float amount;
        try {
            amount = Float.parseFloat(args[2]);
        } catch (NumberFormatException e) {
            say(player, "Not a number: " + args[2]);
            return;
        }
        if (!(amount >= 0)) {
            say(player, "Amount must be >= 0");
            return;
        }
        List<String> flags = Arrays.asList(args)
            .subList(3, args.length);
        ElementalDamage.Builder b = ElementalDamage
            .builder(flags.contains("magic") ? DamageKind.MAGIC : DamageKind.PHYSICAL)
            .add(element, amount);
        if (flags.contains("pierce")) b.piercing();
        EntityLivingBase target = flags.contains("self") ? null : inView(player);
        if (target == null) target = player;
        float before = target.getHealth() + target.getAbsorptionAmount();
        boolean hit = ElementalDamages.deal(target, new ElementalDamageSource(b.build(), player, player));
        float after = target.getHealth() + target.getAbsorptionAmount();
        say(player, target.getCommandSenderName() + ": " + before + " -> " + after + (hit ? "" : " (no damage)"));
    }

    static List<String> complete(String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Element e : ElementRegistry.all()) names.add(e.name());
            return names;
        }
        if (args.length >= 4) return Arrays.asList("physical", "magic", "pierce", "self");
        return Collections.emptyList();
    }

    /** The nearest living thing whose box the look ray crosses within reach. */
    private static EntityLivingBase inView(EntityPlayerMP player) {
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        Vec3 end = eye.addVector(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH);
        AxisAlignedBB area = player.boundingBox.addCoord(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH)
            .expand(1, 1, 1);
        EntityLivingBase best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Object o : player.worldObj.getEntitiesWithinAABBExcludingEntity(player, area)) {
            if (!(o instanceof EntityLivingBase)) continue;
            Entity e = (Entity) o;
            if (e.boundingBox.calculateIntercept(eye, end) == null) continue;
            double d = e.getDistanceSqToEntity(player);
            if (d < bestDistance) {
                bestDistance = d;
                best = (EntityLivingBase) e;
            }
        }
        return best;
    }

    private static void say(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText(text));
    }
}
