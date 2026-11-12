package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;

import chlorine.etjourney.core.combat.model.Condition;
import chlorine.etjourney.core.combat.model.Conditions;
import chlorine.etjourney.core.combat.model.Gate;
import chlorine.etjourney.core.combat.model.GateState;

/** A hook built from parts: which event, under what condition, how often, and what it does. */
public final class TriggeredHook implements CombatHook {

    public enum When {
        DEALT,
        HURT
    }

    /** Returns whether it took effect; only then is a charge of the gate used. */
    public interface Action {

        boolean run(EntityLivingBase owner, CombatHit hit);
    }

    private final String id;
    private final When when;
    private final Condition condition;
    private final Gate gate;
    private final Action action;

    /** A null gate fires every time. */
    public TriggeredHook(String id, When when, Condition condition, Gate gate, Action action) {
        this.id = id;
        this.when = when;
        this.condition = condition == null ? Conditions.ALWAYS : condition;
        this.gate = gate;
        this.action = action;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void onDamageDealt(EntityLivingBase owner, CombatHit hit) {
        if (when == When.DEALT) run(owner, hit);
    }

    @Override
    public void onHurt(EntityLivingBase owner, CombatHit hit) {
        if (when == When.HURT) run(owner, hit);
    }

    private void run(EntityLivingBase owner, CombatHit hit) {
        if (!condition.test(new LivingSituation(owner, hit.attack))) return;
        if (gate == null) {
            action.run(owner, hit);
            return;
        }
        long now = owner.worldObj.getTotalWorldTime();
        GateState state = GateStore.get(owner, id);
        if (!gate.ready(state, now)) return;
        if (action.run(owner, hit)) GateStore.put(owner, id, gate.fire(state, now));
    }
}
