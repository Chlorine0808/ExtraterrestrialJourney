package chlorine.etjourney.world.end.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;

import chlorine.etjourney.core.status.Potions;
import chlorine.etjourney.core.status.StatusEffects;

/** /etj status: gives the creature in view, or the player, a status effect through ETJ's way in; or milk. */
final class StatusCommand {

    static final String USAGE = "/etj status <effect> <seconds> [level] [stubborn] [self] | /etj status milk [self]";

    private StatusCommand() {}

    static void run(EntityPlayerMP player, String[] args) {
        List<String> flags = Arrays.asList(args)
            .subList(Math.min(args.length, 2), args.length);
        if (args.length >= 2 && args[1].equals("milk")) {
            EntityLivingBase target = target(player, flags);
            target.curePotionEffects(new ItemStack(Items.milk_bucket));
            say(player, target.getCommandSenderName() + ": milk, left " + describe(target));
            return;
        }
        if (args.length < 3) {
            say(player, "Usage: " + USAGE);
            return;
        }
        Potion potion = Potions.byName(args[1]);
        if (potion == null) {
            say(player, "Unknown effect: " + args[1]);
            return;
        }
        int seconds, level = 1;
        try {
            seconds = Integer.parseInt(args[2]);
            if (args.length > 3 && args[3].matches("\\d+")) level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            say(player, "Not a number: " + args[2]);
            return;
        }
        if (seconds <= 0 || level <= 0) {
            say(player, "Seconds and level must be > 0");
            return;
        }
        EntityLivingBase target = target(player, flags);
        boolean landed = StatusEffects
            .apply(target, new PotionEffect(potion.id, seconds * 20, level - 1), player, flags.contains("stubborn"));
        say(player, target.getCommandSenderName() + ": " + (landed ? describe(target) : "nothing landed"));
    }

    static List<String> complete(String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>(Potions.shortNames());
            names.add("milk");
            return names;
        }
        if (args.length >= 4 || args.length == 3 && args[1].equals("milk")) return Arrays.asList("stubborn", "self");
        return Collections.emptyList();
    }

    private static EntityLivingBase target(EntityPlayerMP player, List<String> flags) {
        EntityLivingBase target = flags.contains("self") ? null : DamageCommand.inView(player);
        return target == null ? player : target;
    }

    /** Each active effect as name level seconds, with a star when milk cannot wash it off. */
    private static String describe(EntityLivingBase target) {
        StringBuilder out = new StringBuilder();
        for (Object o : target.getActivePotionEffects()) {
            PotionEffect e = (PotionEffect) o;
            Potion potion = Potion.potionTypes[e.getPotionID()];
            if (out.length() > 0) out.append(", ");
            out.append(potion.getName())
                .append(' ')
                .append(e.getAmplifier() + 1)
                .append(' ')
                .append(e.getDuration() / 20)
                .append('s');
            if (StatusEffects.isStubborn(target, potion)) out.append('*');
        }
        return out.length() == 0 ? "no effects" : out.toString();
    }

    private static void say(EntityPlayerMP player, String text) {
        player.addChatMessage(new ChatComponentText(text));
    }
}
