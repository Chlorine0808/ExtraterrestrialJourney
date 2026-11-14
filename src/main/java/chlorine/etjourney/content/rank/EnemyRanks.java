package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import chlorine.etjourney.content.rank.ability.RankAbilities;
import chlorine.etjourney.content.rank.ability.RankAbility;
import chlorine.etjourney.content.rank.ability.RankShots;
import chlorine.etjourney.content.rank.ability.RankWebBlock;
import chlorine.etjourney.content.rank.ability.SummonAbility;
import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.network.ETJNetwork;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/** Rolls a rank for each eligible enemy the first time it joins a world, and runs what the rank brings. */
public final class EnemyRanks {

    private final EffectBoosts boosts = new EffectBoosts();

    private EnemyRanks() {}

    public static void register() {
        // Registered whatever the config says, so packet IDs match a server configured otherwise.
        ETJNetwork.channel()
            .registerMessage(RankSync.Handler.class, RankSync.class, ETJNetwork.nextId(), Side.CLIENT);
        ETJNetwork.channel()
            .registerMessage(RankBolt.Handler.class, RankBolt.class, ETJNetwork.nextId(), Side.CLIENT);
        RankShots.register();
        RankWebBlock.register();
        if (!ETJConfig.enemyRanks) return;
        EnemyRanks ranks = new EnemyRanks();
        MinecraftForge.EVENT_BUS.register(ranks);
        MinecraftForge.EVENT_BUS.register(new FamineEvents());
        FMLCommonHandler.instance()
            .bus()
            .register(ranks);
    }

    public static boolean eligible(EntityLiving mob) {
        int dim = mob.worldObj.provider.dimensionId;
        if (Arrays.stream(ETJConfig.rankDimensions)
            .noneMatch(d -> d == dim)) return false;
        return RankEligibility.allows(
            EntityList.getEntityString(mob),
            mob instanceof IMob,
            mob instanceof IBossDisplayData,
            Arrays.asList(ETJConfig.rankedMobs),
            Arrays.asList(ETJConfig.unrankedMobs));
    }

    /** Rolls by the stage at the mob's place; a mob that has rolled before keeps what it has. */
    public static void roll(EntityLiving mob) {
        RankStage stage = RankStage.at(mob.worldObj, mob.posX, mob.posZ);
        give(
            mob,
            RankOdds.pick(
                stage,
                mob.getRNG()
                    .nextDouble()));
    }

    public static void give(EntityLiving mob, Rank rank) {
        give(mob, rank, RankAbilities.pick(mob, rank, mob.getRNG()));
    }

    /**
     * Sets a rank and abilities outright; the spawn check uses it to try each ability. An ability may be written
     * name:setting, as absorb:fire.
     */
    public static void give(EntityLiving mob, Rank rank, List<String> abilities) {
        List<String> ids = new ArrayList<>();
        for (String a : abilities) {
            int colon = a.indexOf(':');
            String id = colon < 0 ? a : a.substring(0, colon);
            ids.add(id);
            if (colon >= 0) RankData.setSetting(mob, id, a.substring(colon + 1));
        }
        RankData.set(mob, rank, ids);
        RankStats.apply(mob, rank);
        for (RankAbility a : RankAbilities.of(mob)) a.onGive(mob);
        mob.setHealth(mob.getMaxHealth());
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.world.isRemote || !(event.entity instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving) event.entity;
        if (!RankData.rolled(mob) && eligible(mob)) roll(mob);
        if (RankData.ranked(mob)) for (RankAbility a : RankAbilities.of(mob)) a.onJoin(mob);
    }

    /** Low, so piercing damage, which the damage core lets through at lowest, gets past a ward too. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onAttack(LivingAttackEvent event) {
        if (event.entityLiving.worldObj.isRemote) return;
        if (event.entityLiving instanceof EntityLiving && RankData.ranked(event.entityLiving)) {
            EntityLiving self = (EntityLiving) event.entityLiving;
            for (RankAbility a : RankAbilities.of(self)) if (a.wards(self, event.source, event.ammount)) {
                event.setCanceled(true);
                return;
            }
        }
        EntityLiving attacker = meleeRanked(event.source);
        if (attacker == null) return;
        for (RankAbility a : RankAbilities.of(attacker)) if (!a.swings(attacker, event.entityLiving, event.source)) {
            event.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTake(LivingHurtEvent event) {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityLiving)
            || !RankData.ranked(event.entityLiving)) return;
        EntityLiving self = (EntityLiving) event.entityLiving;
        for (RankAbility a : RankAbilities.of(self)) event.ammount = a.takes(self, event.source, event.ammount);
    }

    /** A ranked mob that struck with its own body, not a shot. */
    private static EntityLiving meleeRanked(DamageSource source) {
        Entity e = source.getEntity();
        return e instanceof EntityLiving && e == source.getSourceOfDamage() && RankData.ranked(e) ? (EntityLiving) e
            : null;
    }

    /** Lowest, so the hit has survived every handler that might cancel it. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving.worldObj.isRemote) return;
        Entity attacker = event.source.getEntity();
        if (attacker != null && RankData.ranked(attacker)) boosts.hit(event.entityLiving, RankData.rank(attacker));
        if (event.entityLiving instanceof EntityLiving && RankData.ranked(event.entityLiving)) {
            EntityLiving self = (EntityLiving) event.entityLiving;
            for (RankAbility a : RankAbilities.of(self)) a.onHurt(self, event.source);
        }
        EntityLiving striker = meleeRanked(event.source);
        if (striker != null) for (RankAbility a : RankAbilities.of(striker))
            a.onStrike(striker, event.entityLiving, event.source, event.ammount);
    }

    @SubscribeEvent
    public void onUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityLiving)) return;
        EntityLiving self = (EntityLiving) event.entityLiving;
        if (!self.isEntityAlive() || !RankData.rolled(self)) return;
        boolean second = self.ticksExisted % 20 == 0;
        if (RankData.role(self) == RankData.MINION) {
            if (second) SummonAbility.watch(self);
            return;
        }
        if (!RankData.ranked(self)) return;
        for (RankAbility a : RankAbilities.of(self)) {
            a.onTick(self);
            if (second) a.onSecond(self);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boosts.endOfTick();
        RankTasks.tick();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving) event.entityLiving;
        if (RankData.ranked(mob)) for (RankAbility a : RankAbilities.of(mob)) if (a.cheatsDeath(mob, event.source)) {
            event.setCanceled(true);
            return;
        }
        // Minions come back on every cast, so they must not pay out.
        if (RankData.role(mob) == RankData.MINION) RankRewards.setExperience(mob, 0);
        if (!RankData.ranked(mob)) return;
        if (RankData.markPaid(mob)) RankRewards.setExperience(mob, RankData.rank(mob).experience);
        for (RankAbility a : RankAbilities.of(mob)) a.onDeath(mob, event.source);
        if (RankData.noLoot(mob)) RankRewards.setExperience(mob, 0);
    }

    /**
     * First and cancelled, so no later handler can add to a minion's or a rising corpse's drops or spawn its own,
     * whether the minion's parent is alive or not.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMinionDrops(LivingDropsEvent event) {
        if (event.entityLiving.worldObj.isRemote
            || RankData.role(event.entityLiving) != RankData.MINION && !RankData.noLoot(event.entityLiving)) return;
        event.drops.clear();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrops(LivingDropsEvent event) {
        if (!event.entityLiving.worldObj.isRemote && RankData.ranked(event.entityLiving))
            RankRewards.multiplyDrops(event.drops, RankData.rank(event.entityLiving));
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.entityPlayer instanceof EntityPlayerMP && RankSync.worthSending(event.target)) ETJNetwork.channel()
            .sendTo(new RankSync(event.target), (EntityPlayerMP) event.entityPlayer);
    }
}
