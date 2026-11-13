package chlorine.etjourney.content.craft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.growth.Growth;

/** The Forge: items piled on it turn into the recipe's output after enough hits, or a piece of gear is reforged. */
public final class ForgeTile extends ItemHolderTile {

    static final int CAPACITY = 8;

    private final CraftProgress progress = new CraftProgress();

    public ForgeTile() {
        super(CAPACITY);
    }

    /** One hit; false when the pile matches no recipe here, so the hit does nothing. */
    boolean hit() {
        List<ItemKey> pile = new ArrayList<>();
        for (ItemStack stack : items()) pile.add(CraftWorld.key(stack));
        ItemStack gear = Reforging.find(items());
        Recipe recipe = gear != null ? Reforging.RECIPE
            : CraftRecipes.FORGE.find(null, pile, CraftWorld.context(worldObj, xCoord, yCoord, zCoord));
        boolean finished = progress.advance(recipe, 1);
        if (recipe == null) return false;
        if (finished) {
            clear();
            ItemStack output = gear != null ? gear.copy() : CraftWorld.stack(recipe.output);
            if (gear != null) Growth.reforge(output, worldObj.rand);
            else Growth.ensureRolled(output, worldObj.rand);
            if (output != null) spawn(output);
        }
        worldObj.playSoundEffect(
            xCoord + 0.5,
            yCoord + 0.5,
            zCoord + 0.5,
            finished ? "random.anvil_land" : "random.anvil_use",
            0.5F,
            1.0F);
        return true;
    }
}
