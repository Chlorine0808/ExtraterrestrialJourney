package chlorine.etjourney.content.end.spawncheck;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

/** Runs each ability trial in turn, healing the player and clearing the trial's mobs between them. */
final class AbilityTrials {

    /** Trials that cap their own length still stop here, so one stuck trial cannot hold the check. */
    private static final int MAX_TICKS = 600;

    private final List<AbilityTrial> trials = new ArrayList<>();
    private final Consumer<String> record;
    private int index, tick;
    private AbilityTrial.Ctx ctx;

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
    }

    /** True once every trial is done. */
    boolean step(WorldServer end, EntityPlayerMP player) {
        if (index >= trials.size()) return true;
        AbilityTrial trial = trials.get(index);
        if (ctx == null) {
            ctx = new AbilityTrial.Ctx(end, player, record);
            player.setHealth(player.getMaxHealth());
            player.getFoodStats()
                .addStats(20, 20);
            player.clearActivePotions();
            tick = 0;
        }
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
            for (Entity e : ctx.made) e.setDead();
            ctx = null;
            index++;
        }
        return false;
    }
}
