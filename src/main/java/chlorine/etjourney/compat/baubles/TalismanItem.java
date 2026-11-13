package chlorine.etjourney.compat.baubles;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import baubles.api.BaubleType;
import baubles.api.expanded.BaubleItemHelper;
import baubles.api.expanded.IBaubleExpanded;
import chlorine.etjourney.content.accessory.Talisman;
import chlorine.etjourney.core.accessory.Accessory;
import chlorine.etjourney.core.accessory.AccessoryModifiers;
import chlorine.etjourney.core.combat.CombatHook;
import chlorine.etjourney.core.damage.ElementalAccessory;
import chlorine.etjourney.core.damage.model.Boosts;
import chlorine.etjourney.core.damage.model.Resistance;
import chlorine.etjourney.core.registry.RegistryNames;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A talisman as a Baubles item that fits only ETJ's talisman slots; its effects live in {@link Talisman}. */
final class TalismanItem extends Item implements IBaubleExpanded, Accessory, ElementalAccessory {

    private final Talisman talisman;

    TalismanItem(Talisman talisman) {
        this.talisman = talisman;
        setUnlocalizedName(RegistryNames.unlocalized(talisman.name));
        setTextureName(RegistryNames.texture(talisman.name));
        setCreativeTab(CreativeTabs.tabCombat);
        setMaxStackSize(1);
        if (talisman.lifetime > 0) {
            setMaxDamage(talisman.lifetime);
            setNoRepair();
        }
    }

    @Override
    public String[] getBaubleTypes(ItemStack stack) {
        return new String[] { BaublesCompat.TALISMAN_TYPE };
    }

    /** Only mods that know the old API read this; Baubles Expanded uses the types above. */
    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    // ETJ's AccessoryTicker runs the effects; Baubles skips these hooks on load and on death drops.
    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {}

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase wearer) {
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return BaubleItemHelper.onBaubleRightClick(stack, world, player);
    }

    @Override
    public void addModifiers(ItemStack stack, EntityLivingBase wearer, AccessoryModifiers out) {
        talisman.addModifiers(out);
    }

    @Override
    public void addResistance(ItemStack stack, EntityLivingBase wearer, Resistance.Builder out) {
        talisman.addResistance(out);
    }

    @Override
    public void addBoosts(ItemStack stack, EntityLivingBase wearer, Boosts.Builder out) {
        talisman.addBoosts(wearer, out);
    }

    @Override
    public void addHooks(ItemStack stack, EntityLivingBase wearer, List<CombatHook> out) {
        talisman.addHooks(out);
    }

    @Override
    public ItemStack tickWorn(ItemStack stack, EntityLivingBase wearer) {
        return talisman.wear(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        talisman.addInformation(stack, lines);
        BaubleItemHelper.addSlotInformation(lines, getBaubleTypes(stack));
    }
}
