package chlorine.etjourney.content.rank.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.MinionWatch;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.content.rank.RankStats;

/** In a fight the mob calls minions, which vanish when it dies. */
public class SummonAbility implements RankAbility {

    public static final String ID = "summon";
    private final String id;
    private final int cooldown, perCast, cap;
    /** Part of its health a minion loses. */
    private final double health;
    private static final Map<Class<?>, Function<World, EntityLiving>> MINIONS = new HashMap<>();
    private static final Map<Class<?>, Function<World, EntityLiving>> HORDES = new HashMap<>();

    /** Has mobs of the parent type call what make returns instead of their own kind. */
    public static void minionOf(Class<? extends EntityLiving> parentType, Function<World, EntityLiving> make) {
        MINIONS.put(parentType, make);
    }

    /** Has horde mobs of the parent type call what make returns instead of silverfish. */
    public static void hordeOf(Class<? extends EntityLiving> parentType, Function<World, EntityLiving> make) {
        HORDES.put(parentType, make);
    }

    static EntityLiving hordeMinion(EntityLiving parent) {
        Function<World, EntityLiving> make = HORDES.get(parent.getClass());
        return make != null ? make.apply(parent.worldObj) : new EntitySilverfish(parent.worldObj);
    }

    public SummonAbility() {
        this(ID, 200, 2, 4, 0.5);
    }

    /** A summon with its own numbers; a subclass picks the minion kind by overriding make. */
    protected SummonAbility(String id, int cooldown, int perCast, int cap, double health) {
        this.id = id;
        this.cooldown = cooldown;
        this.perCast = perCast;
        this.cap = cap;
        this.health = health;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityLivingBase target = self.getAttackTarget() != null ? self.getAttackTarget() : self.getAITarget();
        if (target == null) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, id), cooldown)) return;
        int n = MinionWatch.toSummon(minionsOf(self).size(), cap, perCast);
        if (n == 0) return;
        for (int i = 0; i < n; i++) {
            EntityLiving minion = make(self);
            if (minion == null) return;
            Offspring.bind(minion, self, RankData.MINION);
            place(minion, self);
            RankStats.weaken(minion, health, 0);
            minion.setAttackTarget(target);
            self.worldObj.spawnEntityInWorld(minion);
            Offspring.puff(minion, "witchMagic", 16);
        }
        RankData.markUsed(self, id, now);
    }

    @Override
    public void onDeath(EntityLiving self, DamageSource source) {
        for (EntityLiving m : minionsOf(self)) vanish(m);
    }

    /** Once a second for each minion: follows its parent out of the world. */
    public static void watch(EntityLiving minion) {
        UUID parent = RankData.parent(minion);
        EntityLiving found = parent == null ? null : find(minion.worldObj, parent);
        MinionWatch.Parent state = found == null ? MinionWatch.Parent.MISSING
            : found.isEntityAlive() ? MinionWatch.Parent.ALIVE : MinionWatch.Parent.DEAD;
        int misses = MinionWatch.step(RankData.misses(minion), state);
        if (misses == MinionWatch.VANISH) vanish(minion);
        else RankData.setMisses(minion, misses);
    }

    /** Loaded, living minions of the parent. */
    public static List<EntityLiving> minionsOf(EntityLiving parent) {
        List<EntityLiving> out = new ArrayList<>();
        UUID id = parent.getUniqueID();
        for (Object o : parent.worldObj.loadedEntityList) {
            if (!(o instanceof EntityLiving)) continue;
            EntityLiving e = (EntityLiving) o;
            if (e.isEntityAlive() && RankData.role(e) == RankData.MINION && id.equals(RankData.parent(e))) out.add(e);
        }
        return out;
    }

    private static EntityLiving find(World world, UUID id) {
        for (Object o : world.loadedEntityList) {
            if (o instanceof EntityLiving && id.equals(((EntityLiving) o).getUniqueID())) return (EntityLiving) o;
        }
        return null;
    }

    /** A new minion, not yet in the world; null when none can be made. */
    protected EntityLiving make(EntityLiving parent) {
        Function<World, EntityLiving> make = MINIONS.get(parent.getClass());
        return make != null ? make.apply(parent.worldObj) : Offspring.sameKind(parent);
    }

    /** Within two blocks of the parent where the minion fits, else on the parent. */
    private static void place(EntityLiving minion, EntityLiving parent) {
        for (int i = 0; i < 8; i++) {
            double x = parent.posX + (parent.getRNG()
                .nextDouble() * 4 - 2), z = parent.posZ
                    + (parent.getRNG()
                        .nextDouble() * 4 - 2);
            minion.setLocationAndAngles(x, parent.posY, z, parent.rotationYaw, 0);
            if (parent.worldObj.getCollidingBoundingBoxes(minion, minion.boundingBox)
                .isEmpty()) return;
        }
        minion.setLocationAndAngles(parent.posX, parent.posY, parent.posZ, parent.rotationYaw, 0);
    }

    private static void vanish(EntityLiving minion) {
        Offspring.puff(minion, "largesmoke", 12);
        minion.setDead();
    }
}
