package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import chlorine.etjourney.core.registry.RegistryNames;
import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.feature.EndBiome;

/** A biome plant: drawn crossed like grass, standing on any biome surface or on vanilla grass and dirt. */
final class PlantBlock extends BlockBush {

    private final boolean replaceable;

    PlantBlock(PlantBlockTable.Entry entry) {
        super(Material.plants);
        this.replaceable = entry.replaceable;
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

    private static boolean isBiomeSurface(Block block) {
        for (EndBiome biome : EndBiome.values()) {
            if (EndPalette.isBound(biome.top) && EndPalette.block(biome.top) == block) return true;
        }
        return false;
    }
}
