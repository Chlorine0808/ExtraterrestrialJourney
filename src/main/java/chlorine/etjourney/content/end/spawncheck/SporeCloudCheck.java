package chlorine.etjourney.content.end.spawncheck;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import chlorine.etjourney.content.area.AreaSpec;
import chlorine.etjourney.content.area.AreaTypes;
import chlorine.etjourney.content.area.EntityLingeringArea;
import chlorine.etjourney.content.end.EntitySporeZombie;

/** Kills a spore zombie that spawned in MYCELIA beside the player and watches the cloud it leaves. */
final class SporeCloudCheck {

    private static final AreaSpec CLOUD = AreaTypes.SPORE_CLOUD;
    /** No mob spawns naturally this close to a player, so a spore zombie joining here came from the cloud. */
    private static final double NO_SPAWN = 24;
    private static final double EPS = 0.01;

    private final Consumer<String> record;
    private int tick, born;
    private EntitySporeZombie victim;
    private double fellX, fellY, fellZ;
    private EntityLingeringArea cloud;

    SporeCloudCheck(Consumer<String> record) {
        this.record = record;
    }

    /** Counts spore zombies that join near the player while the cloud lasts. */
    void onJoin(Entity e, EntityPlayerMP player) {
        if (victim != null && e instanceof EntitySporeZombie && e.getDistanceToEntity(player) < NO_SPAWN) born++;
    }

    /** Runs this tick's step; true once done. */
    boolean step(WorldServer end, EntityPlayerMP player) {
        tick++;
        if (tick == 1) return !kill(end, player);
        if (cloud == null) cloud = find(end);
        if (tick < CLOUD.size.duration + 5) return false;
        verdict(
            cloud != null && cloud.owner() == victim
                && Math.hypot(cloud.posX - fellX, cloud.posZ - fellZ) < EPS
                && Math.abs(CLOUD.shape.bottom(cloud.posY, CLOUD.height) - fellY) < EPS,
            "a spore zombie killed in MYCELIA leaves a spore cloud resting where it fell" + (cloud == null ? ""
                : String.format(Locale.ROOT, ": bottom y %.2f, fell at %.2f", cloud.posY - CLOUD.height / 2, fellY)));
        verdict(cloud != null && cloud.isDead, "the cloud is gone after its " + CLOUD.size.duration + " ticks");
        verdict(born == 0, "no spore zombie joins near the cloud while it lasts: " + born);
        return true;
    }

    /** Brings the nearest standing spore zombie beside the player and kills it; false if there is none. */
    private boolean kill(WorldServer end, EntityPlayerMP player) {
        for (Object o : end.loadedEntityList) {
            if (!(o instanceof EntitySporeZombie) || !((Entity) o).isEntityAlive()) continue;
            EntitySporeZombie z = (EntitySporeZombie) o;
            if (victim == null || z.getDistanceSqToEntity(player) < victim.getDistanceSqToEntity(player)) victim = z;
        }
        if (victim == null) {
            verdict(false, "a spore zombie stands in MYCELIA to kill for the cloud");
            return false;
        }
        int x = MathHelper.floor_double(player.posX) + 3, z = MathHelper.floor_double(player.posZ);
        victim.setPositionAndUpdate(x + 0.5, end.getTopSolidOrLiquidBlock(x, z), z + 0.5);
        fellX = victim.posX;
        fellY = victim.posY;
        fellZ = victim.posZ;
        victim.attackEntityFrom(DamageSource.causePlayerDamage(player), 1000);
        return true;
    }

    private EntityLingeringArea find(WorldServer end) {
        for (Object o : end.loadedEntityList)
            if (o instanceof EntityLingeringArea && ((EntityLingeringArea) o).spec() == CLOUD
                && ((EntityLingeringArea) o).owner() == victim) return (EntityLingeringArea) o;
        return null;
    }

    private void verdict(boolean pass, String what) {
        record.accept((pass ? "PASS " : "FAIL ") + what);
    }
}
