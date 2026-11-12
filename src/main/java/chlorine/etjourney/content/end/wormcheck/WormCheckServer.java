package chlorine.etjourney.content.end.wormcheck;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.content.end.EntityFlameWorm;
import chlorine.etjourney.content.worm.WormPhases.Phase;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.debug.EndSearch;
import chlorine.etjourney.world.end.feature.EndBiome;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Server half of the worm check: summons a flame worm by a shielded survival player, then checks that its segments
 * trail behind it, that it goes through a warn and a leap, and that hits on its segments come off the head's health.
 */
public final class WormCheckServer {

    private static final int END = 1;
    private static final int JOIN_TICKS = 100, SETTLE_TICKS = 200, TRAIL_TICKS = 100;
    /** How long the worm gets to warn, leap and dive back. */
    private static final int CYCLE_TICKS = 1200;
    /** Longer than the head's invulnerability after a hit. */
    private static final int HIT_GAP_TICKS = 25;
    private static final int CLIENT_TICKS = 200;
    private static final float HIT = 10;
    private static final double SEARCH_X = 1500;

    static volatile boolean done;
    /** The server's IDs of the worm's parts, for the client to compare its own with. */
    static volatile int[] partIds;
    private static volatile String clientLine;

    private enum Stage {
        JOIN,
        SETTLE,
        TRAIL,
        CYCLE,
        HIT_BODY,
        HIT_TAIL,
        CLIENT
    }

    private final List<String> lines = new ArrayList<>();
    private Stage stage = Stage.JOIN;
    private int ticks;
    private boolean sent, warned, leapt, dived;
    private double siteX, siteY, siteZ;
    private EntityFlameWorm worm;
    private float healthBefore;

    private WormCheckServer() {}

    static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(new WormCheckServer());
    }

    static void clientVerdict(boolean same, int parts) {
        clientLine = (same ? "PASS" : "FAIL") + " the client gives the " + parts + " parts the server's IDs";
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
            ModLog.LOG.error("[wormcheck] crashed", e);
            record("FAIL crash: " + e);
            finish();
        }
    }

    private void step(EntityPlayerMP player) {
        ticks++;
        switch (stage) {
            case JOIN:
                if (player.ticksExisted < JOIN_TICKS) return;
                if (player.dimension != END) {
                    if (!sent) player.travelToDimension(END);
                    sent = true;
                    return;
                }
                if (!placePlayer(player)) {
                    finish();
                    return;
                }
                next(Stage.SETTLE);
                return;
            case SETTLE:
                if (ticks < SETTLE_TICKS) return;
                summon(DimensionManager.getWorld(END));
                next(Stage.TRAIL);
                return;
            case TRAIL:
                if (ticks < TRAIL_TICKS) return;
                judgeTrail();
                next(Stage.CYCLE);
                return;
            case CYCLE:
                watchCycle();
                if ((warned && leapt && dived) || ticks >= CYCLE_TICKS) {
                    record((warned ? "PASS" : "FAIL") + " the worm warned");
                    record((leapt ? "PASS" : "FAIL") + " the worm leapt");
                    record((dived ? "PASS" : "FAIL") + " the worm went back to swimming after the leap");
                    record(String.format(Locale.ROOT, "INFO cycle took %d ticks", ticks));
                    healthBefore = worm.getHealth();
                    hit(player, worm.getParts().length / 2, "a middle body segment");
                    next(Stage.HIT_BODY);
                }
                return;
            case HIT_BODY:
                if (ticks < HIT_GAP_TICKS) return;
                healthBefore = worm.getHealth();
                hit(player, worm.getParts().length - 1, "the tail");
                next(Stage.HIT_TAIL);
                return;
            case HIT_TAIL:
                if (ticks < HIT_GAP_TICKS) return;
                next(Stage.CLIENT);
                return;
            default:
                if (clientLine == null && ticks < CLIENT_TICKS) return;
                record(clientLine != null ? clientLine : "FAIL the client never saw the worm");
                worm.setDead();
                finish();
        }
    }

    private boolean placePlayer(EntityPlayerMP player) {
        for (WorldServer w : DimensionManager.getWorlds()) w.difficultySetting = EnumDifficulty.NORMAL;
        Long seed = EndTerrain.seed();
        if (seed == null) {
            record("FAIL no End seed");
            return false;
        }
        TerrainSampler sampler = EndTerrain.sampler(seed);
        double[] site = EndSearch.nearestBiome(sampler, EndBiome.MYCELIA, SEARCH_X, 0);
        if (site == null) {
            record("FAIL no MYCELIA site to stand on");
            return false;
        }
        siteX = site[0];
        siteZ = site[1];
        siteY = sampler.bareColumn(siteX, siteZ, true).top;
        record(String.format(Locale.ROOT, "INFO site %.0f %.0f, ground at %.0f", siteX, siteZ, siteY));
        // A survival player the worm will chase, shielded from its hits and its fire.
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        player.addPotionEffect(new PotionEffect(Potion.resistance.id, 100_000, 4));
        player.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 100_000, 0));
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.setPositionAndUpdate(siteX, siteY + 3, siteZ);
        return true;
    }

    private void summon(WorldServer end) {
        worm = new EntityFlameWorm(end);
        worm.setLocationAndAngles(siteX + 10, siteY - 4, siteZ, 90, 0);
        worm.onSpawnWithEgg(null);
        end.spawnEntityInWorld(worm);
        Entity[] parts = worm.getParts();
        int want = worm.spec.bodySegments + 2;
        record((parts.length == want ? "PASS" : "FAIL") + " the worm has " + want + " parts: " + parts.length);
        int[] ids = new int[parts.length];
        boolean found = true;
        for (int i = 0; i < parts.length; i++) {
            ids[i] = parts[i].getEntityId();
            found &= end.getEntityByID(ids[i]) == parts[i];
        }
        record((found ? "PASS" : "FAIL") + " the server finds every part by its ID");
        partIds = ids;
    }

    private void judgeTrail() {
        Entity[] parts = worm.getParts();
        // The first part is the head's own hitbox, so the parts alone run head to tail.
        double[][] centres = new double[parts.length][];
        for (int i = 0; i < parts.length; i++) {
            Entity p = parts[i];
            centres[i] = new double[] { p.posX, p.posY + p.height / 2, p.posZ };
        }
        double spacing = worm.spec.spacing;
        String gaps = SegmentGaps.describe(centres);
        record((SegmentGaps.trail(centres, spacing) ? "PASS" : "FAIL") + " segments trail the head: " + gaps);
        // Vanilla finds parts through their owner's box; the far tail must still turn up in a lookup round it.
        Entity tail = parts[parts.length - 1];
        boolean found = worm.worldObj.getEntitiesWithinAABBExcludingEntity(null, tail.boundingBox.copy())
            .contains(tail);
        record(
            String.format(
                Locale.ROOT,
                "%s a lookup round the tail, %.1f blocks from the head, finds it",
                found ? "PASS" : "FAIL",
                worm.getDistanceToEntity(tail)));
    }

    private void watchCycle() {
        Phase phase = worm.phase();
        if (phase == Phase.WARN) warned = true;
        if (phase == Phase.LEAP) leapt = true;
        if (leapt && phase == Phase.ROAM) dived = true;
    }

    private void hit(EntityPlayerMP player, int index, String what) {
        Entity part = worm.getParts()[index];
        part.attackEntityFrom(DamageSource.causePlayerDamage(player), HIT);
        float after = worm.getHealth();
        record(
            String.format(
                Locale.ROOT,
                "%s hitting %s takes the head from %.1f to %.1f",
                after < healthBefore ? "PASS" : "FAIL",
                what,
                healthBefore,
                after));
    }

    private void next(Stage s) {
        stage = s;
        ticks = 0;
    }

    private void finish() {
        File file = MinecraftServer.getServer()
            .getFile("wormcheck.txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModLog.LOG.error("[wormcheck] cannot write {}", file, e);
        }
        done = true;
    }

    private void record(String line) {
        lines.add(line);
        ModLog.LOG.info("[wormcheck] {}", line);
    }
}
