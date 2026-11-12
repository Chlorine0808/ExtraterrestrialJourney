package chlorine.etjourney.content.end.spawncheck;

import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import chlorine.etjourney.content.end.EntitySporeZombie;
import chlorine.etjourney.content.rank.EnemyRanks;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.content.rank.ability.SplitAbility;
import chlorine.etjourney.content.rank.ability.SummonAbility;
import chlorine.etjourney.content.rank.ability.WarpAbility;

/** Tries each rank ability on a rare spore zombie next to the player, one step per server tick. */
final class RankAbilityCheck {

    /** The spore zombie's own health, before its rank. */
    private static final double BASE_HEALTH = 44;

    private final Consumer<String> record;
    private int tick;
    private EntityLiving warp, summon;
    private UUID split;

    RankAbilityCheck(Consumer<String> record) {
        this.record = record;
    }

    /** Runs this tick's step; true once every step is done. */
    boolean step(WorldServer end, EntityPlayerMP player) {
        tick++;
        switch (tick) {
            case 1: {
                EntityLiving mob = spawn(end, player, 4, 0, SplitAbility.ID);
                double want = BASE_HEALTH * (1 + Rank.RARE.health);
                verdict(
                    Math.abs(mob.getMaxHealth() - want) < 0.01,
                    String.format(Locale.ROOT, "rare health %.1f, want %.1f", mob.getMaxHealth(), want));
                split = mob.getUniqueID();
                mob.attackEntityFrom(DamageSource.causePlayerDamage(player), 1000);
                break;
            }
            case 10: {
                int children = 0, ranked = 0;
                for (Object o : end.loadedEntityList) {
                    if (!(o instanceof EntityLiving)) continue;
                    EntityLiving e = (EntityLiving) o;
                    if (!e.isEntityAlive() || RankData.role(e) != RankData.SPLIT_CHILD
                        || !split.equals(RankData.parent(e))) continue;
                    children++;
                    if (RankData.ranked(e)) ranked++;
                }
                verdict(children == 2 && ranked == 0, "split: " + children + " children, " + ranked + " ranked");
                break;
            }
            case 20:
                warp = spawn(end, player, 0, 4, WarpAbility.ID);
                break;
            case 30:
                verdict(hitMoves(warp, player) >= 2, "warp: a hit moves the mob");
                break;
            case 60:
                verdict(hitMoves(warp, player) < 0.5, "warp: a hit within the wait does not");
                break;
            case 70:
                summon = spawn(end, player, -4, 0, SummonAbility.ID);
                summon.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
                break;
            case 110: {
                int minions = SummonAbility.minionsOf(summon)
                    .size();
                verdict(minions > 0, "summon: " + minions + " minions after a hit");
                summon.attackEntityFrom(DamageSource.causePlayerDamage(player), 1000);
                break;
            }
            case 115: {
                int left = SummonAbility.minionsOf(summon)
                    .size();
                verdict(left == 0, "summon: " + left + " minions left after the parent died");
                break;
            }
            case 120:
                return true;
            default:
                break;
        }
        return false;
    }

    private void verdict(boolean pass, String what) {
        record.accept((pass ? "PASS " : "FAIL ") + what);
    }

    /** How far one hit from the player moved the mob. */
    private static double hitMoves(EntityLiving mob, EntityPlayerMP player) {
        double x = mob.posX, y = mob.posY, z = mob.posZ;
        mob.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
        return Math.sqrt(mob.getDistanceSq(x, y, z));
    }

    private static EntityLiving spawn(WorldServer end, EntityPlayerMP player, int dx, int dz, String ability) {
        EntitySporeZombie mob = new EntitySporeZombie(end);
        int x = MathHelper.floor_double(player.posX) + dx, z = MathHelper.floor_double(player.posZ) + dz;
        mob.setLocationAndAngles(x + 0.5, end.getTopSolidOrLiquidBlock(x, z), z + 0.5, 0, 0);
        EnemyRanks.give(mob, Rank.RARE, Collections.singletonList(ability));
        end.spawnEntityInWorld(mob);
        return mob;
    }
}
