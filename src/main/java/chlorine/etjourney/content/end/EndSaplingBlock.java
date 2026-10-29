package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.World;

import chlorine.etjourney.core.registry.RegistryNames;
import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.plant.GroveShrub;
import chlorine.etjourney.world.end.feature.plant.GroveTree;
import chlorine.etjourney.world.end.feature.plant.Grower;
import chlorine.etjourney.world.end.feature.plant.LargeFlora;
import chlorine.etjourney.world.end.feature.plant.TwistedTree;

/**
 * A sapling of one biome: stands only on that biome's surface and grows as vanilla saplings do (meta bit 8 = stage).
 */
final class EndSaplingBlock extends BlockBush implements IGrowable {

    private static final int STAGE = 8;
    private static final int GROW_ODDS = 7;
    private static final int GROVE_TREE_PERCENT = 75;
    private static final int MAX_HEADROOM = 64;

    private final EndBiome biome;

    EndSaplingBlock(LargePlantBlockTable.Sapling entry) {
        super(Material.plants);
        this.biome = entry.biome;
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    // The End has no sky light and the soil is the biome surface, so the vanilla soil and light checks do not apply.
    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return LargeFlora.growsOn(biome, EndPalette.biomeOfTop(world.getBlock(x, y - 1, z)));
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        super.updateTick(world, x, y, z, random);
        if (random.nextInt(GROW_ODDS) == 0 && world.getBlock(x, y, z) == this) advance(world, random, x, y, z);
    }

    private void advance(World world, Random random, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & STAGE) == 0) {
            world.setBlockMetadataWithNotify(x, y, z, meta | STAGE, 4);
        } else {
            grow(world, random, x, y, z);
        }
    }

    /** Replaces the sapling with its plant, or puts the sapling back when the plant does not fit. */
    private void grow(World world, Random random, int x, int y, int z) {
        Grower grower = pickGrower(random);
        int reach = grower.radius();
        // Never pull in chunks that are not loaded: the crown would spill into them.
        if (!world.checkChunksExist(x - reach, y, z - reach, x + reach, y, z + reach)) return;
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockToAir(x, y, z);
        int headroom = 0;
        while (headroom < MAX_HEADROOM && y + headroom < 255 && world.isAirBlock(x, y + headroom, z)) headroom++;
        if (!grower.grow(random, x, y, z, headroom, new WorldPlantSink(world))) {
            world.setBlock(x, y, z, this, meta, 3);
        }
    }

    private Grower pickGrower(Random random) {
        if (biome == EndBiome.GROVE) {
            return random.nextInt(100) < GROVE_TREE_PERCENT ? GroveTree.INSTANCE : GroveShrub.INSTANCE;
        }
        return TwistedTree.INSTANCE;
    }

    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean isRemote) {
        return true;
    }

    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return world.rand.nextFloat() < 0.45F;
    }

    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        advance(world, random, x, y, z);
    }
}
