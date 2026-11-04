package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

import chlorine.etjourney.core.registry.RegistryNames;

/** A glowing lantern smaller than a block, solid to walk into, that drops itself and needs no support. */
final class EndLanternBlock extends Block {

    EndLanternBlock(LargePlantBlockTable.Entry entry) {
        super(Material.glass);
        setHardness(0.3F);
        setStepSound(soundTypeGlass);
        setLightLevel(entry.light / 15.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
        setBlockBounds(3 / 16F, 2 / 16F, 3 / 16F, 13 / 16F, 14 / 16F, 13 / 16F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
