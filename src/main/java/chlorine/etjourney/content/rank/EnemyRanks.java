package chlorine.etjourney.content.rank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.IMob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import chlorine.etjourney.core.config.ETJConfig;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Rolls a rank for each eligible enemy the first time it joins a world, and runs what the rank brings. */
public final class EnemyRanks {

    private final EffectBoosts boosts = new EffectBoosts();

    private EnemyRanks() {}

    public static void register() {
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
        List<String> abilities = Collections.emptyList();
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
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) boosts.endOfTick();
    }
}
