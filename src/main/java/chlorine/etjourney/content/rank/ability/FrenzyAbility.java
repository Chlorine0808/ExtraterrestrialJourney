package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.util.DamageSource;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** After a melee hit lands, the mob's melee AI waits half as long before the next swing. */
public final class FrenzyAbility implements RankAbility {

    public static final String ID = "frenzy";
    /** Ticks to the next swing; the melee AI waits 20, and a target's hurt cooldown lets a hit through at 10. */
    static final int WAIT = 10;
    private static final String[] ATTACK_TICK = { "field_75439_d", "attackTick" };

    @Override
    public String id() {
        return ID;
    }

    /** Only mobs that fight with the vanilla melee AI. */
    @Override
    public boolean fits(EntityLiving mob) {
        return meleeAI(mob) != null;
    }

    /** The AI sets its wait before it strikes, so this runs after and shortens it. */
    @Override
    public void onStrike(EntityLiving self, EntityLivingBase target, DamageSource source, float amount) {
        EntityAIAttackOnCollide ai = meleeAI(self);
        if (ai == null) return;
        int wait = ReflectionHelper.getPrivateValue(EntityAIAttackOnCollide.class, ai, ATTACK_TICK);
        if (wait > WAIT) ReflectionHelper.setPrivateValue(EntityAIAttackOnCollide.class, ai, WAIT, ATTACK_TICK);
    }

    private static EntityAIAttackOnCollide meleeAI(EntityLiving mob) {
        for (Object o : mob.tasks.taskEntries) {
            Object action = ((EntityAITasks.EntityAITaskEntry) o).action;
            if (action instanceof EntityAIAttackOnCollide) return (EntityAIAttackOnCollide) action;
        }
        return null;
    }
}
