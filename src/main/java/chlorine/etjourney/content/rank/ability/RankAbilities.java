package chlorine.etjourney.content.rank.ability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.AbilityPicker;
import chlorine.etjourney.content.rank.RankData;

/** Every ability a rank can hand out, by the name saved with the mob. */
public final class RankAbilities {

    private static final Map<String, RankAbility> ALL = new LinkedHashMap<>();

    static {
        add(new SplitAbility());
        add(new WarpAbility());
        add(new SummonAbility());
    }

    private RankAbilities() {}

    static void add(RankAbility ability) {
        ALL.put(ability.id(), ability);
    }

    /** Draws count distinct abilities that fit the mob. */
    public static List<String> pick(EntityLiving mob, int count, Random random) {
        return AbilityPicker.pick(
            new ArrayList<>(ALL.keySet()),
            id -> ALL.get(id)
                .fits(mob),
            count,
            random);
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
