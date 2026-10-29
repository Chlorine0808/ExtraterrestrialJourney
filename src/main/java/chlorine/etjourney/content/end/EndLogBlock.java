package chlorine.etjourney.content.end;

import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import chlorine.etjourney.core.registry.RegistryNames;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A trunk, root or stem: a vanilla log (so leaves hold on to it) with its own side and top sheets. */
final class EndLogBlock extends BlockLog {

    @SideOnly(Side.CLIENT)
    private IIcon side, top;

    EndLogBlock(LargePlantBlockTable.Entry entry) {
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        side = register.registerIcon(getTextureName() + "_side");
        top = register.registerIcon(getTextureName() + "_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected IIcon getSideIcon(int variant) {
        return side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected IIcon getTopIcon(int variant) {
        return top;
    }
}
