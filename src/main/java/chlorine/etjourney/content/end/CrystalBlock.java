package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import chlorine.etjourney.core.registry.RegistryNames;

/** A crystal block: drops nothing and cannot be silk-harvested, so it is only ever seen where it grew. */
class CrystalBlock extends Block {

    CrystalBlock(LargePlantBlockTable.Entry entry) {
        super(Material.glass);
        setHardness(1.5F);
        setStepSound(soundTypeGlass);
        setLightLevel(entry.light / 15.0F);
        setCreativeTab(CreativeTabs.tabBlock);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }
}
