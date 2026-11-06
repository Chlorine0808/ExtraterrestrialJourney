package chlorine.etjourney.content.craft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The recipes of one device, tried in order. */
public final class RecipeBook {

    private final List<Recipe> recipes;

    public RecipeBook(List<Recipe> recipes) {
        this.recipes = Collections.unmodifiableList(new ArrayList<>(recipes));
    }

    /** The first recipe the items and the place satisfy, or null. Pass a null centre for a device without one. */
    public Recipe find(ItemKey center, List<ItemKey> items, ConditionContext context) {
        for (Recipe recipe : recipes) if (recipe.matches(center, items, context)) return recipe;
        return null;
    }
}
