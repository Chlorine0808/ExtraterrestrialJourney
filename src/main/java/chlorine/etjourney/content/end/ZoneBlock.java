package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.IIcon;

import chlorine.etjourney.core.registry.RegistryNames;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A zone's rock, or its surface drawn like grass: own top and side, the rock's texture underneath. */
final class ZoneBlock extends Block {

    private final ZoneBlockTable.Entry entry;
    @SideOnly(Side.CLIENT)
    private IIcon top, side, bottom;

    ZoneBlock(ZoneBlockTable.Entry entry) {
        super(Material.rock);
        this.entry = entry;
        // Same as end stone.
        setHardness(3.0F);
        setResistance(15.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 0);
        setCreativeTab(CreativeTabs.tabBlock);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        if (entry.bottom == null) {
            blockIcon = register.registerIcon(getTextureName());
            top = side = bottom = blockIcon;
            return;
        }
        top = register.registerIcon(getTextureName() + "_top");
        side = register.registerIcon(getTextureName() + "_side");
        bottom = register.registerIcon(RegistryNames.texture(entry.bottom));
        blockIcon = side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int meta) {
        return face == 0 ? bottom : face == 1 ? top : side;
    }
}
