package chlorine.etjourney.content.end.spawncheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import chlorine.etjourney.content.end.EntitySporeZombie;
import chlorine.etjourney.content.rank.EnemyRanks;
import chlorine.etjourney.content.rank.Rank;

/** One ability's in-game check: steps once a server tick until it says it is done. */
abstract class AbilityTrial {

    /** What every step can reach: the End, the player and the results file. */
    static final class Ctx {

        final WorldServer end;
        final EntityPlayerMP player;
        private final Consumer<String> record;
        /** Mobs the trial made; cleared when it ends. */
        final List<Entity> made = new ArrayList<>();

        Ctx(WorldServer end, EntityPlayerMP player, Consumer<String> record) {
            this.end = end;
            this.player = player;
            this.record = record;
        }

        void verdict(boolean pass, String what) {
            record.accept((pass ? "PASS " : "FAIL ") + what);
        }

        /** A spore zombie of the rank with exactly these abilities (name or name:setting), dx dz from the player. */
        EntityLiving spawn(Rank rank, int dx, int dz, String... abilities) {
            EntitySporeZombie mob = new EntitySporeZombie(end);
            place(mob, dx, dz);
            EnemyRanks.give(mob, rank, Arrays.asList(abilities));
            end.spawnEntityInWorld(mob);
            made.add(mob);
            return mob;
        }

        EntityLiving spawn(int dx, int dz, String... abilities) {
            return spawn(Rank.RARE, dx, dz, abilities);
        }

        /** An unranked spore zombie that never rolls one. */
        EntityLiving plain(int dx, int dz) {
            return spawn(Rank.NORMAL, dx, dz);
        }

        /** On the ground dx dz from the player. */
        void place(EntityLiving mob, int dx, int dz) {
            int x = MathHelper.floor_double(player.posX) + dx, z = MathHelper.floor_double(player.posZ) + dz;
            mob.setLocationAndAngles(x + 0.5, end.getTopSolidOrLiquidBlock(x, z), z + 0.5, 0, 0);
        }

        DamageSource byPlayer() {
            return DamageSource.causePlayerDamage(player);
        }
    }

    final String name;

    AbilityTrial(String name) {
        this.name = name;
    }

    /** Runs the step for this tick, counted from 1; true once done. */
    abstract boolean step(Ctx c, int tick);
}
