package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import chlorine.etjourney.core.registry.RegistryNames;
import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.plant.GiantMushroom;
import chlorine.etjourney.world.end.feature.plant.LargeFlora;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A biome plant: drawn crossed like grass, standing on any biome surface or on vanilla grass and dirt. */
final class PlantBlock extends BlockBush implements IGrowable {

    private static final float MUSHROOM_CHANCE = 0.4F;

    private final boolean replaceable;
    private final boolean glowcap;

    PlantBlock(PlantBlockTable.Entry entry) {
        super(Material.plants);
        this.replaceable = entry.replaceable;
        this.glowcap = entry.kind == EndBlock.GLOWCAP;
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightLevel(entry.light / 15.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    // The End has no sky light, and vanilla soil checks only know grass, dirt and farmland.
    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return isBiomeSurface(world.getBlock(x, y - 1, z)) || super.canBlockStay(world, x, y, z);
    }

    @Override
    protected boolean canPlaceBlockOn(Block block) {
        return isBiomeSurface(block) || super.canPlaceBlockOn(block);
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return replaceable;
    }

    /** Bone meal works on the glowcap only, and only on mycelia ground, like a vanilla mushroom. */
    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean isRemote) {
        return glowcap && LargeFlora.growsOn(EndBiome.MYCELIA, EndPalette.biomeOfTop(world.getBlock(x, y - 1, z)));
    }

    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return random.nextFloat() < MUSHROOM_CHANCE;
    }

    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        WorldPlantSink.growInPlace(world, random, x, y, z, GiantMushroom.SMALL);
    }

    private static boolean isBiomeSurface(Block block) {
        return EndPalette.biomeOfTop(block) != null;
    }
}
