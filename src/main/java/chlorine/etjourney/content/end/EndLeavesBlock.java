package chlorine.etjourney.content.end;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import chlorine.etjourney.core.registry.RegistryNames;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Leaves of a large plant: vanilla decay rules, but a leaf may be up to 8 steps from wood (vanilla allows 4) because
 * tree crowns reach 7. The sheets are untinted and opaque; the block turns opaque or not with the vanilla leaves.
 */
final class EndLeavesBlock extends BlockLeaves {

    private static final int DECAY_RANGE = 8;
    private static final int PLAYER_PLACED = 4;
    private static final int CHECK_DECAY = 8;
    private static final int SAPLING_CHANCE = 20;

    private final String name;
    /** Dropped with chance 1 in {@value #SAPLING_CHANCE}; null until the sapling is registered. */
    private Item sapling;

    EndLeavesBlock(LargePlantBlockTable.Entry entry) {
        this.name = entry.name;
        setLightLevel(entry.light / 15.0F);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    void setSapling(Item sapling) {
        this.sapling = sapling;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & CHECK_DECAY) == 0 || (meta & PLAYER_PLACED) != 0) return;
        int reach = DECAY_RANGE + 1;
        // An unloaded neighbourhood cannot be judged; the flag stays and a later tick tries again.
        if (!world.checkChunksExist(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach)) return;
        if (isHeldByWood(world, x, y, z)) {
            world.setBlockMetadataWithNotify(x, y, z, meta & ~CHECK_DECAY, 4);
        } else {
            dropBlockAsItem(world, x, y, z, meta, 0);
            world.setBlockToAir(x, y, z);
        }
    }

    /** Whether wood lies within DECAY_RANGE steps, passing only through leaves. */
    private static boolean isHeldByWood(World world, int x, int y, int z) {
        int side = 2 * DECAY_RANGE + 1;
        boolean[] seen = new boolean[side * side * side];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { x, y, z, 0 });
        seen[(DECAY_RANGE * side + DECAY_RANGE) * side + DECAY_RANGE] = true;
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int face = 0; face < 6; face++) {
                int nx = at[0] + (face == 4 ? -1 : face == 5 ? 1 : 0);
                int ny = at[1] + (face == 0 ? -1 : face == 1 ? 1 : 0);
                int nz = at[2] + (face == 2 ? -1 : face == 3 ? 1 : 0);
                int index = ((ny - y + DECAY_RANGE) * side + (nx - x + DECAY_RANGE)) * side + (nz - z + DECAY_RANGE);
                if (seen[index]) continue;
                seen[index] = true;
                Block block = world.getBlock(nx, ny, nz);
                if (block.canSustainLeaves(world, nx, ny, nz)) return true;
                if (at[3] + 1 < DECAY_RANGE && block.isLeaves(world, nx, ny, nz)) {
                    queue.add(new int[] { nx, ny, nz, at[3] + 1 });
                }
            }
        }
        return false;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        if (sapling != null && world.rand.nextInt(SAPLING_CHANCE) == 0) drops.add(new ItemStack(sapling));
        return drops;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return sapling;
    }

    @Override
    public boolean isOpaqueCube() {
        return Blocks.leaves.isOpaqueCube();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        // The vanilla leaves get their graphics level from the options screen; follow it.
        setGraphicsLevel(!Blocks.leaves.isOpaqueCube());
        return super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int meta) {
        return blockIcon;
    }

    @Override
    public String[] func_150125_e() {
        return new String[] { name };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return 0xFFFFFF;
    }

    /** Leaves a player sets down carry the placed bit, so they do not decay with the tree. */
    public static final class Placed extends ItemBlock {

        // Forge builds the item by reflection, so the class and its constructor must be public.
        public Placed(Block block) {
            super(block);
        }

        @Override
        public int getMetadata(int damage) {
            return damage | PLAYER_PLACED;
        }
    }
}
