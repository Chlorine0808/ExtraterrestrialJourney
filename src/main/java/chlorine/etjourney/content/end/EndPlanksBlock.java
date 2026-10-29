package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

import chlorine.etjourney.core.registry.RegistryNames;

/** Planks sawn from a large plant's trunk; wood material, so furnaces burn them as fuel. */
final class EndPlanksBlock extends Block {

    EndPlanksBlock(String name) {
        super(Material.wood);
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setCreativeTab(CreativeTabs.tabBlock);
        setBlockName(RegistryNames.unlocalized(name));
        setBlockTextureName(RegistryNames.texture(name));
    }
}
