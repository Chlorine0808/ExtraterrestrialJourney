package chlorine.etjourney.content.end.spawncheck;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import chlorine.etjourney.content.end.EntitySporeZombie;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.content.rank.RankOdds;
import chlorine.etjourney.content.rank.RankStage;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.debug.EndSearch;
import chlorine.etjourney.world.end.feature.EndBiome;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Server half of the spawn check: at a MYCELIA and a GROVE site, counts mobs after a spell with the spore zombie on
 * the End's spawn list and a spell with it taken off, and the ranks the mobs that joined rolled. After the first
 * spell it kills a spore zombie there and watches its cloud.
 */
public final class SpawnCheckServer {

    private static final int END = 1;
    /** Ticks the player must have lived before moving to the End. */
    private static final int JOIN_TICKS = 100;
    /** Ticks after a move for the chunks around the player to load, before the mobs are cleared. */
    private static final int SETTLE_TICKS = 200;
    private static final int SPELL_TICKS = 2400;
    /** Horizontal reach of the count: as far as natural spawning reaches from the player. */
    private static final int RADIUS = 128;
    /** Where the site searches start: well out in the outer End. */
    private static final double SEARCH_X = 1500;

    static volatile boolean done;

    private final List<String> lines = new ArrayList<>();
    private final List<Run> runs = new ArrayList<>();
    private int runIndex = -1, phaseTicks;
    private boolean settled, sent;
    private BiomeGenBase.SpawnListEntry entry;
    /** Runs at the first site before its mobs are cleared; null once done. */
    private RankAbilityCheck abilities;
    /** Runs at the first site after its spell is counted; null once done. */
    private SporeCloudCheck cloud;
    private boolean counted;
    private EntityPlayerMP player;
    /** Mobs that joined the End during the current spell, by kind. */
    private int joinedEndermen, joinedSpores, joinedOthers;
    /** Monsters that joined during the current spell; their ranks are read when it ends. */
    private final List<EntityLiving> joinedMobs = new ArrayList<>();
    /** Over every spell: monsters that rolled, those that rolled rare, and those that rolled higher. */
    private int rolled, rare, higher;

    private SpawnCheckServer() {}

    static void register() {
        SpawnCheckServer server = new SpawnCheckServer();
        FMLCommonHandler.instance()
            .bus()
            .register(server);
        MinecraftForge.EVENT_BUS.register(server);
    }

    private static final class Run {

        final EndBiome biome;
        final double x, z;
        final boolean listed;
        int endermen, spores, others;
        int joinedEndermen, joinedSpores, joinedOthers;

        Run(EndBiome biome, double[] at, boolean listed) {
            this.biome = biome;
            x = at[0];
            z = at[1];
            this.listed = listed;
        }

        String label() {
            return biome + (listed ? " with" : " without");
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.world.isRemote || event.world.provider.dimensionId != END || !settled) return;
        Entity e = event.entity;
        if (cloud != null && player != null) cloud.onJoin(e, player);
        if (e instanceof EntitySporeZombie) joinedSpores++;
        else if (e instanceof EntityEnderman) joinedEndermen++;
        else if (e instanceof IMob) joinedOthers++;
        if (e instanceof IMob && e instanceof EntityLiving) joinedMobs.add((EntityLiving) e);
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
            ModLog.LOG.error("[spawncheck] crashed", e);
            record("FAIL crash: " + e);
            finish();
        }
    }

    private void step(EntityPlayerMP player) {
        this.player = player;
        if (runIndex < 0) {
            if (player.ticksExisted < JOIN_TICKS) return;
            if (player.dimension != END) {
                if (!sent) player.travelToDimension(END);
                sent = true;
                return;
            }
            if (plan()) start(player, 0);
            else finish();
            return;
        }
        Run run = runs.get(runIndex);
        WorldServer end = DimensionManager.getWorld(END);
        phaseTicks++;
        if (!settled) {
            if (phaseTicks < SETTLE_TICKS) return;
            if (abilities != null) {
                if (!abilities.step(end, player)) return;
                abilities = null;
            }
            clearMobs(end);
            setListed(run.listed);
            settled = true;
            phaseTicks = 0;
            joinedEndermen = joinedSpores = joinedOthers = 0;
            joinedMobs.clear();
            return;
        }
        if (phaseTicks < SPELL_TICKS) return;
        if (counted) {
            if (!cloud.step(end, player)) return;
            cloud = null;
            nextRun(player);
            return;
        }
        count(end, run);
        run.joinedEndermen = joinedEndermen;
        run.joinedSpores = joinedSpores;
        run.joinedOthers = joinedOthers;
        tallyRanks();
        record(
            String.format(
                Locale.ROOT,
                "INFO %s: standing endermen %d, spore zombies %d, others %d; joined endermen %d, spore zombies %d, others %d",
                run.label(),
                run.endermen,
                run.spores,
                run.others,
                run.joinedEndermen,
                run.joinedSpores,
                run.joinedOthers));
        if (cloud != null) {
            counted = true;
            return;
        }
        nextRun(player);
    }

    private void nextRun(EntityPlayerMP player) {
        counted = false;
        if (runIndex + 1 < runs.size()) {
            start(player, runIndex + 1);
            return;
        }
        setListed(true);
        judge();
        finish();
    }

    private boolean plan() {
        for (WorldServer w : DimensionManager.getWorlds()) w.difficultySetting = EnumDifficulty.NORMAL;
        StringBuilder listed = new StringBuilder();
        for (BiomeGenBase.SpawnListEntry e : monsters()) {
            listed.append(' ')
                .append(e.entityClass.getSimpleName())
                .append('=')
                .append(e.itemWeight);
            if (e.entityClass == EntitySporeZombie.class) entry = e;
        }
        record("INFO End monster list:" + listed);
        if (entry == null) {
            record("FAIL the spore zombie is not on the End's monster list");
            return false;
        }
        Long seed = EndTerrain.seed();
        if (seed == null) {
            record("FAIL no End seed");
            return false;
        }
        TerrainSampler sampler = EndTerrain.sampler(seed);
        double[] mycelia = EndSearch.nearestBiome(sampler, EndBiome.MYCELIA, SEARCH_X, 0);
        double[] grove = EndSearch.nearestBiome(sampler, EndBiome.GROVE, SEARCH_X, 0);
        if (mycelia == null || grove == null) {
            record("FAIL no site: MYCELIA " + (mycelia != null) + ", GROVE " + (grove != null));
            return false;
        }
        record(
            String.format(
                Locale.ROOT,
                "INFO sites: MYCELIA %.0f %.0f, GROVE %.0f %.0f",
                mycelia[0],
                mycelia[1],
                grove[0],
                grove[1]));
        abilities = new RankAbilityCheck(this::record);
        cloud = new SporeCloudCheck(this::record);
        runs.add(new Run(EndBiome.MYCELIA, mycelia, true));
        runs.add(new Run(EndBiome.MYCELIA, mycelia, false));
        runs.add(new Run(EndBiome.GROVE, grove, true));
        runs.add(new Run(EndBiome.GROVE, grove, false));
        return true;
    }

    private void start(EntityPlayerMP player, int index) {
        runIndex = index;
        phaseTicks = 0;
        settled = false;
        Run run = runs.get(index);
        TerrainSampler sampler = EndTerrain.sampler(EndTerrain.seed());
        double y = sampler.bareColumn(run.x, run.z, true).top + 3;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.setPositionAndUpdate(run.x, y, run.z);
    }

    private static void clearMobs(WorldServer end) {
        for (Object o : new ArrayList<>(end.loadedEntityList)) {
            if (o instanceof IMob) ((Entity) o).setDead();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<BiomeGenBase.SpawnListEntry> monsters() {
        return BiomeGenBase.sky.getSpawnableList(EnumCreatureType.monster);
    }

    private void setListed(boolean listed) {
        List<BiomeGenBase.SpawnListEntry> list = monsters();
        list.remove(entry);
        if (listed) list.add(entry);
    }

    private static void count(WorldServer end, Run run) {
        for (Object o : end.loadedEntityList) {
            if (!(o instanceof IMob)) continue;
            Entity e = (Entity) o;
            if (Math.max(Math.abs(e.posX - run.x), Math.abs(e.posZ - run.z)) > RADIUS) continue;
            if (e instanceof EntitySporeZombie) run.spores++;
            else if (e instanceof EntityEnderman) run.endermen++;
            else run.others++;
        }
    }

    private void judge() {
        Run myc = runs.get(0), mycOff = runs.get(1), grove = runs.get(2), groveOff = runs.get(3);
        record((myc.spores > 0 ? "PASS" : "FAIL") + " spore zombies stand in MYCELIA: " + myc.spores);
        int strays = grove.spores + grove.joinedSpores;
        record((strays == 0 ? "PASS" : "FAIL") + " no spore zombie in GROVE: " + strays);
        keep("MYCELIA standing", myc.endermen, mycOff.endermen);
        keep("GROVE standing", grove.endermen, groveOff.endermen);
        keep("MYCELIA joined", myc.joinedEndermen, mycOff.joinedEndermen);
        keep("GROVE joined", grove.joinedEndermen, groveOff.joinedEndermen);
        record(
            String.format(
                Locale.ROOT,
                "INFO spore zombie share in MYCELIA: standing %.0f%%, joined %.0f%%",
                100 * SpawnTally.share(myc.spores, myc.endermen),
                100 * SpawnTally.share(myc.joinedSpores, myc.joinedEndermen)));
        judgeRanks();
    }

    private void tallyRanks() {
        for (EntityLiving e : joinedMobs) {
            if (!RankData.rolled(e) || RankData.role(e) != RankData.FREE) continue;
            rolled++;
            Rank rank = RankData.rank(e);
            if (rank == Rank.RARE) rare++;
            else if (rank != Rank.NORMAL) higher++;
        }
        joinedMobs.clear();
    }

    private void judgeRanks() {
        double chance = RankOdds.chance(RankStage.EARLY, Rank.RARE);
        boolean fits = rare > 0 && SpawnTally.rankShareFits(rare, rolled, chance);
        record(
            String.format(
                Locale.ROOT,
                "%s rare share near %.0f%%: %d of %d (%.1f%%)",
                fits ? "PASS" : "FAIL",
                100 * chance,
                rare,
                rolled,
                rolled == 0 ? 0 : 100.0 * rare / rolled));
        record((higher == 0 ? "PASS" : "FAIL") + " nothing above rare in the early stage: " + higher);
    }

    private void keep(String what, int with, int without) {
        String verdict = SpawnTally.endermenKept(with, without) ? "PASS" : "FAIL";
        record(verdict + " endermen kept, " + what + ": " + with + " of " + without);
    }

    private void finish() {
        File file = MinecraftServer.getServer()
            .getFile("spawncheck.txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ModLog.LOG.error("[spawncheck] cannot write {}", file, e);
        }
        done = true;
    }

    private void record(String line) {
        lines.add(line);
        ModLog.LOG.info("[spawncheck] {}", line);
    }
}
