package chlorine.etjourney.content.rank;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import chlorine.etjourney.content.rank.ability.RankAbilities;
import chlorine.etjourney.content.rank.ability.RankAbility;
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
        if (!ETJConfig.enemyRanks) return;
        EnemyRanks ranks = new EnemyRanks();
        MinecraftForge.EVENT_BUS.register(ranks);
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
        give(mob, rank, RankAbilities.pick(mob, rank.abilities, mob.getRNG()));
    }

    /** Sets a rank and abilities outright; the spawn check uses it to try each ability. */
    public static void give(EntityLiving mob, Rank rank, List<String> abilities) {
        RankData.set(mob, rank, abilities);
        RankStats.apply(mob, rank);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.world.isRemote || !(event.entity instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving) event.entity;
        if (!RankData.rolled(mob) && eligible(mob)) roll(mob);
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
    }

    @SubscribeEvent
    public void onUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.entityLiving.ticksExisted % 20 != 0 || event.entityLiving.worldObj.isRemote
            || !(event.entityLiving instanceof EntityLiving)) return;
        EntityLiving self = (EntityLiving) event.entityLiving;
        if (!self.isEntityAlive() || !RankData.rolled(self)) return;
        if (RankData.role(self) == RankData.MINION) SummonAbility.watch(self);
        else for (RankAbility a : RankAbilities.of(self)) a.onSecond(self);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) boosts.endOfTick();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving) event.entityLiving;
        // Minions come back on every cast, so they must not pay out.
        if (RankData.role(mob) == RankData.MINION) RankRewards.setExperience(mob, 0);
        if (!RankData.ranked(mob)) return;
        if (RankData.markPaid(mob)) RankRewards.setExperience(mob, RankData.rank(mob).experience);
        for (RankAbility a : RankAbilities.of(mob)) a.onDeath(mob, event.source);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrops(LivingDropsEvent event) {
        if (event.entityLiving.worldObj.isRemote) return;
        if (RankData.role(event.entityLiving) == RankData.MINION) event.drops.clear();
        else if (RankData.ranked(event.entityLiving))
            RankRewards.multiplyDrops(event.drops, RankData.rank(event.entityLiving));
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.entityPlayer instanceof EntityPlayerMP && RankSync.worthSending(event.target)) ETJNetwork.channel()
            .sendTo(new RankSync(event.target), (EntityPlayerMP) event.entityPlayer);
    }
}
