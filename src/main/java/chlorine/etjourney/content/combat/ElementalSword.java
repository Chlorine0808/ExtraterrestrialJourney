package chlorine.etjourney.content.combat;

import java.util.List;
import java.util.Map;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.ElementalDamages;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.registry.RegistryNames;

/** A sword whose hit is physical, mostly its element and partly neutral; the chaos one pierces. */
final class ElementalSword extends ItemSword {

    static final float ELEMENT = 5, NEUTRAL = 3;

    private final ElementalGear.Kind kind;
    private final ElementalDamage damage;

    ElementalSword(String name, ElementalGear.Kind kind, ToolMaterial material) {
        super(material);
        this.kind = kind;
        ElementalDamage.Builder b = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(kind.element, ELEMENT)
            .add(ElementRegistry.NEUTRAL, NEUTRAL);
        if (kind.piercing()) b.piercing();
        this.damage = b.build();
        setUnlocalizedName(RegistryNames.unlocalized(name));
        setTextureName("iron_sword");
        setCreativeTab(CreativeTabs.tabCombat);
    }

    /** Replaces the vanilla attack on living targets; anything else, such as an item frame, stays vanilla. */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!(entity instanceof EntityLivingBase) || !entity.canAttackWithItem()) return false;
        if (player.worldObj.isRemote) return true;
        EntityLivingBase target = (EntityLivingBase) entity;
        // Piercing skips the hurt cooldown, so keep the vanilla pace by hand.
        if (damage.isPiercing() && target.hurtResistantTime > target.maxHurtResistantTime / 2) return true;
        if (ElementalDamages.deal(target, new ElementalDamageSource(damage, player, player))) {
            stack.hitEntity(target, player);
            if (stack.stackSize <= 0) player.destroyCurrentEquippedItem();
            player.addExhaustion(0.3f);
        }
        return true;
    }

    /** No vanilla attack bonus: the tooltip lists the elemental parts instead. */
    @Override
    @SuppressWarnings("rawtypes")
    public Multimap getItemAttributeModifiers() {
        return HashMultimap.create();
    }

    @Override
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return kind.color;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        for (Map.Entry<Element, Float> part : damage.parts()
            .entrySet()) {
            String element = StatCollector.translateToLocal(
                "etjourney.element." + part.getKey()
                    .name());
            lines.add(
                EnumChatFormatting.BLUE
                    + StatCollector.translateToLocalFormatted("etjourney.tooltip.damage", element, part.getValue()));
        }
        if (damage.isPiercing())
            lines.add(EnumChatFormatting.DARK_PURPLE + StatCollector.translateToLocal("etjourney.tooltip.piercing"));
    }
}
