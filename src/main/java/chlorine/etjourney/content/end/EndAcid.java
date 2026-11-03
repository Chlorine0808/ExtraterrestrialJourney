package chlorine.etjourney.content.end;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import chlorine.etjourney.core.registry.RegistryNames;
import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.modifier.EndBlock;
import cpw.mods.fml.common.registry.GameRegistry;

/** Registers the hydrofluoric acid and binds it in place of the End terrain's water. */
public final class EndAcid {

    /** Hydrofluoric acid is a little denser than water. */
    private static final int DENSITY = 1150;

    private EndAcid() {}

    public static void preInit() {
        FluidRegistry.registerFluid(
            new Fluid(HydrofluoricAcid.NAME).setDensity(DENSITY)
                .setUnlocalizedName(RegistryNames.unlocalized(HydrofluoricAcid.NAME)));
        // Another mod may have registered a fluid by this name first; share it.
        Fluid fluid = FluidRegistry.getFluid(HydrofluoricAcid.NAME);
        AcidBlock block = new AcidBlock(fluid);
        GameRegistry.registerBlock(block, HydrofluoricAcid.NAME);
        EndPalette.bind(EndBlock.WATER, block, 0);
    }
}
