package chlorine.etjourney.content.end.spawncheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

/** Runs each ability trial in turn, healing the player and clearing the trial's mobs between them. */
final class AbilityTrials {

    /** Trials that cap their own length still stop here, so one stuck trial cannot hold the check. */
    private static final int MAX_TICKS = 600;

    private final List<AbilityTrial> trials = new ArrayList<>();
    private final Consumer<String> record;
    private int index, tick;
    private AbilityTrial.Ctx ctx;
    /** Where the player hovers when the first trial starts; each trial starts from there. */
    private double[] home;

    AbilityTrials(Consumer<String> record) {
        this.record = record;
        trials.add(new BulkyTrial());
        trials.add(new BruteTrial());
        trials.add(new JuggernautTrial());
        trials.add(new HeavyTrial());
        trials.add(new BuffsTrial());
        trials.add(new RegenTrial());
        trials.add(new EnrageTrial());
        trials.add(new OneupTrial());
        trials.add(new ShieldTrial());
        trials.add(new FrenzyTrial());
        trials.add(new ThornsTrial());
        trials.add(new GuardTrial());
        trials.add(new RangedWardTrial());
        trials.add(new MagicWardTrial());
        trials.add(new EtherealTrial());
        trials.add(new AbsorbTrial());
        trials.add(new BarrierTrial());
        trials.add(new VeilTrial());
        trials.add(new GiantTrial());
        trials.add(new TinyTrial());
        trials.add(new BlightTrial());
        trials.add(new FamineTrial());
        trials.add(new MenderTrial());
        trials.add(new RouseTrial());
        trials.add(new GatherTrial());
        trials.add(new PullTrial());
        trials.add(new FlightTrial());
        trials.add(new HordeTrial());
        trials.add(new FireballTrial());
        trials.add(new RockfallTrial());
        trials.add(new WebsTrial());
        trials.add(new LightningTrial());
        trials.add(new DowndraftTrial());
        trials.add(new BlastTrial());
        trials.add(new ReviveTrial());
    }

    private static void fly(EntityPlayerMP player) {
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    /** True once every trial is done. */
    boolean step(WorldServer end, EntityPlayerMP player) {
        if (index >= trials.size()) {
            player.setGameType(WorldSettings.GameType.CREATIVE);
            fly(player);
            return true;
        }
        AbilityTrial trial = trials.get(index);
        if (ctx == null) {
            ctx = new AbilityTrial.Ctx(end, player, record);
            // The check world is creative, and mobs neither target nor hurt a creative player.
            player.setGameType(WorldSettings.GameType.SURVIVAL);
            fly(player);
            if (home == null) home = new double[] { player.posX, player.posY, player.posZ };
            else player.setPositionAndUpdate(home[0], home[1], home[2]);
            player.setHealth(player.getMaxHealth());
            player.getFoodStats()
                .addStats(20, 20);
            player.clearActivePotions();
            // A hit at the end of the last trial must not swallow damage in this one.
            player.hurtResistantTime = 0;
            tick = 0;
        }
        // A dead player is no target, and every later trial would fail with it.
        if (player.getHealth() < 6) player.setHealth(player.getMaxHealth());
        boolean done;
        try {
            done = trial.step(ctx, ++tick);
        } catch (RuntimeException e) {
            ctx.verdict(false, trial.name + ": crashed: " + e);
            done = true;
        }
        if (!done && tick >= MAX_TICKS) {
            ctx.verdict(false, trial.name + ": did not finish");
            done = true;
        }
        if (done) {
            if (!ctx.made.isEmpty()) {
                Entity first = ctx.made.get(0);
                record.accept(
                    String.format(
                        Locale.ROOT,
                        "INFO %s: player %.1f %.1f %.1f (%s), mob %.1f %.1f %.1f",
                        trial.name,
                        player.posX,
                        player.posY,
                        player.posZ,
                        player.theItemInWorldManager.getGameType(),
                        first.posX,
                        first.posY,
                        first.posZ));
            }
            for (Entity e : ctx.made) e.setDead();
            // Drops of one trial must not count in the next.
            for (Object o : end.getEntitiesWithinAABB(EntityItem.class, player.boundingBox.expand(16, 16, 16)))
                ((Entity) o).setDead();
            ctx = null;
            index++;
        }
        return false;
    }
}
