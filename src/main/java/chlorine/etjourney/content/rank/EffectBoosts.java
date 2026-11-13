package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

/** Notes each target a ranked mob hits and, at the end of the tick, boosts the effects the hit gave. */
final class EffectBoosts {

    private static final class Pending {

        final Rank rank;
        /** Duration and level of each effect before the hit, by potion ID. */
        final Map<Integer, int[]> before;

        Pending(Rank rank, Map<Integer, int[]> before) {
            this.rank = rank;
            this.before = before;
        }
    }

    private final Map<EntityLivingBase, Pending> pending = new HashMap<>();

    void hit(EntityLivingBase target, Rank rank) {
        Pending p = pending.get(target);
        if (p != null && p.rank.ordinal() >= rank.ordinal()) return;
        pending.put(target, new Pending(rank, p != null ? p.before : snapshot(target)));
    }

    void endOfTick() {
        if (pending.isEmpty()) return;
        for (Map.Entry<EntityLivingBase, Pending> entry : pending.entrySet()) {
            EntityLivingBase target = entry.getKey();
            if (target.isEntityAlive()) boost(target, entry.getValue());
        }
        pending.clear();
    }

    private static void boost(EntityLivingBase target, Pending p) {
        for (Object o : new ArrayList<>(target.getActivePotionEffects())) {
            PotionEffect e = (PotionEffect) o;
            int[] was = p.before.get(e.getPotionID());
            int[] now = EffectBoost.boost(
                was == null ? EffectBoost.ABSENT : was[0],
                was == null ? 0 : was[1],
                e.getDuration(),
                e.getAmplifier(),
                p.rank);
            if (now != null)
                target.addPotionEffect(new PotionEffect(e.getPotionID(), now[0], now[1], e.getIsAmbient()));
        }
    }

    private static Map<Integer, int[]> snapshot(EntityLivingBase target) {
        Map<Integer, int[]> out = new HashMap<>();
        for (Object o : target.getActivePotionEffects()) {
            PotionEffect e = (PotionEffect) o;
            out.put(e.getPotionID(), new int[] { e.getDuration(), e.getAmplifier() });
        }
        return out;
    }
}
