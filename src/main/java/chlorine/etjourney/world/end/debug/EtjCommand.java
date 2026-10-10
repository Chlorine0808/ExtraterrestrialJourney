package chlorine.etjourney.world.end.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;

import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.feature.Valleys;
import chlorine.etjourney.world.end.feature.Zone;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.region.RegionMap;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Style;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.PredictedIslands;
import chlorine.etjourney.world.end.reserve.Reservations;

/** /etj end ...: inspect the End terrain and teleport to the nearest example of a feature. */
public final class EtjCommand extends CommandBase {

    private static final int END = 1;
    /** Length of each piece a look is traced in. */
    private static final int LOOK_STEP = 64;

    @Override
    public String getCommandName() {
        return "etj";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/etj look [range] | /etj end <here|styles|style <name>|style <name> pure|mix [a] [b] [c]|mountain [minTopY]|lake|hole|valley|zone <name>|heeisland|destitute>";
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        List<String> styles = new ArrayList<>();
        for (Style style : Styles.all()) styles.add(style.name);
        List<String> zones = new ArrayList<>();
        for (Zone zone : Zone.values()) zones.add(zone.name());
        return EtjArgs.complete(args, styles, zones);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equals("look")) {
            look(getCommandSenderAsPlayer(sender), args.length > 1 ? parseIntBounded(sender, args[1], 1, 4096) : 1024);
            return;
        }
        if (args.length < 2 || !args[0].equals("end")) {
            say(sender, "Usage: " + getCommandUsage(sender));
            return;
        }
        if (args[1].equals("styles")) {
            for (String line : EtjArgs.styleLines(Styles.all())) say(sender, line);
            return;
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (player.dimension != END) {
            player.travelToDimension(END);
            say(player, "Moved to the End; run the command again");
            return;
        }
        Long seed = EndTerrain.seed();
        if (seed == null) return;
        TerrainSampler sampler = EndTerrain.sampler(seed);
        String what = args[1];
        String arg = args.length > 2 ? args[2] : null;
        switch (what) {
            case "here":
                here(player, sampler);
                break;
            case "style":
                styleRegion(player, sampler, arg, args.length > 3 && args[3].equals("pure"));
                break;
            case "mix":
                mixRegion(player, sampler, Arrays.copyOfRange(args, 2, args.length));
                break;
            case "mountain":
                mountain(player, sampler, arg == null ? 128 : parseInt(sender, arg));
                break;
            case "lake":
                lake(player, sampler);
                break;
            case "hole":
                hole(player, sampler);
                break;
            case "valley":
                valley(player, sampler);
                break;
            case "zone":
                zone(player, sampler, arg);
                break;
            case "heeisland":
                hee(player, seed);
                break;
            case "destitute":
                destitute(player);
                break;
            default:
                say(sender, "Usage: " + getCommandUsage(sender));
        }
    }

    private static void here(EntityPlayerMP player, TerrainSampler sampler) {
        List<Map.Entry<Style, Double>> entries = new ArrayList<>(
            sampler.weights(player.posX, player.posZ)
                .asMap()
                .entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        StringBuilder out = new StringBuilder("Style here:");
        for (Map.Entry<Style, Double> e : entries) {
            if (e.getValue() < 0.01) break;
            out.append(' ')
                .append(e.getKey())
                .append(' ')
                .append(Math.round(100 * e.getValue()))
                .append('%');
        }
        say(player, out.toString());
    }

    /** Nearest region holding the style as its base or an overlay, or with pure, as its base with no overlay. */
    private static void styleRegion(EntityPlayerMP player, TerrainSampler sampler, String name, boolean pure) {
        RegionPicker picker = sampler.picker();
        Style target = name == null ? null : picker.byName(name);
        if (target == null) {
            say(player, (name == null ? "Name a style" : "Unknown style " + name) + "; /etj end styles lists them");
            return;
        }
        if (pure && !target.isBase()) {
            say(player, target + " is only ever laid over a base; use /etj end mix " + target);
            return;
        }
        long seed = sampler.seed();
        nearestRegion(
            player,
            sampler,
            (cx, cz) -> pure ? picker.isPure(seed, cx, cz, target) : picker.contains(seed, cx, cz, target));
    }

    /** Nearest region with at least one overlay that holds every named style (up to three) as its base or overlays. */
    private static void mixRegion(EntityPlayerMP player, TerrainSampler sampler, String[] names) {
        RegionPicker picker = sampler.picker();
        if (names.length > RegionPicker.MAX_OVERLAYS + 1) {
            say(player, "A region holds at most " + (RegionPicker.MAX_OVERLAYS + 1) + " styles");
            return;
        }
        List<Style> wanted = new ArrayList<>();
        for (String name : names) {
            Style style = picker.byName(name);
            if (style == null) {
                say(player, "Unknown style " + name + "; /etj end styles lists them");
                return;
            }
            wanted.add(style);
        }
        long seed = sampler.seed();
        nearestRegion(
            player,
            sampler,
            (cx, cz) -> !picker.overlays(seed, cx, cz)
                .isEmpty() && picker.containsAll(seed, cx, cz, wanted));
    }

    private interface CellTest {

        boolean test(int cx, int cz);
    }

    /** Teleports to the centre of the nearest region cell that passes, clear of the central island. */
    private static void nearestRegion(EntityPlayerMP player, TerrainSampler sampler, CellTest test) {
        RegionPicker picker = sampler.picker();
        long seed = sampler.seed();
        double[] hit = EndSearch.nearestInCells(RegionMap.REGION, player.posX, player.posZ, (cx, cz) -> {
            if (!test.test(cx, cz)) return null;
            double[] c = RegionMap.cellCentre(seed, cx, cz);
            return Math.hypot(c[0], c[1]) < 1100 ? null : c;
        });
        if (hit == null) {
            say(player, "No such region within " + EndSearch.RADIUS + " blocks");
            return;
        }
        int cx = (int) Math.floor(hit[0] / RegionMap.REGION), cz = (int) Math.floor(hit[1] / RegionMap.REGION);
        teleport(player, hit[0], 140, hit[1], picker.base(seed, cx, cz) + " " + picker.overlays(seed, cx, cz));
    }

    /**
     * Moves the player onto the block they look at, up to range blocks away, or to the point range blocks ahead when
     * nothing is in the way; never below Y 0 or above Y 270.
     */
    private static void look(EntityPlayerMP player, int range) {
        double[] d = EtjArgs.lookDirection(player.rotationYaw, player.rotationPitch);
        double[] eye = { player.posX, player.posY + player.getEyeHeight(), player.posZ };
        double[] far = EtjArgs.lookTarget(eye, d, range);
        World world = player.worldObj;
        MovingObjectPosition hit = null;
        // rayTraceBlocks stops after 200 block boundaries, about 115 blocks on a diagonal, so trace in pieces.
        for (double[] p : EtjArgs.lookSegments(eye, d, range, LOOK_STEP)) {
            hit = world
                .rayTraceBlocks(Vec3.createVectorHelper(p[0], p[1], p[2]), Vec3.createVectorHelper(p[3], p[4], p[5]));
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) break;
        }
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            teleport(player, far[0], far[1], far[2], "look (nothing in the way)");
            return;
        }
        // Stand on the first two free blocks above the hit, so a wall hit still lands on top.
        int y = hit.blockY + 1;
        while (y < EtjArgs.MAX_Y
            && !(world.isAirBlock(hit.blockX, y, hit.blockZ) && world.isAirBlock(hit.blockX, y + 1, hit.blockZ))) y++;
        teleport(player, hit.blockX + 0.5, Math.min(EtjArgs.MAX_Y, y), hit.blockZ + 0.5, "look");
    }

    private static void mountain(EntityPlayerMP player, TerrainSampler sampler, int minTop) {
        long seed = sampler.seed();
        double[] hit = EndSearch.nearestInCells(Mountains.CELL, player.posX, player.posZ, (cx, cz) -> {
            Mountains.Mountain m = Mountains.inCell(seed, cx, cz);
            if (m == null) return null;
            double top = sampler.bareColumn(m.x, m.z, true).top;
            return top < minTop ? null : new double[] { m.x, m.z, top, m.peak };
        });
        if (hit == null) say(player, "No mountain reaching Y " + minTop + " within " + EndSearch.RADIUS + " blocks");
        else teleport(player, hit[0], hit[2] + 4, hit[1], "mountain peak=" + (int) hit[3] + " top=" + (int) hit[2]);
    }

    private static void lake(EntityPlayerMP player, TerrainSampler sampler) {
        long seed = sampler.seed();
        double[] hit = EndSearch.nearestInCells(Lakes.CELL, player.posX, player.posZ, (cx, cz) -> {
            Lakes.Lake l = Lakes.inCell(seed, cx, cz, sampler.lakeProbe());
            return l == null ? null : new double[] { l.x, l.z, l.waterLevel + 3 };
        });
        if (hit == null) say(player, "No lake within " + EndSearch.RADIUS + " blocks");
        else teleport(player, hit[0], hit[2], hit[1], "lake");
    }

    private static void hole(EntityPlayerMP player, TerrainSampler sampler) {
        long seed = sampler.seed();
        double[] hit = EndSearch.nearestInCells(Holes.CELL, player.posX, player.posZ, (cx, cz) -> {
            Holes.Hole h = Holes.inCell(seed, cx, cz, sampler.holeProbe());
            return h == null ? null : new double[] { h.x, h.z };
        });
        if (hit == null) say(player, "No hole within " + EndSearch.RADIUS + " blocks");
        // Above the shaft, high enough to look down into it.
        else teleport(player, hit[0], sampler.bareColumn(hit[0], hit[1], true).top + 30, hit[1], "hole");
    }

    private static void valley(EntityPlayerMP player, TerrainSampler sampler) {
        long seed = sampler.seed();
        double[] hit = EndSearch.nearestPoint(
            player.posX,
            player.posZ,
            (x, z) -> Valleys.depth(seed, x, z) >= 10 && sampler.land(x, z) >= 30);
        if (hit == null) say(player, "No valley within " + EndSearch.RADIUS + " blocks");
        else teleport(player, hit[0], sampler.bareColumn(hit[0], hit[1], true).top + 20, hit[1], "valley");
    }

    private static void zone(EntityPlayerMP player, TerrainSampler sampler, String name) {
        Zone wanted = null;
        for (Zone z : Zone.values()) if (z.name()
            .equalsIgnoreCase(String.valueOf(name))) wanted = z;
        if (wanted == null) {
            say(player, "Zones: " + Arrays.toString(Zone.values()));
            return;
        }
        long seed = sampler.seed();
        Zone target = wanted;
        WorldServer end = (WorldServer) player.worldObj;
        IChunkProvider generator = end.theChunkProviderServer.currentChunkProvider;
        double[] hit = EndSearch.nearestInCells(ZoneIslands.CELL, player.posX, player.posZ, (cx, cz) -> {
            ZoneIslands.Island island = ZoneIslands.inCell(seed, cx, cz, sampler.landProbe());
            if (island == null || ZoneIslands.zoneOf(seed, island) != target) return null;
            List<Area> reserved = Reservations
                .forChunk(generator, end, seed, (int) Math.floor(island.x) >> 4, (int) Math.floor(island.z) >> 4);
            if (Reservations.overlaps(reserved, island.x, island.z, island.radius * 1.2)) return null;
            return new double[] { island.x, island.z, island.y + island.up + 3 };
        });
        if (hit == null) say(player, "No " + target + " island within " + EndSearch.RADIUS + " blocks");
        else teleport(player, hit[0], hit[2], hit[1], target + " island");
    }

    private static void hee(EntityPlayerMP player, long seed) {
        WorldServer end = (WorldServer) player.worldObj;
        IChunkProvider generator = end.theChunkProviderServer.currentChunkProvider;
        double[] hit = nearestArea(
            player,
            chunks -> Reservations.near(generator, end, chunkOf(player.posX), chunkOf(player.posZ), chunks),
            "hee");
        if (hit == null) say(player, "No HEE island within " + EndSearch.RADIUS + " blocks");
        else teleport(player, hit[0], 100, hit[1], "HEE island");
    }

    private static void destitute(EntityPlayerMP player) {
        // FML seeds world generators from World#getSeed of the End, which HEE offsets by the dragon kill count.
        long endSeed = player.worldObj.getSeed();
        double[] hit = nearestArea(
            player,
            chunks -> PredictedIslands.near(endSeed, "destitute", chunkOf(player.posX), chunkOf(player.posZ), chunks),
            "destitute");
        if (hit == null) say(player, "No destitute island within " + EndSearch.RADIUS + " blocks");
        else teleport(player, hit[0], 40, hit[1], "destitute island (predicted)");
    }

    interface AreaSource {

        List<Area> within(int chunkRadius);
    }

    /** Grows a square window; a hit no farther than the window's half-width is the true nearest. */
    private static double[] nearestArea(EntityPlayerMP player, AreaSource source, String label) {
        double[] best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int chunks = 64; chunks <= EndSearch.RADIUS / 16; chunks *= 2) {
            for (Area area : source.within(chunks)) {
                if (!area.label.equals(label)) continue;
                double d = Math.hypot(area.x - player.posX, area.z - player.posZ);
                if (d < bestDistance && d <= EndSearch.RADIUS) {
                    bestDistance = d;
                    best = new double[] { area.x, area.z };
                }
            }
            if (bestDistance <= chunks * 16) break;
        }
        return best;
    }

    private static int chunkOf(double block) {
        return (int) Math.floor(block) >> 4;
    }

    private static void teleport(EntityPlayerMP player, double x, double y, double z, String label) {
        player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, 0);
        say(player, label + " at " + (int) x + " " + (int) y + " " + (int) z);
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
