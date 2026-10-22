package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

import chlorine.etjourney.core.registry.RegistryNames;

/** An End ore or gem; it drops itself until the mod has items to drop. */
final class OreBlock extends Block {

    OreBlock(OreBlockTable.Entry entry) {
        super(Material.rock);
        setHardness(entry.hardness);
        setResistance(15.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", entry.harvestLevel);
        setCreativeTab(CreativeTabs.tabBlock);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }
}
