package chlorine.etjourney.content.end;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;

import chlorine.etjourney.core.registry.RegistryNames;

/** The hydrofluoric acid of the End's pools and falls; it hurts creatures, splits Coltan and dissolves other items. */
final class AcidBlock extends BlockFluidClassic {

    private static final DamageSource DAMAGE = new DamageSource(RegistryNames.unlocalized(HydrofluoricAcid.NAME));
    /** What vanilla waits before a dropped item can be picked up. */
    private static final int PICKUP_DELAY = 10;

    private IIcon still, flowing;

    AcidBlock(Fluid fluid) {
        // Water, so that creatures swim in it and the vanilla liquid rules hold.
        super(fluid, Material.water);
        setHardness(100.0F);
        setLightOpacity(3);
        setBlockName(RegistryNames.unlocalized(HydrofluoricAcid.NAME));
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        still = register.registerIcon(RegistryNames.texture(HydrofluoricAcid.NAME + "_still"));
        flowing = register.registerIcon(RegistryNames.texture(HydrofluoricAcid.NAME + "_flow"));
        if (getFluid().getBlock() == this) getFluid().setIcons(still, flowing);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return side == 0 || side == 1 ? still : flowing;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote) return;
        if (entity instanceof EntityItem) {
            react(world, (EntityItem) entity);
        } else if (entity instanceof EntityLivingBase && HydrofluoricAcid.hurtsOn(world.getTotalWorldTime())) {
            // Hurt immunity keeps the other acid blocks the creature touches this tick from hitting again.
            entity.attackEntityFrom(DAMAGE, HydrofluoricAcid.DAMAGE);
        }
    }

    private static void react(World world, EntityItem entity) {
        if (entity.isDead) return;
        ItemStack stack = entity.getEntityItem();
        String id = Item.itemRegistry.getNameForObject(stack.getItem());
        HydrofluoricAcid.Outcome outcome = HydrofluoricAcid.outcome(id);
        if (outcome == HydrofluoricAcid.Outcome.KEEP) return;
        for (String product : HydrofluoricAcid.products(id)) {
            Item item = (Item) Item.itemRegistry.getObject(product);
            EntityItem split = new EntityItem(
                world,
                entity.posX,
                entity.posY,
                entity.posZ,
                new ItemStack(item, stack.stackSize));
            split.delayBeforeCanPickup = PICKUP_DELAY;
            world.spawnEntityInWorld(split);
        }
        entity.setDead();
        world.playSoundAtEntity(
            entity,
            "random.fizz",
            0.5F,
            2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
    }
}
