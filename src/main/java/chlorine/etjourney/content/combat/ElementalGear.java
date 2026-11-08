package chlorine.etjourney.content.combat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraftforge.common.util.EnumHelper;

import chlorine.etjourney.core.damage.model.Element;
import cpw.mods.fml.common.registry.GameRegistry;

/** One trial sword and armor set per element, drawn as tinted vanilla iron; no recipes yet. */
public final class ElementalGear {

    /** Each element's tint; order alone grants death immunity, chaos alone pierces it. */
    enum Kind {

        FIRE(Elements.FIRE, 0xFF5A28),
        ICE(Elements.ICE, 0x8FD8FF),
        WIND(Elements.WIND, 0x8CF0A0),
        ELECTRIC(Elements.ELECTRIC, 0xFFE83A),
        CHAOS(Elements.CHAOS, 0x7A2FB0),
        ORDER(Elements.ORDER, 0xE0C060),
        LIGHT(Elements.LIGHT, 0xFFFFE0);

        final Element element;
        final int color;

        Kind(Element element, int color) {
            this.element = element;
            this.color = color;
        }

        boolean piercing() {
            return this == CHAOS;
        }

        boolean immune() {
            return this == ORDER;
        }
    }

    private static final String[] PIECES = { "helmet", "chestplate", "leggings", "boots" };

    private ElementalGear() {}

    public static void preInit() {
        Item.ToolMaterial tool = EnumHelper.addToolMaterial("ETJ_ELEMENTAL", 2, 1000, 6f, 2f, 14);
        ItemArmor.ArmorMaterial armor = EnumHelper.addArmorMaterial("ETJ_ELEMENTAL", 25, new int[] { 3, 7, 5, 2 }, 12);
        for (Kind kind : Kind.values()) {
            String prefix = kind.element.name() + "_";
            add(new ElementalSword(prefix + "sword", kind, tool), prefix + "sword");
            for (int i = 0; i < PIECES.length; i++)
                add(ElementalArmorItem.of(prefix + PIECES[i], kind, armor, i), prefix + PIECES[i]);
        }
    }

    private static void add(Item item, String name) {
        GameRegistry.registerItem(item, name);
    }

}
