package chlorine.etjourney.content.projectile.check;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.projectile.EntityEtjProjectile;
import chlorine.etjourney.content.projectile.ProjectileSpec;
import chlorine.etjourney.content.projectile.ProjectileTypes;
import chlorine.etjourney.content.projectile.Projectiles;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.util.ModLog;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Server half of the projectile check: fires the trial shots at pigs held in the air and checks that they fly, hit,
 * carry their element, home, and pass or stop at walls as their specs say.
 */
public final class ProjectileCheckServer {

    private static final int JOIN_TICKS = 100;
    private static final double HEIGHT = 40;
    private static final float PIG_HEALTH = 200;

    static volatile boolean done;
    /** The name of the first shot the client saw with its spec. */
    static volatile String clientSaw;

    /** One check: starts, then is polled each tick until it reports itself finished. */
    private abstract class Trial {

        final String name;
        final int timeout;

        Trial(String name, int timeout) {
            this.name = name;
            this.timeout = timeout;
        }

        abstract void start();

        /** True when it has recorded its verdict; timedOut is set on its last poll. */
        abstract boolean poll(int t, boolean timedOut);
    }

    private final List<String> lines = new ArrayList<>();
    private final List<Trial> trials = new ArrayList<>();
    private final Map<EntityPig, double[]> held = new HashMap<>();
    private final Map<Integer, DamageSource> attacks = new HashMap<>();
    private final List<Entity> spawned = new ArrayList<>();
    private final List<int[]> placed = new ArrayList<>();
    private EntityPlayerMP player;
    private WorldServer world;
    private double bx, by, bz;
    private int index = -1, t, waited;

    private ProjectileCheckServer() {}

    static void register() {
        ProjectileCheckServer check = new ProjectileCheckServer();
        FMLCommonHandler.instance()
            .bus()
            .register(check);
        MinecraftForge.EVENT_BUS.register(check);
    }

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (!event.entity.worldObj.isRemote) attacks.put(event.entity.getEntityId(), event.source);
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
            ModLog.LOG.error("[projectilecheck] crashed", e);
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
            record((clientSaw != null ? "PASS" : "FAIL") + " the client got a shot with its kind: " + clientSaw);
            finish();
        }
    }

    private void begin() {
        t = 0;
        attacks.clear();
        trials.get(index)
            .start();
    }

    private void setUp(EntityPlayerMP p) {
        player = p;
        world = (WorldServer) p.worldObj;
        world.difficultySetting = EnumDifficulty.PEACEFUL;
        bx = Math.floor(p.posX) + 0.5;
        by = HEIGHT;
        bz = Math.floor(p.posZ) + 0.5;
        // A survival player the enemy shots can reach, shielded from their damage.
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.addPotionEffect(new PotionEffect(Potion.resistance.id, 100_000, 4));
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        p.setPositionAndUpdate(bx, by, bz);
        addTrials();
    }

    private void addTrials() {
        trials.add(new Trial("flight", 10) {

            EntityEtjProjectile beam, rock;
            double startY;

            @Override
            void start() {
                beam = keep(
                    Projectiles.shootAlong(
                        ProjectileTypes.TRIAL_BEAM,
                        world,
                        null,
                        bx,
                        by + 3,
                        bz + 10,
                        new double[] { 1, 0, 0 }));
                startY = by + 3;
                rock = keep(
                    Projectiles.shootAlong(
                        ProjectileTypes.TRIAL_ROCK,
                        world,
                        null,
                        bx,
                        startY,
                        bz + 14,
                        new double[] { 1, 0, 0 }));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!timedOut) return false;
                double flown = beam.posX - bx;
                record(
                    String.format(
                        Locale.ROOT,
                        "%s a beam flies 15 blocks in 10 ticks: %.2f",
                        Math.abs(flown - 15) <= 1.5 && !beam.isDead ? "PASS" : "FAIL",
                        flown));
                double fell = startY - rock.posY;
                record(
                    String.format(
                        Locale.ROOT,
                        "%s a rock falls in 10 ticks: %.2f blocks",
                        fell >= 1 ? "PASS" : "FAIL",
                        fell));
                return true;
            }
        });
        trials.add(new Trial("hit", 30) {

            EntityPig pig;

            @Override
            void start() {
                pig = pig(8, 0);
                keep(Projectiles.shootAt(ProjectileTypes.TRIAL_BEAM, world, player, bx, by, bz, pig));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!hurt(pig) && !timedOut) return false;
                record(verdict(hurt(pig), "a beam hits a pig: " + health(pig)));
                DamageSource source = attacks.get(pig.getEntityId());
                boolean elemental = source instanceof ElementalDamageSource;
                record(verdict(elemental, "the hit is elemental: " + (source == null ? null : source.damageType)));
                if (elemental) {
                    ElementalDamageSource e = (ElementalDamageSource) source;
                    record(
                        verdict(
                            e.damage()
                                .dominant() == Elements.ICE,
                            "the hit is ice: " + e.damage()
                                .dominant()));
                    record(verdict(e.isProjectile(), "the hit is marked as a projectile"));
                    record(verdict(e.getEntity() == player, "the player is to blame for the hit"));
                    record(
                        verdict(
                            e.getSourceOfDamage() instanceof EntityEtjProjectile,
                            "the shot itself touched the pig"));
                }
                record(verdict(pig.isPotionActive(Potion.moveSlowdown), "the beam's impact effect slows the pig"));
                return true;
            }
        });
        trials.add(new Trial("lob", 100) {

            EntityPig pig;

            @Override
            void start() {
                pig = pig(12, 0);
                keep(Projectiles.shootAt(ProjectileTypes.TRIAL_ROCK, world, player, bx, by, bz, pig));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!hurt(pig) && !timedOut) return false;
                record(verdict(hurt(pig), "a lobbed rock comes down on a pig 12 blocks away: " + health(pig)));
                return true;
            }
        });
        trials.add(seeking(ProjectileTypes.TRIAL_SEEKER, true));
        trials.add(seeking(ProjectileTypes.TRIAL_BEAM, false));
        trials.add(new Trial("beam through a wall", 30) {

            EntityPig pig;

            @Override
            void start() {
                wall(4);
                pig = pig(8, 0);
                keep(Projectiles.shootAt(ProjectileTypes.TRIAL_BEAM, world, player, bx, by, bz, pig));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!hurt(pig) && !timedOut) return false;
                record(verdict(hurt(pig), "a beam passes a stone wall and hits the pig behind: " + health(pig)));
                return true;
            }
        });
        trials.add(new Trial("rock at a wall", 30) {

            EntityPig pig;
            EntityEtjProjectile rock;

            @Override
            void start() {
                wall(4);
                pig = pig(8, 0);
                rock = keep(
                    Projectiles
                        .shootAlong(ProjectileTypes.TRIAL_ROCK, world, player, bx, by, bz, new double[] { 1, 0, 0 }));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!timedOut) return false;
                record(
                    verdict(
                        rock.isDead && Math.abs(rock.posX - (bx + 3.5)) < 1,
                        String.format(
                            Locale.ROOT,
                            "a rock stops at the wall: dead %s at %.2f",
                            rock.isDead,
                            rock.posX - bx)));
                record(verdict(!hurt(pig), "the pig behind the wall is unhurt: " + health(pig)));
                return true;
            }
        });
        trials.add(new Trial("enemy shot", 30) {

            EntityPig pig;

            @Override
            void start() {
                pig = pig(6, 0);
                // Low enough that the pig sits on the line to the player's feet.
                keep(
                    Projectiles
                        .shoot(ProjectileTypes.TRIAL_BEAM, world, null, bx + 12, by, bz, bx, by + 0.2, bz, player));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                DamageSource source = attacks.get(player.getEntityId());
                if (source == null && !timedOut) return false;
                record(
                    verdict(
                        source instanceof ElementalDamageSource,
                        "a shooterless shot reaches the player: " + (source == null ? null : source.damageType)));
                record(verdict(!hurt(pig), "the enemy shot passes through the pig on the way: " + health(pig)));
                return true;
            }
        });
        trials.add(new Trial("own shot", 10) {

            @Override
            void start() {
                keep(
                    Projectiles.shootAlong(
                        ProjectileTypes.TRIAL_BEAM,
                        world,
                        player,
                        player.posX,
                        player.posY + 1,
                        player.posZ,
                        new double[] { 1, 0, 0 }));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (!timedOut) return false;
                record(
                    verdict(
                        !attacks.containsKey(player.getEntityId()),
                        "a player's shot fired from inside them never hits them"));
                return true;
            }
        });
    }

    /** Fires 60 degrees off a pig; a homing shot should still land and a straight one should not. */
    private Trial seeking(ProjectileSpec spec, boolean homes) {
        return new Trial("seeking " + spec.name, 100) {

            EntityPig pig;

            @Override
            void start() {
                pig = pig(10, 0);
                double a = Math.toRadians(60);
                keep(
                    Projectiles
                        .shootAlong(spec, world, player, bx, by, bz, new double[] { Math.cos(a), 0, Math.sin(a) }));
            }

            @Override
            boolean poll(int t, boolean timedOut) {
                if (homes && !hurt(pig) && !timedOut) return false;
                if (!homes && !timedOut && t < 40) return false;
                String what = homes ? "turns onto" : "misses";
                record(
                    verdict(
                        hurt(pig) == homes,
                        String.format(
                            Locale.ROOT,
                            "a %s fired 60 degrees off %s the pig: %s",
                            spec.name,
                            what,
                            health(pig))));
                return true;
            }
        };
    }

    private EntityPig pig(double dx, double dz) {
        EntityPig pig = new EntityPig(world);
        double[] at = { bx + dx, by - 0.45, bz + dz };
        pig.setLocationAndAngles(at[0], at[1], at[2], 0, 0);
        pig.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(PIG_HEALTH);
        pig.setHealth(PIG_HEALTH);
        world.spawnEntityInWorld(pig);
        held.put(pig, at);
        spawned.add(pig);
        return pig;
    }

    /** A 5 by 5 stone wall across the x axis, dx blocks ahead. */
    private void wall(int dx) {
        int x = (int) Math.floor(bx) + dx, y0 = (int) Math.floor(by), z0 = (int) Math.floor(bz);
        for (int y = y0 - 2; y <= y0 + 2; y++) for (int z = z0 - 2; z <= z0 + 2; z++) {
            world.setBlock(x, y, z, Blocks.stone);
            placed.add(new int[] { x, y, z });
        }
    }

    private <E extends Entity> E keep(E e) {
        spawned.add(e);
        return e;
    }

    /** Keeps the pigs where they were put, floating in the air. */
    private void hold() {
        for (Map.Entry<EntityPig, double[]> e : held.entrySet()) {
            EntityPig pig = e.getKey();
            double[] at = e.getValue();
            pig.setPosition(at[0], at[1], at[2]);
            pig.motionX = pig.motionY = pig.motionZ = 0;
            pig.fallDistance = 0;
        }
    }

    private void clear() {
        for (Entity e : spawned) e.setDead();
        for (int[] b : placed) world.setBlockToAir(b[0], b[1], b[2]);
        spawned.clear();
        placed.clear();
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
            .getFile("projectilecheck.txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModLog.LOG.error("[projectilecheck] cannot write {}", file, e);
        }
        done = true;
    }

    private void record(String line) {
        lines.add(line);
        ModLog.LOG.info("[projectilecheck] {}", line);
    }
}
