package chlorine.etjourney.content.craft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 * The Attune altar: works on its centre item while the catalysts on the pedestals around it and the place match a
 * recipe, then turns the centre into the output and uses up the catalysts.
 */
public final class AltarTile extends ItemHolderTile {

    /**
     * Pedestal spots around the altar on its own level, in a ring like AbyssalCraft's altar; tweak freely, the recipes
     * do not depend on it.
     */
    static final int[][] PEDESTALS = { { 3, 0 }, { -3, 0 }, { 0, 3 }, { 0, -3 }, { 2, 2 }, { 2, -2 }, { -2, 2 },
        { -2, -2 } };
    /** Matching reads the End terrain's noise, so it runs every few ticks and the work counts those ticks. */
    private static final int CHECK_INTERVAL = 10;

    private final CraftProgress progress = new CraftProgress();

    public AltarTile() {
        super(1);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote || (worldObj.getTotalWorldTime() + xCoord + zCoord) % CHECK_INTERVAL != 0) return;
        List<PedestalTile> pedestals = pedestals();
        Recipe recipe = match(pedestals);
        if (!progress.advance(recipe, CHECK_INTERVAL)) return;
        for (PedestalTile pedestal : pedestals) pedestal.clear();
        ItemStack output = CraftWorld.stack(recipe.output);
        if (output == null) clear();
        else replaceAll(output);
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.levelup", 0.5F, 1.0F);
    }

    private Recipe match(List<PedestalTile> pedestals) {
        if (items().isEmpty()) return null;
        List<ItemKey> catalysts = new ArrayList<>();
        for (PedestalTile pedestal : pedestals) catalysts.add(
            CraftWorld.key(
                pedestal.items()
                    .get(0)));
        ItemKey center = CraftWorld.key(items().get(0));
        return CraftRecipes.ATTUNE.find(center, catalysts, CraftWorld.context(worldObj, xCoord, yCoord, zCoord));
    }

    /** The pedestals in the ring that hold a catalyst. */
    private List<PedestalTile> pedestals() {
        List<PedestalTile> found = new ArrayList<>();
        for (int[] spot : PEDESTALS) {
            TileEntity tile = worldObj.getTileEntity(xCoord + spot[0], yCoord, zCoord + spot[1]);
            if (tile instanceof PedestalTile && !((PedestalTile) tile).items()
                .isEmpty()) found.add((PedestalTile) tile);
        }
        return found;
    }
}
