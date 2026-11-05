package chlorine.etjourney.world.end.building;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.building.BlockRef;
import chlorine.etjourney.world.building.BlockTarget;
import chlorine.etjourney.world.building.Rotation;
import chlorine.etjourney.world.building.VanillaMetaRotations;

/** Writes a building into a world, turning facing blocks and filling chests from their loot category. */
public final class WorldTarget implements BlockTarget {

    private static final Map<String, Block> BLOCKS = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final World world;

    public WorldTarget(World world) {
        this.world = world;
    }

    @Override
    public void set(int x, int y, int z, BlockRef ref, Rotation rotation) {
        Block block = block(ref.name);
        if (block == null) return;
        int meta = VanillaMetaRotations.forBlock(block)
            .rotate(ref.meta, rotation);
        world.setBlock(x, y, z, block, meta, 2);
        if (ref.loot != null) fill(x, y, z, ref.loot);
    }

    @Override
    public void setAir(int x, int y, int z) {
        if (!world.isAirBlock(x, y, z)) world.setBlock(x, y, z, Blocks.air, 0, 2);
    }

    @Override
    public boolean replaceable(int x, int y, int z) {
        return world.getBlock(x, y, z)
            .isReplaceable(world, x, y, z);
    }

    private void fill(int x, int y, int z, String loot) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IInventory)) return;
        // From the position, so a chest gets the same loot whichever window writes it.
        Random random = new Random(world.getSeed() ^ (x * 341873128712L + y * 2147483647L + z * 132897987541L));
        WeightedRandomChestContent.generateChestContents(
            random,
            ChestGenHooks.getItems(loot, random),
            (IInventory) tile,
            ChestGenHooks.getCount(loot, random));
    }

    /** The block of a registry name, or null with one warning per name when no mod registers it. */
    private static Block block(String name) {
        Block block = BLOCKS.get(name);
        if (block != null) return block;
        if (!Block.blockRegistry.containsKey(name)) {
            if (WARNED.add(name)) ModLog.LOG.warn("Building block {} is not registered; it is left out", name);
            return null;
        }
        block = (Block) Block.blockRegistry.getObject(name);
        BLOCKS.put(name, block);
        return block;
    }
}
