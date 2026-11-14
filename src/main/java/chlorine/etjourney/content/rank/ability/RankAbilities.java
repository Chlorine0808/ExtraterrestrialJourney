package chlorine.etjourney.content.rank.ability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.AbilityPicker;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankData;

/** Every ability a rank can hand out, by the name saved with the mob. */
public final class RankAbilities {

    private static final Map<String, RankAbility> ALL = new LinkedHashMap<>();

    static {
        add(new SplitAbility());
        add(new WarpAbility());
        add(new SummonAbility());
        add(new TrailAbility());
        add(new BulkyAbility());
        add(new BruteAbility());
        add(new JuggernautAbility());
        add(new HeavyAbility());
        add(new BuffsAbility());
        add(new RegenAbility());
        add(new EnrageAbility());
        add(new OneupAbility());
        add(new ShieldAbility());
        add(new FrenzyAbility());
        add(new ThornsAbility());
        add(new GuardAbility());
        add(new RangedWardAbility());
        add(new MagicWardAbility());
        add(new EtherealAbility());
        add(new AbsorbAbility());
        add(new BarrierAbility());
        add(new VeilAbility());
        add(new GiantAbility());
        add(new TinyAbility());
        add(new BlightAbility());
        add(new FamineAbility());
        add(new MenderAbility());
        add(new RouseAbility());
        add(new GatherAbility());
        add(new PullAbility());
        add(new FlightAbility());
    }

    private RankAbilities() {}

    static void add(RankAbility ability) {
        ALL.put(ability.id(), ability);
    }

    /** Draws the rank's number of distinct abilities that fit the mob and the rank and do not clash. */
    public static List<String> pick(EntityLiving mob, Rank rank, Random random) {
        return AbilityPicker.pick(new ArrayList<>(ALL.keySet()), id -> {
            RankAbility a = ALL.get(id);
            return a.least()
                .ordinal() <= rank.ordinal() && a.fits(mob);
        },
            (a, b) -> ALL.get(a)
                .clashes(b),
            rank.abilities,
            random);
    }

    /** The ability of that name, or null. */
    public static RankAbility get(String id) {
        return ALL.get(id);
    }

    public static List<String> ids() {
        return new ArrayList<>(ALL.keySet());
    }

    /** The names saved with the mob, known or not. */
    public static List<String> idsOf(EntityLiving mob) {
        return RankData.abilities(mob);
    }

    /** The mob's abilities; names no longer known are skipped. */
    public static List<RankAbility> of(EntityLiving mob) {
        List<RankAbility> out = new ArrayList<>();
        for (String id : RankData.abilities(mob)) {
            RankAbility a = ALL.get(id);
            if (a != null) out.add(a);
        }
        return out;
    }
}
