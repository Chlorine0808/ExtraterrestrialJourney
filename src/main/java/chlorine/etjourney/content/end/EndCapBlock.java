package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import chlorine.etjourney.core.registry.RegistryNames;

/** A soft full block of a fungus or amber growth (cap, gill, seed, stem) that drops nothing when broken. */
final class EndCapBlock extends Block {

    EndCapBlock(LargePlantBlockTable.Entry entry) {
        super(Material.wood);
        setHardness(entry.type == LargePlantBlockTable.Type.STEM ? 0.4F : 0.2F);
        setStepSound(soundTypeWood);
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
}
