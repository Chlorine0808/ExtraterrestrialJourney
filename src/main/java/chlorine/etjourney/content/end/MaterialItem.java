package chlorine.etjourney.content.end;

import net.minecraft.item.Item;

import chlorine.etjourney.content.ModCreativeTab;
import chlorine.etjourney.core.registry.RegistryNames;

/** A plain material such as a raw ore or a gem, named and drawn from its registry name. */
final class MaterialItem extends Item {

    MaterialItem(String name) {
        setUnlocalizedName(RegistryNames.unlocalized(name));
        setTextureName(RegistryNames.texture(name));
        setCreativeTab(ModCreativeTab.TAB);
    }
}
