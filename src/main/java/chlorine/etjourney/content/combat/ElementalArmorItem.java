package chlorine.etjourney.content.combat;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import chlorine.etjourney.content.ModCreativeTab;
import chlorine.etjourney.core.damage.DeathImmunity;
import chlorine.etjourney.core.damage.ElementalArmor;
import chlorine.etjourney.core.damage.model.Resistance;
import chlorine.etjourney.core.registry.RegistryNames;

/** One piece resisting its own element; a full set takes about half and 2 less. Drawn as tinted vanilla iron. */
class ElementalArmorItem extends ItemArmor implements ElementalArmor {

    static final float FACTOR = 0.85f, FLAT = 0.5f;

    private static final String[] ICONS = { "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots" };

    /** Drawn over the tinted pass; blank so the iron keeps its tint. */
    private static final String BLANK_OVERLAY = "etjourney:textures/models/armor/blank_overlay.png";

    private final ElementalGear.Kind kind;

    ElementalArmorItem(String name, ElementalGear.Kind kind, ArmorMaterial material, int type) {
        super(material, 2, type);
        this.kind = kind;
        setUnlocalizedName(RegistryNames.unlocalized(name));
        setTextureName(ICONS[type]);
        setCreativeTab(ModCreativeTab.TAB);
    }

    static ElementalArmorItem of(String name, ElementalGear.Kind kind, ArmorMaterial material, int type) {
        return kind.immune() ? new Immune(name, kind, material, type)
            : new ElementalArmorItem(name, kind, material, type);
    }

    @Override
    public void addResistance(ItemStack stack, EntityLivingBase wearer, Resistance.Builder out) {
        out.multiply(kind.element, FACTOR)
            .subtract(kind.element, FLAT);
    }

    /** Non-leather armor stays untinted unless this returns a color; the renderer and the icon both read it. */
    @Override
    public int getColor(ItemStack stack) {
        return kind.color;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
        if ("overlay".equals(type)) return BLANK_OVERLAY;
        return "minecraft:textures/models/armor/iron_layer_" + (slot == 2 ? 2 : 1) + ".png";
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        String element = StatCollector.translateToLocal("etjourney.element." + kind.element.name());
        lines.add(
            EnumChatFormatting.BLUE
                + StatCollector.translateToLocalFormatted("etjourney.tooltip.resist", element, FACTOR, FLAT));
        if (kind.immune())
            lines.add(EnumChatFormatting.GOLD + StatCollector.translateToLocal("etjourney.tooltip.immune_set"));
    }

    private static final class Immune extends ElementalArmorItem implements DeathImmunity.Armor {

        Immune(String name, ElementalGear.Kind kind, ArmorMaterial material, int type) {
            super(name, kind, material, type);
        }
    }
}
