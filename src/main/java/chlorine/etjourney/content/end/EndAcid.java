package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import chlorine.etjourney.core.registry.RegistryNames;
import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.modifier.EndBlock;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers the hydrofluoric acid in place of the End terrain's water, and the Tantalum bucket that alone takes it
 * up.
 */
public final class EndAcid {

    /** Hydrofluoric acid is a little denser than water. */
    private static final int DENSITY = 1150;

    private final Block acid;
    private final Item emptyBucket, fullBucket;

    private EndAcid(Block acid, Item emptyBucket, Item fullBucket) {
        this.acid = acid;
        this.emptyBucket = emptyBucket;
        this.fullBucket = fullBucket;
    }

    public static void preInit() {
        FluidRegistry.registerFluid(
            new Fluid(HydrofluoricAcid.NAME).setDensity(DENSITY)
                .setUnlocalizedName(RegistryNames.unlocalized(HydrofluoricAcid.NAME)));
        // Another mod may have registered a fluid by this name first; share it.
        Fluid fluid = FluidRegistry.getFluid(HydrofluoricAcid.NAME);
        AcidBlock acid = new AcidBlock(fluid);
        GameRegistry.registerBlock(acid, HydrofluoricAcid.NAME);
        EndPalette.bind(EndBlock.WATER, acid, 0);

        TantalumBucket empty = new TantalumBucket(Blocks.air, HydrofluoricAcid.EMPTY_BUCKET);
        // Vanilla stacks its empty bucket to 16.
        empty.setMaxStackSize(16);
        TantalumBucket full = new TantalumBucket(acid, HydrofluoricAcid.FULL_BUCKET);
        full.setEmpty(empty);
        GameRegistry.registerItem(empty, HydrofluoricAcid.EMPTY_BUCKET);
        GameRegistry.registerItem(full, HydrofluoricAcid.FULL_BUCKET);
        FluidContainerRegistry.registerFluidContainer(
            new FluidStack(fluid, FluidContainerRegistry.BUCKET_VOLUME),
            new ItemStack(full),
            new ItemStack(empty));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(empty), "I I", " I ", 'I', "ingotTantalum"));
        MinecraftForge.EVENT_BUS.register(new EndAcid(acid, empty, full));
    }

    /** The Tantalum bucket takes up only acid sources, and the vanilla bucket never takes up acid. */
    @SubscribeEvent
    public void onFillBucket(FillBucketEvent event) {
        if (event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        Item held = event.current.getItem();
        boolean tantalum = held == emptyBucket;
        if (!tantalum && held != Items.bucket) return;
        World world = event.world;
        int x = event.target.blockX, y = event.target.blockY, z = event.target.blockZ;
        boolean onAcid = world.getBlock(x, y, z) == acid;
        if (!tantalum) {
            if (onAcid) event.setCanceled(true);
            return;
        }
        if (!onAcid || world.getBlockMetadata(x, y, z) != 0
            || !event.entityPlayer.canPlayerEdit(x, y, z, event.target.sideHit, event.current)) {
            event.setCanceled(true);
            return;
        }
        world.setBlockToAir(x, y, z);
        event.result = new ItemStack(fullBucket);
        event.setResult(Event.Result.ALLOW);
    }
}
