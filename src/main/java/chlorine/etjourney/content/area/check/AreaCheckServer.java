package chlorine.etjourney.content.area.check;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import chlorine.etjourney.content.area.AreaSlow;
import chlorine.etjourney.content.area.AreaSpec;
import chlorine.etjourney.content.area.AreaTypes;
import chlorine.etjourney.content.area.Areas;
import chlorine.etjourney.content.area.EntityLingeringArea;
import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.projectile.Projectiles;
import chlorine.etjourney.content.rank.EnemyRanks;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.TrailAbility;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Server half of the area check: leaves the trial areas by command, by hand and from a shot, around pigs held on
 * the ground and a shielded player, and checks that they land, work on those inside, end, and spare allies.
 */
public final class AreaCheckServer {

    /** The take-off speed of a vanilla jump. */
    private static final double VANILLA_JUMP = 0.42;
    private static final int JOIN_TICKS = 100;
    private static final float PIG_HEALTH = 200;
    private static final double EPS = 0.01;

    static volatile boolean done;
    /** The first area the client saw with its kind and radius. */
    static volatile String clientSaw;

    /** One check: starts, then is polled each tick until it reports itself finished. */
    private abstract class Trial {

        final int timeout;

        Trial(int timeout) {
            this.timeout = timeout;
        }

        abstract void start();

        /** True when it has recorded its verdict; timedOut is set on its last poll. */
        abstract boolean poll(int t, boolean timedOut);
    }

    private final List<String> lines = new ArrayList<>();
    private final List<Trial> trials = new ArrayList<>();
    private final Map<EntityPig, double[]> held = new HashMap<>();
    /** The first elemental hit each creature took this trial. */
    private final Map<Integer, ElementalDamageSource> hits = new HashMap<>();
    private final List<Entity> spawned = new ArrayList<>();
    private EntityPlayerMP player;
    private WorldServer world;
    private double bx, by, bz;
    private int index = -1, t, waited;

    private AreaCheckServer() {}

    static void register() {
        AreaCheckServer check = new AreaCheckServer();
        FMLCommonHandler.instance()
            .bus()
            .register(check);
        MinecraftForge.EVENT_BUS.register(check);
    }

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (event.entity.worldObj.isRemote || !(event.source instanceof ElementalDamageSource)) return;
        hits.putIfAbsent(event.entity.getEntityId(), (ElementalDamageSource) event.source);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || done) return;
        List<?> players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) return;
        try {
            step((EntityPlayerMP) players.get(0));
        } catch (RuntimeException e) {
            ModLog.LOG.error("[areacheck] crashed", e);
            record("FAIL crash: " + e);
            finish();
        }
    }

    private void step(EntityPlayerMP p) {
        if (index < 0) {
            if (++waited < JOIN_TICKS) return;
            setUp(p);
            index = 0;
            begin();
            return;
        }
        hold();
        Trial trial = trials.get(index);
        t++;
        if (!trial.poll(t, t >= trial.timeout) && t < trial.timeout) return;
        clear();
        index++;
        if (index < trials.size()) begin();
        else {
            record((clientSaw != null ? "PASS" : "FAIL") + " the client got an area with its kind: " + clientSaw);
            finish();
        }
    }

    private void begin() {
        t = 0;
        hits.clear();
        trials.get(index)
            .start();
    }

    private void setUp(EntityPlayerMP p) {
        player = p;
        world = (WorldServer) p.worldObj;
        world.difficultySetting = EnumDifficulty.PEACEFUL;
        bx = Math.floor(p.posX) + 0.5;
        bz = Math.floor(p.posZ) + 0.5;
        by = world.getTopSolidOrLiquidBlock((int) Math.floor(bx), (int) Math.floor(bz));
        // A survival player the enemy areas can reach, shielded from their damage.
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.addPotionEffect(new PotionEffect(Potion.resistance.id, 100_000, 4));
        p.setPositionAndUpdate(bx, by, bz);
        record(String.format(Locale.ROOT, "INFO ground at y %.2f", by));
        addTrials();
    }

    private void addTrials() {
        trials.add(new Trial(5) {

            @Override
            void start() {
                player.setPositionAndUpdate(bx, by, bz);
                MinecraftServer.getServer()
                    .getCommandManager()
                    .executeCommand(player, "etjarea trial_sap here");
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!timedOut) return false;
                EntityLingeringArea area = find(AreaTypes.TRIAL_SAP, bx, bz);
                record(
                    verdict(
                        area != null && Math.abs(area.posY - by) < EPS,
                        "/etjarea lays a sap floor on the ground under the player: "
                            + (area == null ? null : String.format(Locale.ROOT, "y %.2f", area.posY))));
                return true;
            }
        });
        trials.add(new Trial(2) {

            EntityLingeringArea dropped, floating;

            @Override
            void start() {
                dropped = keep(Areas.placeOnGround(AreaTypes.TRIAL_SAP, world, null, bx + 6, by + 3, bz));
                floating = keep(Areas.placeOnGround(AreaTypes.TRIAL_SAP, world, null, bx + 6, by + 30, bz));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!timedOut) return false;
                record(
                    verdict(
                        dropped != null && Math.abs(dropped.posY - by) < EPS,
                        "a floor put 3 blocks up falls to the ground: "
                            + (dropped == null ? null : String.format(Locale.ROOT, "y %.2f", dropped.posY))));
                record(verdict(floating == null, "a floor with no ground within reach is not laid"));
                return true;
            }
        });
        trials.add(new Trial(40) {

            EntityPig pig;
            EntityLingeringArea embers;

            @Override
            void start() {
                pig = pig(6, 0);
                embers = keep(Areas.placeOnGround(AreaTypes.TRIAL_EMBERS, world, player, bx + 6, by + 0.5, bz));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!timedOut) return false;
                record(verdict(hurt(pig), "the player's embers hurt a pig standing in them: " + health(pig)));
                ElementalDamageSource hit = hits.get(pig.getEntityId());
                record(
                    verdict(
                        hit != null && hit.damage()
                            .dominant() == Elements.FIRE,
                        "the hit is fire: " + (hit == null ? null
                            : hit.damage()
                                .dominant())));
                record(
                    verdict(
                        hit != null && hit.getSourceOfDamage() == embers && hit.getEntity() == player,
                        "the area touched the pig and the player is to blame"));
                record(verdict(pig.isBurning(), "the embers set the pig alight"));
                record(
                    verdict(
                        embers.radius() < AreaTypes.TRIAL_EMBERS.size.radius,
                        String.format(Locale.ROOT, "the embers burn down as they burn: r %.2f", embers.radius())));
                return true;
            }
        });
        trials.add(new Trial(30) {

            EntityPig pig;

            @Override
            void start() {
                player.setPositionAndUpdate(bx + 12, by, bz);
                pig = pig(12, 1);
                keep(Areas.placeOnGround(AreaTypes.TRIAL_SPORES, world, null, bx + 12, by + 1, bz + 0.5));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                player.setPositionAndUpdate(bx + 12, by, bz);
                if (!timedOut) return false;
                record(verdict(player.isPotionActive(Potion.poison), "an enemy spore cloud poisons the player in it"));
                record(
                    verdict(
                        hits.containsKey(player.getEntityId()),
                        "and hits them: " + hits.get(player.getEntityId())));
                record(
                    verdict(
                        !pig.isPotionActive(Potion.poison) && !hurt(pig),
                        "it spares the pig beside them, an ally of the enemy: " + health(pig)));
                return true;
            }
        });
        trials.add(new Trial(10) {

            EntityPig pig;
            EntityLingeringArea sap;
            double during, jump;

            @Override
            void start() {
                player.setPositionAndUpdate(bx, by, bz);
                pig = pig(18, 0);
                sap = keep(Areas.placeOnGround(AreaTypes.TRIAL_SAP, world, null, bx + 18, by + 0.5, bz));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (t == 5) {
                    during = AreaSlow.factor(pig);
                    pig.motionY = VANILLA_JUMP;
                    MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(pig));
                    jump = pig.motionY;
                    pig.motionY = 0;
                    sap.setDead();
                }
                if (!timedOut) return false;
                record(
                    verdict(
                        Math.abs(during - AreaTypes.TRIAL_SAP.slow) < EPS,
                        String.format(
                            Locale.ROOT,
                            "an enemy sap floor, taking allies along, slows a pig: x%.2f",
                            during)));
                double expected = VANILLA_JUMP * Math.sqrt(AreaTypes.TRIAL_SAP.slow);
                record(
                    verdict(
                        Math.abs(jump - expected) < EPS,
                        String.format(
                            Locale.ROOT,
                            "the sap floor holds back the pig's jump: %.3f, expected %.3f",
                            jump,
                            expected)));
                record(
                    verdict(
                        AreaSlow.factor(pig) == 1,
                        String.format(
                            Locale.ROOT,
                            "the pig moves freely once the floor is gone: x%.2f",
                            AreaSlow.factor(pig))));
                return true;
            }
        });
        trials.add(new Trial(AreaTypes.TRIAL_SAP.size.duration + 5) {

            EntityLingeringArea sap;
            double shrinking = -1;

            @Override
            void start() {
                sap = keep(Areas.placeOnGround(AreaTypes.TRIAL_SAP, world, null, bx + 24, by + 0.5, bz));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (t == AreaTypes.TRIAL_SAP.size.duration - 30) shrinking = sap.radius();
                if (!timedOut) return false;
                record(
                    verdict(
                        shrinking > 0 && shrinking < AreaTypes.TRIAL_SAP.size.radius,
                        String.format(Locale.ROOT, "a sap floor shrinks near its end: r %.2f", shrinking)));
                record(
                    verdict(
                        sap.isDead && find(AreaTypes.TRIAL_SAP, bx + 24, bz) == null,
                        "a sap floor is gone after its " + AreaTypes.TRIAL_SAP.size.duration + " ticks"));
                return true;
            }
        });
        trials.add(new Trial(100) {

            EntityPig pig;
            EntityLingeringArea cloud;

            @Override
            void start() {
                player.setPositionAndUpdate(bx, by, bz);
                pig = pig(10, 0);
                keep(Projectiles.shootAt(AreaTypes.TRIAL_SPORE_POD, world, player, bx, by + 1.5, bz, pig));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (cloud == null) cloud = find(AreaTypes.TRIAL_SPORES, bx + 10, bz);
                if (cloud == null && !timedOut) return false;
                record(
                    verdict(
                        cloud != null,
                        "a spore pod thrown at a pig leaves a spore cloud on it" + (cloud == null ? ""
                            : String.format(
                                Locale.ROOT,
                                ": %.2f from the pig",
                                Math.hypot(cloud.posX - pig.posX, cloud.posZ - pig.posZ)))));
                keep(cloud);
                return true;
            }
        });
        trials.add(new Trial(60) {

            EntityZombie zombie;
            EntityLingeringArea trail;

            @Override
            void start() {
                // Peaceful would remove the zombie on its first tick.
                world.difficultySetting = EnumDifficulty.EASY;
                player.setPositionAndUpdate(bx, by, bz);
                zombie = new EntityZombie(world);
                zombie.setLocationAndAngles(bx + 8, by, bz, 0, 0);
                world.spawnEntityInWorld(zombie);
                keep(zombie);
                EnemyRanks.give(zombie, Rank.RARE, Collections.singletonList(TrailAbility.ID));
                zombie.setAttackTarget(player);
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                for (Object o : world.loadedEntityList)
                    if (o instanceof EntityLingeringArea && ((EntityLingeringArea) o).spec() == AreaTypes.RANK_TRAIL
                        && ((EntityLingeringArea) o).owner() == zombie) trail = (EntityLingeringArea) o;
                if (trail == null && !timedOut) return false;
                record(
                    verdict(
                        trail != null && Math.abs(trail.posY - by) < EPS,
                        "a rare zombie with the trail ability leaves a floor as it chases the player"
                            + (trail == null ? ""
                                : String.format(
                                    Locale.ROOT,
                                    ": %.2f from it",
                                    Math.hypot(trail.posX - zombie.posX, trail.posZ - zombie.posZ)))));
                world.difficultySetting = EnumDifficulty.PEACEFUL;
                return true;
            }
        });
    }

    /** A live area of that kind within 3 blocks of the point, or null. */
    private EntityLingeringArea find(AreaSpec spec, double x, double z) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - 3, by - 3, z - 3, x + 3, by + 6, z + 3);
        for (Object o : world.getEntitiesWithinAABB(EntityLingeringArea.class, box)) {
            EntityLingeringArea a = (EntityLingeringArea) o;
            if (!a.isDead && a.spec() == spec) return a;
        }
        return null;
    }

    private EntityPig pig(double dx, double dz) {
        EntityPig pig = new EntityPig(world);
        double[] at = { bx + dx, by, bz + dz };
        pig.setLocationAndAngles(at[0], at[1], at[2], 0, 0);
        pig.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(PIG_HEALTH);
        pig.setHealth(PIG_HEALTH);
        world.spawnEntityInWorld(pig);
        held.put(pig, at);
        spawned.add(pig);
        return pig;
    }

    private <E extends Entity> E keep(E e) {
        if (e != null) spawned.add(e);
        return e;
    }

    /** Keeps the pigs where they were put. */
    private void hold() {
        for (Map.Entry<EntityPig, double[]> e : held.entrySet()) {
            EntityPig pig = e.getKey();
            double[] at = e.getValue();
            pig.setPosition(at[0], at[1], at[2]);
            pig.motionX = pig.motionY = pig.motionZ = 0;
        }
    }

    private void clear() {
        for (Entity e : spawned) e.setDead();
        // Areas left by the command are not in the list.
        for (Object o : world.loadedEntityList) if (o instanceof EntityLingeringArea) ((Entity) o).setDead();
        spawned.clear();
        held.clear();
    }

    private static boolean hurt(EntityPig pig) {
        return pig.getHealth() < PIG_HEALTH;
    }

    private static String health(EntityPig pig) {
        return String.format(Locale.ROOT, "%.1f/%.0f", pig.getHealth(), PIG_HEALTH);
    }

    private static String verdict(boolean pass, String what) {
        return (pass ? "PASS " : "FAIL ") + what;
    }

    private void finish() {
        File file = MinecraftServer.getServer()
            .getFile("areacheck.txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModLog.LOG.error("[areacheck] cannot write {}", file, e);
        }
        done = true;
    }

    private void record(String line) {
        lines.add(line);
        ModLog.LOG.info("[areacheck] {}", line);
    }
}
